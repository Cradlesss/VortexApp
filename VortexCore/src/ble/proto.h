#pragma once

static constexpr const char* UUID_SVC_LED = "895dc926-817a-424d-8736-f582d2dbac8e";
static constexpr const char* UUID_CHR_CONTROL = "7953deb4-b2e1-4829-a692-8ec173cc71fc";
static constexpr const char* UUID_CHR_STATE = "a4de8684-c8ef-460f-ab4f-027237d50997";

static constexpr uint8_t VER_MAJOR = 2;
static constexpr uint8_t VER_MINOR = 0;
static constexpr uint8_t VER_PATCH = 0;

enum class Op : uint8_t {
    SetAnimation = 0x10,
    SetBrightness = 0x11,
    SetRgb = 0x12,
    SetName = 0x13,
    GetState = 0x20,
    Ping = 0x7E,
};

static constexpr uint8_t OP_PONG = 0x7F;

enum : uint16_t {
    FEAT_LED_RGB = 1u << 0,
    FEAT_LED_ANIMATION = 1u << 1,
    FEAT_OTA = 1u << 2,
};

enum : uint8_t {
    ICON_NONE = 0x00,
    ICON_LED_STRIP = 0x01,
    ICON_BULB = 0x02,
    ICON_SENSOR = 0x03,
    ICON_TEST = 0x04,
};

struct __attribute__((packed)) Adv_Manufacturer {
        uint16_t companyId;
        uint8_t  protoVer;    // our adv schema version (1)
        uint8_t modelId;
        uint8_t  version[3];  // {MAJ, MIN, PATCH}
        uint16_t iconCode;    // 0x0001 etc.
        uint32_t features;    // FEAT_* bitmask
};