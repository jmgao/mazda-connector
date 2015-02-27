#include <assert.h>
#include <err.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>

#include <sys/epoll.h>
#include <sys/timerfd.h>
#include <linux/input.h>

#include <thread>

#include "gesture_recognizer.hpp"

#define USE_THREAD_FOR_CALLBACK 1
constexpr long MS_TO_NS = 1000000;

gesture_recognizer::gesture_recognizer(cb_receive_input input_callback, int long_press_ms, int tap_interval_ms) :
    input_callback(input_callback), long_press_ms(long_press_ms), tap_interval_ms(tap_interval_ms)
{
    if (::pipe(input_pipe) != 0) {
        err(1, "failed to create input pipe in gesture_recognizer");
    }

    this->spawn_thread();
}

gesture_recognizer::~gesture_recognizer(void)
{
    ::close(input_pipe[0]);
    ::close(input_pipe[1]);
}

void gesture_recognizer::spawn_thread(void)
{
    assert(!started && "gesture_recognizer started multiple times");
    started = true;

    std::thread([this]() {
        int epfd = ::epoll_create(1);
        struct epoll_event epoll_event = {
            .events = EPOLLIN,
            .data =  {
                .u64 = 0
            }
        };

        epoll_ctl(epfd, EPOLL_CTL_ADD, input_pipe[0], &epoll_event);

        while (true) {
            int count = ::epoll_wait(epfd, &epoll_event, 1, -1);
            if (count <= 0) {
                err(1, "epoll_wait failed");
            }

            if (epoll_event.data.u64 == 0) {
                // Input event
                struct input_event event;
                if (::read(input_pipe[0], &event, sizeof event) != sizeof event) {
                    err(1, "failed to receive input event");
                }

                if (event.type != EV_KEY) {
                    printf("received unhandled input, ignoring (type = %u, code = %u, value = %d)\n", event.type, event.code, event.value);
                    continue;
                }

                auto &state = this->key_state[event.code];
                if (event.value == 1) {
                    // KEY_DOWN
                    assert(state.last_value == 0);

                    ++state.tap_count;

                    struct itimerspec long_press_timerspec = {
                        .it_interval = {0, 0},
                        .it_value = { long_press_ms / 1000, (long_press_ms % 1000) * MS_TO_NS }
                    };

                    if (state.tap_count == 1) {
                        // Create the timer if it doesn't already exist
                        if (state.timer < 0) {
                            state.timer = ::timerfd_create(CLOCK_MONOTONIC, TFD_NONBLOCK);
                            if (state.timer < 0) {
                                err(1, "failed to create timer");
                            }

                            struct epoll_event timer_event = {
                                .events = EPOLLIN,
                                .data = {
                                    .u64 = event.code
                                }
                            };

                            if (::epoll_ctl(epfd, EPOLL_CTL_ADD, state.timer, &timer_event) != 0) {
                                err(1, "failed to add timer to epoll");
                            }
                        }

                        // Start the timer
                        if (::timerfd_settime(state.timer, 0, &long_press_timerspec, nullptr) != 0) {
                            err(1, "failed to set timer for key down");
                        }
                    } else {
                        // Update the timer to the long press interval
                        assert(state.timer >= 0);
                        if (::timerfd_settime(state.timer, 0, &long_press_timerspec, nullptr) != 0) {
                            err(1, "failed to set timer for key down");
                        }
                    }
                } else {
                    // KEY_UP
                    assert(event.value == 0);

                    if (state.tap_count == 0) {
                        printf("received key up for key with no current event, ignoring\n");
                        assert(state.tap_count == 0);
                        assert(state.last_value == 0);
                    } else {
                        assert(state.last_value == 1);
                        assert(state.timer >= 0);

                        // Start the tap interval timer
                        struct itimerspec tap_interval_timerspec = {
                            .it_interval = {0, 0},
                            .it_value = { tap_interval_ms / 1000, (tap_interval_ms % 1000) * MS_TO_NS }
                        };

                        if (::timerfd_settime(state.timer, 0, &tap_interval_timerspec, nullptr) != 0) {
                            err(1, "failed to set timer for key up");
                        }
                    }
                }

                state.last_value = event.value;
            } else {
                // A timer expired
                int keycode = epoll_event.data.u64;
                auto it = this->key_state.find(keycode);
                assert(it != this->key_state.end());
                auto &state = it->second;

                assert(state.tap_count > 0);
                assert(state.timer >= 0);

                // Turn off the timer
                struct itimerspec disabled_timerspec = {
                    .it_interval = {0, 0},
                    .it_value = {0, 0}
                };

                if (::timerfd_settime(state.timer, 0, &disabled_timerspec, nullptr) != 0) {
                    err(1, "failed to disable timer");
                }

                gesture result;
                result.keycode = keycode;
                if (state.last_value == 0) {
                    // Tap interval timer went off
                    result.long_press = false;
                } else {
                    // Long press timer went off
                    result.long_press = true;
                }

                result.tap_count = state.tap_count;

#if USE_THREAD_FOR_CALLBACK
                std::thread([this, result]() {
#endif
                    input_callback(result);
#if USE_THREAD_FOR_CALLBACK
                }).detach();
#endif

                state.tap_count = 0;
                state.last_value = 0;
            }
        }

    }).detach();
}

void gesture_recognizer::handle_input(const struct input_event *input)
{
    if (::write(input_pipe[1], input, sizeof *input) != sizeof *input) {
        err(1, "failed to write input into pipe");
    }
}
