#include <assert.h>
#include <err.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <unistd.h>

#include <map>
#include <mutex>

#include <dbus/dbus.h>

#include "shared/stringify.h"
#include "bluetooth.hpp"
#include "dbus.hpp"

static std::mutex connections_mutex;

using service_map_t = std::map<bluetooth_service_id_t, bluetooth_socket>;
using device_map_t = std::map<bluetooth_device_id_t, service_map_t>;

static device_map_t connections;

std::shared_future<int> bluetooth_connect(bluetooth_device_id_t device_id, bluetooth_service_id_t service_id)
{
    auto lock = std::unique_lock<decltype(connections_mutex)>(connections_mutex);
    auto device_it = connections.find(device_id);

    if (device_it == connections.end()) {
        connections[device_id];
        device_it = connections.find(device_id);
    }

    auto &services = device_it->second;
    auto service_it = services.find(service_id);
    if (service_it == services.end()) {
        services[service_id];
        service_it = services.find(service_id);
    }

    auto &socket = service_it->second;
    switch (socket.state) {
        case bluetooth_socket_state::disconnected:
        {
            socket.state = bluetooth_socket_state::connecting;
            socket.fd_promise = std::promise<int>();
            socket.fd_future = socket.fd_promise.get_future();
            break;
        }

        case bluetooth_socket_state::connecting:
        {
            // Make the ConnectRequest again, it probably doesn't hurt?
            // return socket.fd_future;
            break;
        }

        case bluetooth_socket_state::connected:
        {
            assert(false && "bluetooth_connect called on socket that claims to be connected");
        }
    }

    DBusMessage *msg = dbus_message_new_method_call("com.jci.bca", "/com/jci/bca", "com.jci.bca", "ConnectRequest");
    DBusPendingCall *pending = nullptr;

    if (!msg) {
        assert(false && "failed to create message");
    }

    if (!dbus_message_append_args(msg, DBUS_TYPE_UINT32, &service_id,
                                       DBUS_TYPE_UINT32, &device_id,
                                       DBUS_TYPE_INVALID)) {
       assert(false && "failed to append arguments to message");
    }

    if (!dbus_connection_send_with_reply(hmi_bus, msg, &pending, -1)) {
        assert(false && "failed to send message");
    }

    dbus_connection_flush(hmi_bus);
    dbus_message_unref(msg);

    dbus_pending_call_block(pending);
    msg = dbus_pending_call_steal_reply(pending);
    if (!msg) {
       assert(false && "[" STRINGIFY(__LINE__) "]: received null reply");
    }

    dbus_int32_t result;
    if (!dbus_message_get_args(msg, nullptr, DBUS_TYPE_INT32, &result,
                                             DBUS_TYPE_INVALID)) {
        assert(false && "failed to get result");
    }
    dbus_message_unref(msg);

    if (result != 100) {
        fprintf(stderr, "bluetooth_connect(%u, %u) received failure result %d\n", device_id.t, service_id.t, result);
        socket.fd_promise.set_value(-1);
    }

    return socket.fd_future;
}

// FIXME: Does not actually disconnect!!
void bluetooth_disconnect(bluetooth_device_id_t device_id, bluetooth_service_id_t service_id)
{
    auto lock = std::unique_lock<decltype(connections_mutex)>(connections_mutex);

    auto device_it = connections.find(device_id);

    if (device_it == connections.end()) {
        return;
    }

    auto &services = device_it->second;
    auto service_it = services.find(service_id);
    if (service_it == services.end()) {
        return;
    }

    if (!service_it->second.fd_future.valid()) {
        service_it->second.fd_promise.set_value(-1);
    }

    services.erase(service_it);
}


void handle_bluetooth_connection_response(DBusMessage *msg)
{
    DBusMessageIter args;
    bluetooth_service_id_t service_id;
    dbus_uint32_t connection_status;
    bluetooth_device_id_t device_id;
    dbus_uint32_t status;
    std::string path;

    // Obnoxious message unpacking
    {
        if (!dbus_message_iter_init(msg, &args)) {
            assert(false && "received empty reply");
        }

        if (dbus_message_iter_get_arg_type(&args) != DBUS_TYPE_UINT32) {
            errx(1, "[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(&args));
        }
        dbus_message_iter_get_basic(&args, &service_id.t);
        dbus_message_iter_next(&args);

        if (dbus_message_iter_get_arg_type(&args) != DBUS_TYPE_UINT32) {
            errx(1, "[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(&args));
        }
        dbus_message_iter_get_basic(&args, &connection_status);
        dbus_message_iter_next(&args);

        if (dbus_message_iter_get_arg_type(&args) != DBUS_TYPE_UINT32) {
            errx(1, "[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(&args));
        }
        dbus_message_iter_get_basic(&args, &device_id.t);
        dbus_message_iter_next(&args);

        if (dbus_message_iter_get_arg_type(&args) != DBUS_TYPE_UINT32) {
            errx(1, "[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(&args));
        }
        dbus_message_iter_get_basic(&args, &status);
        dbus_message_iter_next(&args);

        if (dbus_message_iter_get_arg_type(&args) != DBUS_TYPE_STRUCT) {
            errx(1, "[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(&args));
        }

        DBusMessageIter struct_iter;
        dbus_message_iter_recurse(&args, &struct_iter);

        if (dbus_message_iter_get_arg_type(&struct_iter) != DBUS_TYPE_ARRAY) {
            errx(1, "[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(&args));
        }

        DBusMessageIter array_iter;
        dbus_message_iter_recurse(&struct_iter, &array_iter);
        if (dbus_message_iter_get_arg_type(&array_iter) != DBUS_TYPE_BYTE) {
            errx(1, "[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(&args));
        }

        uint8_t *path_data = nullptr;
        int path_length;
        dbus_message_iter_get_fixed_array(&array_iter, &path_data, &path_length);

        path = (char *)path_data;
    }

    // Dump what we know about the message
    printf("device %u, service %u:\n", device_id.t, service_id.t);
    printf("\tconnection_status: ");
    switch (connection_status) {
        case 2:
            printf("connecting?\n");
            break;

        case 7:
            printf("failed to connect?\n");
            break;

        case 3:
            printf("connected?\n");
            break;

        default:
            printf("unknown: %d\n", connection_status);
            break;
    }
    printf("\tstatus: ");
    switch (status) {
        case 100:
            printf("okay?\n");
            break;

        case 104:
            printf("service not found? (check BdsConfiguration.xml\n");
            break;

        case 107:
            printf("device not connected?\n");
            break;

        case 108:
            printf("service doesn't exist on device?\n");
            break;

        default:
            printf("unknown: %d\n", status);
    }

    // Do the actual handling
    {
        auto lock = std::unique_lock<decltype(connections_mutex)>(connections_mutex);
        auto device_it = connections.find(device_id);
        if (device_it == connections.end()) {
            printf("can't find device\n");
            return;
        }

        auto &services = device_it->second;
        auto service_it = services.find(service_id);
        if (service_it == services.end()) {
            printf("can't find service\n");
            return;
        }

        auto &socket = service_it->second;
        switch (socket.state) {
            case bluetooth_socket_state::disconnected:
                break;

            case bluetooth_socket_state::connecting:
            {
                if (connection_status == 3) {
                    int fd = ::open(path.c_str(), O_RDWR);
                    if (fd < 0) {
                        err(1, "failed to open supposedly open socket at %s", path.c_str());
                    }
                    socket.state = bluetooth_socket_state::connected;
                    socket.fd_promise.set_value(fd);
                } else if (connection_status != 2) {
                    socket.state = bluetooth_socket_state::disconnected;
                    socket.fd_promise.set_value(-1);
                }
                break;
            }

            case bluetooth_socket_state::connected:
            {
                if (connection_status != 3) {
                    socket.state = bluetooth_socket_state::disconnected;
                    ::close(socket.fd_future.get());
                }
                break;
            }
        }
    }
}
