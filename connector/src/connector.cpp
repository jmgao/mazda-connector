#include <err.h>
#include <poll.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <thread>
#include <vector>

#include <dbus/dbus.h>

#include "bluetooth.hpp"
#include "connector.hpp"
#include "navigation.hpp"

DBusConnection *service_bus;
DBusConnection *hmi_bus;

std::atomic<int> btfd { -1 };

#define SERVICE_BUS_ADDRESS "unix:path=/tmp/dbus_service_socket"
#define HMI_BUS_ADDRESS "unix:path=/tmp/dbus_hmi_socket"

static DBusHandlerResult handle_service_message(DBusConnection *, DBusMessage *message, void *)
{
    return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
}

static DBusHandlerResult handle_hmi_message(DBusConnection *, DBusMessage *message, void *)
{
    if (strcmp("ConnectionStatusResp", dbus_message_get_member(message)) == 0) {
        handle_bluetooth_connection_response(message);
        return DBUS_HANDLER_RESULT_HANDLED;
    } else if (strcmp("TriggerVR", dbus_message_get_member(message)) == 0) {
        int fd = btfd.load();
        if (fd < 0) {
            printf("Can't trigger voice recognition, not yet connected\n");
        } else {
            printf("Triggering voice recognition, writing to fd %d\n", fd);
            std::string command = "TriggerVR";
            std::vector<char> buf(command.length() + sizeof(uint32_t));
            *(uint32_t *)&buf[0] = command.length();
            memcpy(&buf[4], command.c_str(), command.length());

            printf("writing %d bytes\n", buf.size());
            ::write(fd, &buf[0], buf.size());
        }

        return DBUS_HANDLER_RESULT_HANDLED;
    }

    return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
}

static void initialize_dbus(void)
{
    dbus_threads_init_default();

    DBusError error;
    dbus_error_init(&error);

    // Initialize service bus
    service_bus = dbus_connection_open(SERVICE_BUS_ADDRESS, &error);
    if (!service_bus) {
        errx(1, "failed to connect to service bus: %s: %s\n", error.name, error.message);
    }

    if (!dbus_bus_register(service_bus, &error)) {
        errx(1, "failed to register with service bus: %s: %s\n", error.name, error.message);
    }
    dbus_connection_add_filter(service_bus, handle_service_message, nullptr, nullptr);

    // Initialize HMI bus
    hmi_bus = dbus_connection_open(HMI_BUS_ADDRESS, &error);
    if (!hmi_bus) {
        errx(1, "failed to connect to HMI bus: %s: %s\n", error.name, error.message);
    }

    if (!dbus_bus_register(hmi_bus, &error)) {
        errx(1, "failed to register with HMI bus: %s: %s\n", error.name, error.message);
    }
    dbus_connection_add_filter(hmi_bus, handle_hmi_message, nullptr, nullptr);

    // Bluetooth ConnectionStatusResp
    dbus_bus_add_match(hmi_bus, "type='signal',interface='com.jci.bca',member='ConnectionStatusResp'", &error);
    if (dbus_error_is_set(&error)) {
        errx(1, "failed to add ConnectionStatusResp match: %s: %s\n", error.name, error.message);
    }

    dbus_bus_add_match(hmi_bus, "type='signal',interface='us.insolit.mazda.connector',member='TriggerVR'", &error);
    if (dbus_error_is_set(&error)) {
        errx(1, "failed to add ConnectionStatusResp match: %s: %s\n", error.name, error.message);
    }

    std::thread service_thread([]() {
        while (dbus_connection_read_write_dispatch(service_bus, -1));
    });

    std::thread hmi_thread([]() {
        while (dbus_connection_read_write_dispatch(hmi_bus, -1));
    });

    service_thread.detach();
    hmi_thread.detach();
}

int main(void)
{
    initialize_dbus();

    setbuf(stdout, NULL);
    setbuf(stderr, NULL);

    bluetooth_device_id_t device_id = bluetooth_device_id_t(1);
    bluetooth_service_id_t service_id = bluetooth_service_id_t(8017);
    while (true) {
        std::shared_future<int> socket = bluetooth_connect(device_id, service_id);
        int fd = socket.get();
        if (fd < 0) {
            printf("Failed to connect to socket, retrying in 1 seconds\n");
            sleep(1);
            continue;
        }

        fd = dup(fd);
        btfd.store(fd);

        struct pollfd pfd {
            .fd = fd,
            .events = POLLHUP | POLLERR,
            .revents = 0
        };

        poll(&pfd, 1, -1);
        bluetooth_disconnect(device_id, service_id);
        btfd.store(-1);
        ::close(fd);
    }

    return 0;
}

