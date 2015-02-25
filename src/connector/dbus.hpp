#pragma once

#include <dbus/dbus.h>

#define SERVICE_BUS_ADDRESS "unix:path=/tmp/dbus_service_socket"
#define HMI_BUS_ADDRESS "unix:path=/tmp/dbus_hmi_socket"

extern DBusConnection *service_bus;
extern DBusConnection *hmi_bus;

void initialize_dbus(void);
