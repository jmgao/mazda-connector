#include <err.h>
#include <poll.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <sstream>
#include <thread>
#include <vector>

#include <dbus/dbus.h>
#include <linux/input.h>
#include <sys/uio.h>

#include "shared/dbus_helpers.hpp"
#include "shared/prevent_brick.hpp"

#include "bluetooth.hpp"
#include "dbus.hpp"
#include "gesture_recognizer.hpp"
#include "navigation.hpp"

static void handle_input(gesture input);

static std::atomic<int> btfd { -1 };
static gesture_recognizer recognizer(handle_input);

static DBusHandlerResult handle_service_message(DBusConnection *, DBusMessage *message, void *)
{
    return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
}

static DBusHandlerResult handle_hmi_message(DBusConnection *, DBusMessage *message, void *)
{
    if (strcmp("ConnectionStatusResp", dbus_message_get_member(message)) == 0) {
        handle_bluetooth_connection_response(message);
        return DBUS_HANDLER_RESULT_HANDLED;
    } else if (strcmp("KeyEvent", dbus_message_get_member(message)) == 0) {
        DBusMessageIter iter;
        if (!dbus_message_iter_init(message, &iter)) {
            errx(1, "failed to open message iterator for reading KeyEvent message");
        }

        struct input_event event;
        if (!dbus_message_decode_input_event(&iter, &event)) {
            errx(1, "failed to get input event from KeyEvent message");
        }

        recognizer.handle_input(&event);

        return DBUS_HANDLER_RESULT_HANDLED;
    }

    printf("failed to handle message with member: %s\n", dbus_message_get_member(message));

    return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
}

static void handle_input(gesture input) {
    int fd = btfd.load();
    if (fd < 0) {
        printf("Can't send input to device, not yet connected\n");
    } else {
        static_assert(sizeof input == 9, "struct gesture has the wrong size");

        size_t size = sizeof input + 4;
        constexpr char tag[] = "INPT";

        struct iovec iovs[3] = {
            { &size, sizeof size },
            { const_cast<char *>(tag), 4 },
            { &input, sizeof input }
        };

        ::writev(fd, iovs, sizeof iovs / sizeof *iovs);
    }
}

static void register_signals(void)
{
    DBusError error;
    dbus_error_init(&error);

    dbus_connection_add_filter(service_bus, handle_service_message, nullptr, nullptr);
    dbus_connection_add_filter(hmi_bus, handle_hmi_message, nullptr, nullptr);

    // Bluetooth ConnectionStatusResp
    dbus_bus_add_match(hmi_bus, "type='signal',interface='com.jci.bca',member='ConnectionStatusResp'", &error);
    if (dbus_error_is_set(&error)) {
        errx(1, "failed to add ConnectionStatusResp match: %s: %s\n", error.name, error.message);
    }

    dbus_bus_add_match(hmi_bus, "type='signal',interface='us.insolit.mazda.connector',member='KeyEvent'", &error);
    if (dbus_error_is_set(&error)) {
        errx(1, "failed to add KeyEvent match: %s: %s\n", error.name, error.message);
    }
}

int main(void)
{
    // Try not to brick the car
    prevent_brick("/tmp/mnt/data/enable_connector");

    initialize_dbus();
    register_signals();

    // Continually try to update the location
    std::thread(
        []() {
            while (true) {
                int fd = btfd.load();
                if (fd >= 0) {
                    auto location = GetPosition().get();
                    static_assert(sizeof location == 64, "struct gesture has the wrong size");

                    size_t size = sizeof location + 4;
                    constexpr char tag[] = "GPS!";

                    struct iovec iovs[3] = {
                        { &size, sizeof size },
                        { const_cast<char *>(tag), 4 },
                        { &location, sizeof location }
                    };

                    ::writev(fd, iovs, sizeof iovs / sizeof *iovs);
                }

                // TODO: Find out if the car's location actually only updates at 1Hz
                usleep(100000);
            }
        }).detach();

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
        printf("Socket was closed, disconnecting device\n");
        bluetooth_disconnect(device_id, service_id);
        btfd.store(-1);
        ::close(fd);
    }

    return 0;
}

