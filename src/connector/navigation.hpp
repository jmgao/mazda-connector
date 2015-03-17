#pragma once

#include <stdint.h>

#include <future>

#pragma pack(push, 1)
struct location {
    enum class accuracy : int32_t {
        unknown = 0x00,
        gps_3d = 0x01,
        gps_2d = 0x02,
        dead_reckoning = 0x3,
    };

    accuracy positionAccuracy;
    uint64_t time;
    double latitude;
    double longitude;
    int32_t altitude; /* meters */
    double heading;
    double velocity; /* km/h */
    double horizontalAccuracy;
    double verticalAccuracy;
};
#pragma pack(pop)

std::future<location> GetPosition(void);

void GuidanceChanged(
    int32_t manueverIcon,
    int32_t manueverDistance,
    int32_t manueverDistanceUnit,
    int32_t speedLimit,
    int32_t speedLimitUnit,
    int32_t laneIcon1,
    int32_t laneIcon2,
    int32_t laneIcon3,
    int32_t laneIcon4,
    int32_t laneIcon5,
    int32_t laneIcon6,
    int32_t laneIcon7,
    int32_t laneIcon8
);

std::future<uint8_t>
SetHUDDisplayMsgReq(
    uint32_t manueverIcon,
    uint16_t manueverDistance,
    uint8_t manueverDistanceUnit,
    uint16_t speedLimit,
    uint8_t speedLimitUnit
);

void NotificationBar_Notify(
    int32_t manueverIcon,
    int32_t manueverDistance,
    int32_t manueverDistanceUnit,
    const char *streetName,
    int32_t priority
);

void updateHUD(
    int32_t manueverIcon,
    int32_t manueverDistance,
    int32_t manueverDistanceUnit,
    int32_t speedLimit,
    int32_t speedLimitUnit,
    const char *streetName,
    int32_t priority
);
