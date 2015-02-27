#pragma once

#include <stdint.h>
#include <dbus/dbus.h>
#include <linux/input.h>

#include "stringify.h"

inline bool dbus_message_encode_timeval(DBusMessageIter *iter, const struct timeval *time)
{
    DBusMessageIter sub;
    if (!dbus_message_iter_open_container(iter, DBUS_TYPE_STRUCT, nullptr, &sub)) {
        printf("ERROR: failed to open struct container\n");
        return false;
    }

    dbus_bool_t result = TRUE;
    uint64_t tv_sec = time->tv_sec;
    uint64_t tv_usec = time->tv_usec;

    result &= dbus_message_iter_append_basic(&sub, DBUS_TYPE_UINT64, &tv_sec);
    result &= dbus_message_iter_append_basic(&sub, DBUS_TYPE_UINT64, &tv_usec);

    if (!result) {
        printf("ERROR: failed to append time data");
        return false;
    }

    if (!dbus_message_iter_close_container(iter, &sub)) {
        errx(1, "ERROR: failed to close struct container\n");
    }

    return true;
}

inline bool dbus_message_encode_input_event(DBusMessageIter *iter, const struct input_event *event)
{
    DBusMessageIter sub;
    if (!dbus_message_iter_open_container(iter, DBUS_TYPE_STRUCT, nullptr, &sub)) {
        printf("ERROR: failed to open struct container\n");
        return false;
    }

    if (!dbus_message_encode_timeval(&sub, &event->time)) {
        printf("ERROR: failed to append time\n");
        return false;
    }

    dbus_bool_t result = TRUE;
    result &= dbus_message_iter_append_basic(&sub, DBUS_TYPE_UINT16, &event->type);
    result &= dbus_message_iter_append_basic(&sub, DBUS_TYPE_UINT16, &event->code);
    result &= dbus_message_iter_append_basic(&sub, DBUS_TYPE_INT32, &event->value);

    if (!result) {
        printf("ERROR: failed to append event data");
        return false;
    }

    if (!dbus_message_iter_close_container(iter, &sub)) {
        errx(1, "ERROR: failed to close struct container\n");
    }

    return true;

}

inline bool dbus_message_decode_timeval(DBusMessageIter *iter, struct timeval *time)
{
    DBusMessageIter sub;
    uint64_t tv_sec = 0;
    uint64_t tv_usec = 0;

    if (dbus_message_iter_get_arg_type(iter) != DBUS_TYPE_STRUCT) {
        printf("[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(iter));
        return false;
    }

    dbus_message_iter_recurse(iter, &sub);

    if (dbus_message_iter_get_arg_type(&sub) != DBUS_TYPE_UINT64) {
        printf("[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(iter));
        return false;
    }
    dbus_message_iter_get_basic(&sub, &tv_sec);
    dbus_message_iter_next(&sub);

    if (dbus_message_iter_get_arg_type(&sub) != DBUS_TYPE_UINT64) {
        printf("[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(iter));
        return false;
    }
    dbus_message_iter_get_basic(&sub, &tv_usec);

    dbus_message_iter_next(iter);

    time->tv_sec = tv_sec;
    time->tv_usec = tv_usec;

    return true;
}

inline bool dbus_message_decode_input_event(DBusMessageIter *iter, struct input_event *event)
{
    DBusMessageIter sub;

    if (dbus_message_iter_get_arg_type(iter) != DBUS_TYPE_STRUCT) {
        printf("[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(iter));
        return false;
    }

    dbus_message_iter_recurse(iter, &sub);

    if (!dbus_message_decode_timeval(&sub, &event->time)) {
        printf("ERROR: failed to decode timeval");
        return false;
    }

    if (dbus_message_iter_get_arg_type(&sub) != DBUS_TYPE_UINT16) {
        printf("[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(&sub));
        return false;
    }
    dbus_message_iter_get_basic(&sub, &event->type);
    dbus_message_iter_next(&sub);

    if (dbus_message_iter_get_arg_type(&sub) != DBUS_TYPE_UINT16) {
        printf("[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(&sub));
        return false;
    }
    dbus_message_iter_get_basic(&sub, &event->code);
    dbus_message_iter_next(&sub);

    if (dbus_message_iter_get_arg_type(&sub) != DBUS_TYPE_INT32) {
        printf("[" STRINGIFY(__LINE__) "]: received unexpected type: %c\n", dbus_message_iter_get_arg_type(&sub));
        return false;
    }
    dbus_message_iter_get_basic(&sub, &event->value);

    dbus_message_iter_next(iter);

    return true;
}
