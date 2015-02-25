#pragma once

#include <mutex>
#include <future>

#include <boost/strong_typedef.hpp>

BOOST_STRONG_TYPEDEF(uint32_t, bluetooth_device_id_t);

// Service specified in BdsConfiguration.xml
BOOST_STRONG_TYPEDEF(uint32_t, bluetooth_service_id_t);

enum class bluetooth_socket_state {
    disconnected,
    connecting,
    connected,
};

struct bluetooth_socket {
    bluetooth_socket_state state = bluetooth_socket_state::disconnected;
    std::promise<int> fd_promise;
    std::shared_future<int> fd_future = fd_promise.get_future();
};


// Returns a future that will be populated with the FD of the socket.
// Immediately dup this fd when received, the original will be closed when the socket is closed.
std::shared_future<int> bluetooth_connect(bluetooth_device_id_t device, bluetooth_service_id_t service);

void bluetooth_disconnect(bluetooth_device_id_t device_id, bluetooth_service_id_t service_id);

void handle_bluetooth_connection_response(DBusMessage *message);
