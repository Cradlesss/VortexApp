/**
 * Native unit tests for the SavedState struct and its checksum algorithm.
 *
 * Tests two things without any hardware dependency:
 *   1. SavedState struct layout – size must match what is written to /state.bin
 *   2. stateChecksum – mirrors the production algorithm in StateStorage.cpp
 *
 * Run with:  pio test -e native
 */

#include <unity.h>
#include <stdint.h>
#include <string.h>

// ── mirror production constants ──────────────────────────────────────────────
static constexpr uint8_t STATE_MAGIC   = 0x42;
static constexpr uint8_t STATE_VERSION = 1;

// ── mirror SavedState (must stay in sync with StateStorage.h) ─────────────────
struct SavedState {
    uint8_t magic;
    uint8_t version;
    uint8_t mode;
    uint8_t brightness;
    uint8_t r, g, b;
    uint8_t checksum;
};

static_assert(sizeof(SavedState) == 8,
              "SavedState must be 8 bytes for /state.bin binary compatibility");

// ── mirror stateChecksum (must stay in sync with StateStorage.cpp) ───────────
static uint8_t stateChecksum(const SavedState& s) {
    uint8_t sum = 0;
    sum += s.magic;
    sum += s.version;
    sum += s.mode;
    sum += s.brightness;
    sum += s.r;
    sum += s.g;
    sum += s.b;
    return sum;
}

// ── helpers ──────────────────────────────────────────────────────────────────
static SavedState makeEntry(uint8_t mode, uint8_t brightness, uint8_t r, uint8_t g, uint8_t b) {
    SavedState s{};
    s.magic      = STATE_MAGIC;
    s.version    = STATE_VERSION;
    s.mode       = mode;
    s.brightness = brightness;
    s.r = r; s.g = g; s.b = b;
    s.checksum   = stateChecksum(s);
    return s;
}

// ═════════════════════════════════════════════════════════════════════════════
// 1. SavedState struct layout
// ═════════════════════════════════════════════════════════════════════════════

void test_savedstate_struct_size_is_8() {
    TEST_ASSERT_EQUAL(8, sizeof(SavedState));
}

void test_savedstate_magic_value() {
    TEST_ASSERT_EQUAL(0x42u, STATE_MAGIC);
}

void test_savedstate_version_value() {
    TEST_ASSERT_EQUAL(1u, STATE_VERSION);
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. stateChecksum
// ═════════════════════════════════════════════════════════════════════════════

void test_checksum_known_value() {
    // magic=0x42=66 version=1 mode=2 brightness=60 r=0 g=0 b=0
    // sum = 66 + 1 + 2 + 60 + 0 + 0 + 0 = 129
    SavedState s = makeEntry(2, 60, 0, 0, 0);
    s.checksum = 0;
    TEST_ASSERT_EQUAL(129u, stateChecksum(s));
}

void test_checksum_roundtrip_valid() {
    SavedState s = makeEntry(5, 128, 255, 0, 128);
    uint8_t stored = s.checksum;
    s.checksum = 0;
    TEST_ASSERT_EQUAL(stored, stateChecksum(s));
}

void test_checksum_changes_when_mode_changes() {
    SavedState a = makeEntry(1, 100, 0, 0, 0);
    SavedState b = makeEntry(2, 100, 0, 0, 0);
    a.checksum = 0; b.checksum = 0;
    TEST_ASSERT_NOT_EQUAL(stateChecksum(a), stateChecksum(b));
}

void test_checksum_changes_when_brightness_changes() {
    SavedState a = makeEntry(0, 50,  0, 0, 0);
    SavedState b = makeEntry(0, 100, 0, 0, 0);
    a.checksum = 0; b.checksum = 0;
    TEST_ASSERT_NOT_EQUAL(stateChecksum(a), stateChecksum(b));
}

void test_checksum_changes_when_rgb_changes() {
    SavedState a = makeEntry(0, 0, 10, 20, 30);
    SavedState b = makeEntry(0, 0, 11, 20, 30);
    a.checksum = 0; b.checksum = 0;
    TEST_ASSERT_NOT_EQUAL(stateChecksum(a), stateChecksum(b));
}

void test_checksum_detects_mode_corruption() {
    SavedState s = makeEntry(3, 200, 100, 150, 200);
    s.mode = 99; // corrupt after checksum computed
    TEST_ASSERT_NOT_EQUAL(s.checksum, stateChecksum(s));
}

void test_checksum_detects_brightness_corruption() {
    SavedState s = makeEntry(1, 128, 0, 0, 0);
    s.brightness = 0; // corrupt
    TEST_ASSERT_NOT_EQUAL(s.checksum, stateChecksum(s));
}

void test_checksum_detects_rgb_corruption() {
    SavedState s = makeEntry(0, 0, 255, 255, 255);
    s.b = 0; // corrupt blue channel
    TEST_ASSERT_NOT_EQUAL(s.checksum, stateChecksum(s));
}

void test_checksum_all_zeros_payload() {
    SavedState s = makeEntry(0, 0, 0, 0, 0);
    // sum = 0x42 + 1 = 67
    s.checksum = 0;
    TEST_ASSERT_EQUAL(67u, stateChecksum(s));
}

void test_checksum_max_values() {
    SavedState s = makeEntry(255, 255, 255, 255, 255);
    uint8_t stored = s.checksum;
    s.checksum = 0;
    TEST_ASSERT_EQUAL(stored, stateChecksum(s));
}

// ═════════════════════════════════════════════════════════════════════════════
// Runner
// ═════════════════════════════════════════════════════════════════════════════

void setUp()    {}
void tearDown() {}

int main() {
    UNITY_BEGIN();

    // Struct layout
    RUN_TEST(test_savedstate_struct_size_is_8);
    RUN_TEST(test_savedstate_magic_value);
    RUN_TEST(test_savedstate_version_value);

    // Checksum
    RUN_TEST(test_checksum_known_value);
    RUN_TEST(test_checksum_roundtrip_valid);
    RUN_TEST(test_checksum_changes_when_mode_changes);
    RUN_TEST(test_checksum_changes_when_brightness_changes);
    RUN_TEST(test_checksum_changes_when_rgb_changes);
    RUN_TEST(test_checksum_detects_mode_corruption);
    RUN_TEST(test_checksum_detects_brightness_corruption);
    RUN_TEST(test_checksum_detects_rgb_corruption);
    RUN_TEST(test_checksum_all_zeros_payload);
    RUN_TEST(test_checksum_max_values);

    return UNITY_END();
}
