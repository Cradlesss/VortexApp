/**
 * Native unit tests for the BLE control-frame protocol.
 *
 * Tests three things without any hardware dependency:
 *   1. Opcode constants – values must match proto.h and the Android LedControlFrames
 *   2. Frame validation – mirrors the bounds checks in onControlWrite for every opcode
 *   3. Adv_Manufacturer struct layout – size must be 13 bytes (packed)
 *
 * Run with:  pio test -e native
 */

#include <unity.h>
#include <stdint.h>
#include <string.h>

// ── mirror opcode constants (must stay in sync with proto.h) ─────────────────
static constexpr uint8_t OP_SET_ANIMATION  = 0x10;
static constexpr uint8_t OP_SET_BRIGHTNESS = 0x11;
static constexpr uint8_t OP_SET_RGB        = 0x12;
static constexpr uint8_t OP_SET_NAME       = 0x13;
static constexpr uint8_t OP_GET_STATE      = 0x20;
static constexpr uint8_t OP_PING           = 0x7E;
static constexpr uint8_t OP_PONG           = 0x7F;

static constexpr uint8_t NAME_MAX_LEN = 18;

// ── mirror Adv_Manufacturer (must stay in sync with proto.h) ─────────────────
struct __attribute__((packed)) Adv_Manufacturer {
    uint16_t companyId;
    uint8_t  protoVer;
    uint8_t  modelId;
    uint8_t  version[3];
    uint16_t iconCode;
    uint32_t features;
};

static_assert(sizeof(Adv_Manufacturer) == 13,
              "Adv_Manufacturer must be 13 bytes for BLE advertising compatibility");

// ── generic frame validator (mirrors onControlWrite header checks) ───────────
static bool isValidFrame(const uint8_t* data, uint16_t frameLen, uint8_t expectedOp,
                         uint8_t minPayload, uint8_t maxPayload) {
    if (frameLen < 2)                              return false;
    if ((data[0] & 0xFF) != expectedOp)            return false;
    uint8_t  payLen = data[1];
    uint16_t expect = static_cast<uint16_t>(2u + payLen);
    if (frameLen != expect)                        return false;
    if (payLen < minPayload || payLen > maxPayload) return false;
    return true;
}

// ═════════════════════════════════════════════════════════════════════════════
// 1. Opcode constants
// ═════════════════════════════════════════════════════════════════════════════

void test_opcode_set_animation_is_0x10()  { TEST_ASSERT_EQUAL(0x10u, OP_SET_ANIMATION);  }
void test_opcode_set_brightness_is_0x11() { TEST_ASSERT_EQUAL(0x11u, OP_SET_BRIGHTNESS); }
void test_opcode_set_rgb_is_0x12()        { TEST_ASSERT_EQUAL(0x12u, OP_SET_RGB);        }
void test_opcode_set_name_is_0x13()       { TEST_ASSERT_EQUAL(0x13u, OP_SET_NAME);       }
void test_opcode_get_state_is_0x20()      { TEST_ASSERT_EQUAL(0x20u, OP_GET_STATE);      }
void test_opcode_ping_is_0x7E()           { TEST_ASSERT_EQUAL(0x7Eu, OP_PING);           }
void test_opcode_pong_is_0x7F()           { TEST_ASSERT_EQUAL(0x7Fu, OP_PONG);           }

void test_all_opcodes_are_unique() {
    uint8_t ops[] = {OP_SET_ANIMATION, OP_SET_BRIGHTNESS, OP_SET_RGB,
                     OP_SET_NAME, OP_GET_STATE, OP_PING, OP_PONG};
    const size_t n = sizeof(ops) / sizeof(ops[0]);
    for (size_t i = 0; i < n; i++)
        for (size_t j = i + 1; j < n; j++)
            TEST_ASSERT_NOT_EQUAL(ops[i], ops[j]);
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. Frame validation – SetAnimation (payload: exactly 1 byte)
// ═════════════════════════════════════════════════════════════════════════════

void test_setanimation_valid() {
    uint8_t f[] = {OP_SET_ANIMATION, 1, 5};
    TEST_ASSERT_TRUE(isValidFrame(f, sizeof(f), OP_SET_ANIMATION, 1, 1));
}

void test_setanimation_mode0_valid() {
    uint8_t f[] = {OP_SET_ANIMATION, 1, 0};
    TEST_ASSERT_TRUE(isValidFrame(f, sizeof(f), OP_SET_ANIMATION, 1, 1));
}

void test_setanimation_mode255_valid() {
    uint8_t f[] = {OP_SET_ANIMATION, 1, 255};
    TEST_ASSERT_TRUE(isValidFrame(f, sizeof(f), OP_SET_ANIMATION, 1, 1));
}

void test_setanimation_empty_payload_rejected() {
    uint8_t f[] = {OP_SET_ANIMATION, 0};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_ANIMATION, 1, 1));
}

void test_setanimation_too_long_rejected() {
    uint8_t f[] = {OP_SET_ANIMATION, 2, 0, 0};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_ANIMATION, 1, 1));
}

void test_setanimation_too_short_for_header_rejected() {
    uint8_t f[] = {OP_SET_ANIMATION};
    TEST_ASSERT_FALSE(isValidFrame(f, 1, OP_SET_ANIMATION, 1, 1));
}

void test_setanimation_wrong_opcode_rejected() {
    uint8_t f[] = {OP_SET_BRIGHTNESS, 1, 5};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_ANIMATION, 1, 1));
}

void test_setanimation_length_mismatch_rejected() {
    // claims 2 payload bytes but only 1 present
    uint8_t f[] = {OP_SET_ANIMATION, 2, 5};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_ANIMATION, 1, 1));
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. Frame validation – SetBrightness (payload: exactly 1 byte)
// ═════════════════════════════════════════════════════════════════════════════

void test_setbrightness_valid() {
    uint8_t f[] = {OP_SET_BRIGHTNESS, 1, 128};
    TEST_ASSERT_TRUE(isValidFrame(f, sizeof(f), OP_SET_BRIGHTNESS, 1, 1));
}

void test_setbrightness_empty_payload_rejected() {
    uint8_t f[] = {OP_SET_BRIGHTNESS, 0};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_BRIGHTNESS, 1, 1));
}

void test_setbrightness_too_long_rejected() {
    uint8_t f[] = {OP_SET_BRIGHTNESS, 2, 0, 0};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_BRIGHTNESS, 1, 1));
}

void test_setbrightness_wrong_opcode_rejected() {
    uint8_t f[] = {OP_SET_ANIMATION, 1, 128};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_BRIGHTNESS, 1, 1));
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. Frame validation – SetRgb (payload: exactly 3 bytes)
// ═════════════════════════════════════════════════════════════════════════════

void test_setrgb_valid() {
    uint8_t f[] = {OP_SET_RGB, 3, 255, 128, 0};
    TEST_ASSERT_TRUE(isValidFrame(f, sizeof(f), OP_SET_RGB, 3, 3));
}

void test_setrgb_all_zeros_valid() {
    uint8_t f[] = {OP_SET_RGB, 3, 0, 0, 0};
    TEST_ASSERT_TRUE(isValidFrame(f, sizeof(f), OP_SET_RGB, 3, 3));
}

void test_setrgb_all_max_valid() {
    uint8_t f[] = {OP_SET_RGB, 3, 255, 255, 255};
    TEST_ASSERT_TRUE(isValidFrame(f, sizeof(f), OP_SET_RGB, 3, 3));
}

void test_setrgb_too_short_rejected() {
    uint8_t f[] = {OP_SET_RGB, 2, 255, 128};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_RGB, 3, 3));
}

void test_setrgb_too_long_rejected() {
    uint8_t f[] = {OP_SET_RGB, 4, 0, 0, 0, 0};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_RGB, 3, 3));
}

void test_setrgb_empty_payload_rejected() {
    uint8_t f[] = {OP_SET_RGB, 0};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_RGB, 3, 3));
}

void test_setrgb_wrong_opcode_rejected() {
    uint8_t f[] = {OP_SET_ANIMATION, 3, 0, 0, 0};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_RGB, 3, 3));
}

void test_setrgb_length_mismatch_rejected() {
    // claims 3 payload bytes but only 2 present
    uint8_t f[] = {OP_SET_RGB, 3, 0, 0};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_SET_RGB, 3, 3));
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. Frame validation – GetState (no payload)
// ═════════════════════════════════════════════════════════════════════════════

void test_getstate_valid() {
    uint8_t f[] = {OP_GET_STATE, 0};
    TEST_ASSERT_TRUE(isValidFrame(f, sizeof(f), OP_GET_STATE, 0, 0));
}

void test_getstate_with_payload_rejected() {
    uint8_t f[] = {OP_GET_STATE, 1, 0x00};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_GET_STATE, 0, 0));
}

void test_getstate_too_short_for_header_rejected() {
    uint8_t f[] = {OP_GET_STATE};
    TEST_ASSERT_FALSE(isValidFrame(f, 1, OP_GET_STATE, 0, 0));
}

void test_getstate_wrong_opcode_rejected() {
    uint8_t f[] = {OP_SET_ANIMATION, 0};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_GET_STATE, 0, 0));
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. Frame validation – Ping (payload: exactly 1 token byte)
// ═════════════════════════════════════════════════════════════════════════════

void test_ping_valid() {
    uint8_t f[] = {OP_PING, 1, 0xAB};
    TEST_ASSERT_TRUE(isValidFrame(f, sizeof(f), OP_PING, 1, 1));
}

void test_ping_token0_valid() {
    uint8_t f[] = {OP_PING, 1, 0x00};
    TEST_ASSERT_TRUE(isValidFrame(f, sizeof(f), OP_PING, 1, 1));
}

void test_ping_empty_payload_rejected() {
    uint8_t f[] = {OP_PING, 0};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_PING, 1, 1));
}

void test_ping_too_long_rejected() {
    uint8_t f[] = {OP_PING, 2, 0x00, 0x00};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_PING, 1, 1));
}

void test_ping_wrong_opcode_rejected() {
    uint8_t f[] = {OP_PONG, 1, 0x00};
    TEST_ASSERT_FALSE(isValidFrame(f, sizeof(f), OP_PING, 1, 1));
}

// ═════════════════════════════════════════════════════════════════════════════
// 7. Adv_Manufacturer struct layout
// ═════════════════════════════════════════════════════════════════════════════

void test_adv_manufacturer_struct_size_is_13() {
    TEST_ASSERT_EQUAL(13, sizeof(Adv_Manufacturer));
}

// ═════════════════════════════════════════════════════════════════════════════
// Runner
// ═════════════════════════════════════════════════════════════════════════════

void setUp()    {}
void tearDown() {}

int main() {
    UNITY_BEGIN();

    // Opcode constants
    RUN_TEST(test_opcode_set_animation_is_0x10);
    RUN_TEST(test_opcode_set_brightness_is_0x11);
    RUN_TEST(test_opcode_set_rgb_is_0x12);
    RUN_TEST(test_opcode_set_name_is_0x13);
    RUN_TEST(test_opcode_get_state_is_0x20);
    RUN_TEST(test_opcode_ping_is_0x7E);
    RUN_TEST(test_opcode_pong_is_0x7F);
    RUN_TEST(test_all_opcodes_are_unique);

    // SetAnimation frame
    RUN_TEST(test_setanimation_valid);
    RUN_TEST(test_setanimation_mode0_valid);
    RUN_TEST(test_setanimation_mode255_valid);
    RUN_TEST(test_setanimation_empty_payload_rejected);
    RUN_TEST(test_setanimation_too_long_rejected);
    RUN_TEST(test_setanimation_too_short_for_header_rejected);
    RUN_TEST(test_setanimation_wrong_opcode_rejected);
    RUN_TEST(test_setanimation_length_mismatch_rejected);

    // SetBrightness frame
    RUN_TEST(test_setbrightness_valid);
    RUN_TEST(test_setbrightness_empty_payload_rejected);
    RUN_TEST(test_setbrightness_too_long_rejected);
    RUN_TEST(test_setbrightness_wrong_opcode_rejected);

    // SetRgb frame
    RUN_TEST(test_setrgb_valid);
    RUN_TEST(test_setrgb_all_zeros_valid);
    RUN_TEST(test_setrgb_all_max_valid);
    RUN_TEST(test_setrgb_too_short_rejected);
    RUN_TEST(test_setrgb_too_long_rejected);
    RUN_TEST(test_setrgb_empty_payload_rejected);
    RUN_TEST(test_setrgb_wrong_opcode_rejected);
    RUN_TEST(test_setrgb_length_mismatch_rejected);

    // GetState frame
    RUN_TEST(test_getstate_valid);
    RUN_TEST(test_getstate_with_payload_rejected);
    RUN_TEST(test_getstate_too_short_for_header_rejected);
    RUN_TEST(test_getstate_wrong_opcode_rejected);

    // Ping frame
    RUN_TEST(test_ping_valid);
    RUN_TEST(test_ping_token0_valid);
    RUN_TEST(test_ping_empty_payload_rejected);
    RUN_TEST(test_ping_too_long_rejected);
    RUN_TEST(test_ping_wrong_opcode_rejected);

    // Adv_Manufacturer struct
    RUN_TEST(test_adv_manufacturer_struct_size_is_13);

    return UNITY_END();
}
