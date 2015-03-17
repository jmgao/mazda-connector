#include <err.h>
#include <thread>
#include <dbus/dbus.h>

#include "dbus.hpp"

DBusConnection *service_bus;
DBusConnection *hmi_bus;

void initialize_dbus(void)
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

    // Initialize HMI bus
    hmi_bus = dbus_connection_open(HMI_BUS_ADDRESS, &error);
    if (!hmi_bus) {
        errx(1, "failed to connect to HMI bus: %s: %s\n", error.name, error.message);
    }

    if (!dbus_bus_register(hmi_bus, &error)) {
        errx(1, "failed to register with HMI bus: %s: %s\n", error.name, error.message);
    }

    #warning FIXME: This breaks method calls for some reason
    // std::thread service_thread([]() {
    //     while (dbus_connection_read_write_dispatch(service_bus, 1000)) {
    //         printf("tick\n");
    //     }
    //     printf("what\n");
    // });

    std::thread hmi_thread([]() {
        while (dbus_connection_read_write_dispatch(hmi_bus, -1));
    });

    // service_thread.detach();
    hmi_thread.detach();
}
