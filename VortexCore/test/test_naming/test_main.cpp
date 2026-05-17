/**
 * Native (host) unit tests for the naming feature.
 *
 * Tests three things without any hardware dependency:
 *   1. SavedName struct layout – size must match what is written to /name.bin
 *   2. nameChecksum – mirrors the production algorithm in StateStorage.cpp
 *   3. SetName frame validation – mirrors the bounds checks in onControlWrite
 *
 * Run with:  pio test -e native
 */

#include <unity.h>
#include <stdint.h>
#include <string.h>

// ── mirror production constants ──────────────────────────────────────────────
static constexpr uint8_t NAME_MAX_LEN  = 18;
static constexpr uint8_t OP_SET_NAME   = 0x13;
static constexpr uint8_t NAME_MAGIC    = 0x4E;
static constexpr uint8_t NAME_VERSION  = 1;

// ── mirror SavedName (must stay in sync with StateStorage.h) ─────────────────
struct SavedName {
    uint8_t magic;
    uint8_t version;
    uint8_t len;
    char    name[18];
    uint8_t checksum;
};

// Compile-time guard: if the struct size changes, /name.bin becomes unreadable.
static_assert(sizeof(SavedName) == 22,
              "SavedName must be 22 bytes for /name.bin binary compatibility");

// ── mirror nameChecksum (must stay in sync with StateStorage.cpp) ─────────────
static uint8_t nameChecksum(const SavedName& s) {
    uint8_t sum = s.magic + s.version + s.len;
    for (uint8_t i = 0; i < s.len; i++) sum += static_cast<uint8_t>(s.name[i]);
    return sum;
}

// ── mirror SetName frame validation (must stay in sync with onControlWrite) ──
static bool isValidSetNameFrame(const uint8_t* data, uint16_t frameLen) {
    if (frameLen < 2)                         return false;
    if ((data[0] & 0xFF) != OP_SET_NAME)      return false;
    uint8_t  payLen = data[1];
    uint16_t expect = static_cast<uint16_t>(2u + payLen);
    if (frameLen != expect)                   return false;
    if (payLen < 1 || payLen > NAME_MAX_LEN)  return false;
    return true;
}

// ── helpers ──────────────────────────────────────────────────────────────────
static SavedName makeEntry(const char* str) {
    SavedName s{};
    s.magic   = NAME_MAGIC;
    s.version = NAME_VERSION;
    s.len     = static_cast<uint8_t>(strlen(str));
    memcpy(s.name, str, s.len);
    s.checksum = nameChecksum(s);
    return s;
}

// ═════════════════════════════════════════════════════════════════════════════
// 1. SavedName struct layout
// ═════════════════════════════════════════════════════════════════════════════

void test_savedname_struct_size_is_22() {
    TEST_ASSERT_EQUAL(22, sizeof(SavedName));
}

void test_savedname_name_field_capacity() {
    SavedName s{};
    TEST_ASSERT_EQUAL(18u, sizeof(s.name));
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. nameChecksum
// ═════════════════════════════════════════════════════════════════════════════

void test_checksum_known_value() {
    // magic=0x4E version=1 len=3 name="abc"
    // sum = 0x4E + 1 + 3 + 97 + 98 + 99 = 376 → 376 & 0xFF = 120
    SavedName s = makeEntry("abc");
    s.checksum = 0; // clear before recomputing
    TEST_ASSERT_EQUAL(120u, nameChecksum(s));
}

void test_checksum_single_char() {
    SavedName s = makeEntry("X");
    s.checksum = 0;
    uint8_t expected = NAME_MAGIC + NAME_VERSION + 1u + (uint8_t)'X';
    TEST_ASSERT_EQUAL(expected, nameChecksum(s));
}

void test_checksum_changes_when_name_changes() {
    SavedName a = makeEntry("foo");
    SavedName b = makeEntry("bar");
    a.checksum = 0; b.checksum = 0;
    TEST_ASSERT_NOT_EQUAL(nameChecksum(a), nameChecksum(b));
}

void test_checksum_changes_when_len_changes() {
    // Same underlying bytes in name[], different len
    SavedName s{};
    s.magic   = NAME_MAGIC;
    s.version = NAME_VERSION;
    memcpy(s.name, "ab", 2);

    s.len = 1; uint8_t c1 = nameChecksum(s);
    s.len = 2; uint8_t c2 = nameChecksum(s);
    TEST_ASSERT_NOT_EQUAL(c1, c2);
}

void test_checksum_roundtrip_valid() {
    SavedName s = makeEntry("vortex_led");
    // Verify the stored checksum matches a fresh computation
    uint8_t stored = s.checksum;
    s.checksum = 0;
    TEST_ASSERT_EQUAL(stored, nameChecksum(s));
}

void test_checksum_detects_name_corruption() {
    SavedName s = makeEntry("mydevice");
    s.name[0] = 'X'; // corrupt one byte after checksum was computed
    uint8_t fresh = nameChecksum(s);
    TEST_ASSERT_NOT_EQUAL(s.checksum, fresh);
}

void test_checksum_max_length_name() {
    // 18 'a' characters must produce a deterministic, stable checksum
    SavedName s = makeEntry("aaaaaaaaaaaaaaaaaa"); // exactly 18
    TEST_ASSERT_EQUAL(NAME_MAX_LEN, s.len);
    uint8_t stored = s.checksum;
    s.checksum = 0;
    TEST_ASSERT_EQUAL(stored, nameChecksum(s));
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. SetName frame validation
// ═════════════════════════════════════════════════════════════════════════════

void test_frame_valid_short_name() {
    uint8_t f[] = {OP_SET_NAME, 3, 'a', 'b', 'c'};
    TEST_ASSERT_TRUE(isValidSetNameFrame(f, sizeof(f)));
}

void test_frame_valid_single_char() {
    uint8_t f[] = {OP_SET_NAME, 1, 'X'};
    TEST_ASSERT_TRUE(isValidSetNameFrame(f, sizeof(f)));
}

void test_frame_valid_max_length() {
    uint8_t f[2 + NAME_MAX_LEN];
    f[0] = OP_SET_NAME;
    f[1] = NAME_MAX_LEN;
    memset(f + 2, 'z', NAME_MAX_LEN);
    TEST_ASSERT_TRUE(isValidSetNameFrame(f, sizeof(f)));
}

void test_frame_empty_payload_rejected() {
    uint8_t f[] = {OP_SET_NAME, 0};
    TEST_ASSERT_FALSE(isValidSetNameFrame(f, sizeof(f)));
}

void test_frame_one_over_max_rejected() {
    uint8_t f[2 + NAME_MAX_LEN + 1];
    f[0] = OP_SET_NAME;
    f[1] = NAME_MAX_LEN + 1;
    memset(f + 2, 'z', NAME_MAX_LEN + 1);
    TEST_ASSERT_FALSE(isValidSetNameFrame(f, sizeof(f)));
}

void test_frame_length_mismatch_rejected() {
    // payLen claims 5 bytes but only 3 payload bytes are present
    uint8_t f[] = {OP_SET_NAME, 5, 'a', 'b', 'c'};
    TEST_ASSERT_FALSE(isValidSetNameFrame(f, sizeof(f)));
}

void test_frame_too_short_for_header_rejected() {
    uint8_t f[] = {OP_SET_NAME};
    TEST_ASSERT_FALSE(isValidSetNameFrame(f, 1));
}

void test_frame_wrong_opcode_rejected() {
    // 0x10 = SetAnimation — must not be accepted as SetName
    uint8_t f[] = {0x10, 1, 0x00};
    TEST_ASSERT_FALSE(isValidSetNameFrame(f, sizeof(f)));
}

void test_frame_opcode_is_0x13() {
    // Belt-and-suspenders: verify the constant itself hasn't drifted
    TEST_ASSERT_EQUAL(0x13u, OP_SET_NAME);
}

// ═════════════════════════════════════════════════════════════════════════════
// Runner
// ═════════════════════════════════════════════════════════════════════════════

void setUp()    {}
void tearDown() {}

int main() {
    UNITY_BEGIN();

    // Struct layout
    RUN_TEST(test_savedname_struct_size_is_22);
    RUN_TEST(test_savedname_name_field_capacity);

    // Checksum
    RUN_TEST(test_checksum_known_value);
    RUN_TEST(test_checksum_single_char);
    RUN_TEST(test_checksum_changes_when_name_changes);
    RUN_TEST(test_checksum_changes_when_len_changes);
    RUN_TEST(test_checksum_roundtrip_valid);
    RUN_TEST(test_checksum_detects_name_corruption);
    RUN_TEST(test_checksum_max_length_name);

    // Frame validation
    RUN_TEST(test_frame_valid_short_name);
    RUN_TEST(test_frame_valid_single_char);
    RUN_TEST(test_frame_valid_max_length);
    RUN_TEST(test_frame_empty_payload_rejected);
    RUN_TEST(test_frame_one_over_max_rejected);
    RUN_TEST(test_frame_length_mismatch_rejected);
    RUN_TEST(test_frame_too_short_for_header_rejected);
    RUN_TEST(test_frame_wrong_opcode_rejected);
    RUN_TEST(test_frame_opcode_is_0x13);

    return UNITY_END();
}
