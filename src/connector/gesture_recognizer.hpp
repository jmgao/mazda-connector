#pragma once

#include <functional>
#include <unordered_map>

#pragma pack(push, 1)
struct gesture {
    int32_t keycode;
    int32_t tap_count;
    char long_press;
};
#pragma pack(pop)

class gesture_recognizer {
public:
    using cb_receive_input = std::function<void (gesture)>;

    gesture_recognizer(const gesture_recognizer &copy) = delete;
    gesture_recognizer(gesture_recognizer &&move) = default;
    gesture_recognizer(cb_receive_input input_callback, int long_press_ms = 500, int tap_interval_ms = 250);
    ~gesture_recognizer(void);

private:
    bool started = false;
    int input_pipe[2];
    cb_receive_input input_callback;

    // Time to wait to trigger a long press
    int long_press_ms;

    // Time to wait for additional taps
    int tap_interval_ms;

    struct recognizer_state {
        int tap_count = 0;
        struct timeval last_timestamp = { .tv_sec = 0, .tv_usec = 0 };
        int last_value = 0;
        int timer = -1;
    };

    // map from keycode to last event
    std::unordered_map<int, recognizer_state> key_state;

    void spawn_thread(void);

public:
    void handle_input(const struct input_event *input);
};
