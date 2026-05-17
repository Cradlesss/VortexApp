#include "StateStorage.h"
#include <Adafruit_LittleFS.h>
#include <InternalFileSystem.h>
#include <debug.h>

using namespace Adafruit_LittleFS_Namespace;

namespace {
    constexpr uint8_t kMagic = 0x42;
    constexpr uint8_t kVersion = 1;
    const char* const kStateFile = "/state.bin";

    constexpr uint8_t kNameMagic = 0x4E;
    constexpr uint8_t kNameVersion = 1;
    const char* const kNameFile = "/name.bin";
}

StateStorage& StateStorage::instance() {
    static StateStorage inst;
    return inst;
}

void StateStorage::ensureFsMounted() {
    if (fsTried_) return;
    fsTried_ = true;

    VDBG_LN("StateStorage: mounting InternalFS...");
    if (InternalFS.begin()) {
        fsOk_ = true;
        VDBG_LN("StateStorage: InternalFS mounted.");
        return;
    }

    VDBG_LN("StateStorage: InternalFS mount failed, formatting...");
    InternalFS.format();

    if (InternalFS.begin()) {
        fsOk_ = true;
        VDBG_LN("StateStorage: InternalFS mounted after format.");
    } else {
        fsOk_ = false;
        VDBG_LN("StateStorage: InternalFS mount failed after format.");
    }
}

void StateStorage::load() {
    valid_ = false;

    ensureFsMounted();
    if (!fsOk_) {
        VDBG_LN("StateStorage::load(): FS not OK, skipping.");
        return;
    }

    File file(InternalFS);
    if (!file.open(kStateFile, FILE_O_READ)) {
        VDBG_LN("StateStorage::load(): file not found.");
        return;
    }

    SavedState tmp{};
    int readBytes = file.read(&tmp, sizeof(SavedState));
    file.close();

    if (readBytes != static_cast<int>(sizeof(SavedState))) {
        VDBG("StateStorage::load(): short read, got %d\n", readBytes);
        return;
    }
    if (tmp.magic != kMagic || tmp.version != kVersion) {
        VDBG_LN("StateStorage::load(): bad magic/version.");
        return;
    }
    if (checksum(tmp) != tmp.checksum) {
        VDBG_LN("StateStorage::load(): checksum mismatch.");
        return;
    }

    state_ = tmp;
    valid_ = true;
    VDBG("StateStorage::load(): OK, mode=%u brightness=%u rgb=%u,%u,%u\n",
         state_.mode, state_.brightness, state_.r, state_.g, state_.b);
}

void StateStorage::save(Command cmd, uint8_t brightness, uint8_t r, uint8_t g, uint8_t b) {

    SavedState next{};
    next.magic = 0x42;
    next.version = 1;
    next.mode = static_cast<uint8_t>(cmd);
    next.brightness = brightness;
    next.r = r;
    next.g = g;
    next.b = b;
    next.checksum = checksum(next);

    if (valid_ && memcmp(&next, &state_, sizeof(SavedState)) == 0) {
        VDBG_LN("StateStorage::save(): unchanged, skipping write.");
        return;
    }

    ensureFsMounted();
    if (!fsOk_) {
        VDBG_LN("StateStorage::save(): FS not OK, only caching in RAM.");
        state_ = next;
        valid_ = true;
        return;
    }

    File file(InternalFS);
    if (!file.open(kStateFile, FILE_O_WRITE)) {
        VDBG_LN("StateStorage::save(): open for write FAILED.");
        state_ = next;
        valid_ = true;
        return;
    }

    file.seek(0);
    file.truncate();

    int written = file.write(
        reinterpret_cast<const uint8_t*>(&next),
        sizeof(SavedState)
    );
    file.flush();
    file.close();

    if (written == static_cast<int>(sizeof(SavedState))) {
        state_ = next;
        valid_ = true;
        VDBG("StateStorage::save(): wrote OK, mode=%u\n", state_.mode);
    } else {
        VDBG("StateStorage::save(): short write, wrote %d\n", written);
    }
}

void StateStorage::loadName() {
    nameValid_ = false;
    ensureFsMounted();
    if (!fsOk_) return;

    File file(InternalFS);
    if (!file.open(kNameFile, FILE_O_READ)) {
        VDBG_LN("StateStorage::loadName(): file not found.");
        return;
    }
    SavedName tmp{};
    int n = file.read(&tmp, sizeof(SavedName));
    file.close();

    if (n != static_cast<int>(sizeof(SavedName))) {
        VDBG("StateStorage::loadName(): short read, got %d\n", n);
        return;
    }
    if (tmp.magic != kNameMagic || tmp.version != kNameVersion) {
        VDBG_LN("StateStorage::loadName(): bad magic/version.");
        return;
    }
    if (tmp.len == 0 || tmp.len > NAME_MAX_LEN) {
        VDBG_LN("StateStorage::loadName(): invalid len.");
        return;
    }
    if (nameChecksum(tmp) != tmp.checksum) {
        VDBG_LN("StateStorage::loadName(): checksum mismatch.");
        return;
    }
    nameBuf_ = tmp;
    nameValid_ = true;
#ifdef VORTEX_DEBUG
    char printBuf[NAME_MAX_LEN + 1] = {};
    memcpy(printBuf, tmp.name, tmp.len);
    VDBG("StateStorage::loadName(): OK, name=%s\n", printBuf);
#endif
}

void StateStorage::saveName(const char* name, uint8_t len) {
    if (len == 0 || len > NAME_MAX_LEN) {
        VDBG_LN("StateStorage::saveName(): invalid len.");
        return;
    }

    SavedName next{};
    next.magic = kNameMagic;
    next.version = kNameVersion;
    next.len = len;
    memcpy(next.name, name, len);
    next.checksum = nameChecksum(next);

    if (nameValid_ && memcmp(&next, &nameBuf_, sizeof(SavedName)) == 0) {
        VDBG_LN("StateStorage::saveName(): unchanged, skipping write.");
        return;
    }

    ensureFsMounted();
    if (!fsOk_) {
        VDBG_LN("StateStorage::saveName(): FS not OK, caching in RAM only.");
        nameBuf_ = next;
        nameValid_ = true;
        return;
    }

    File file(InternalFS);
    if (!file.open(kNameFile, FILE_O_WRITE)) {
        VDBG_LN("StateStorage::saveName(): open for write FAILED.");
        nameBuf_ = next;
        nameValid_ = true;
        return;
    }
    file.seek(0);
    file.truncate();
    int written = file.write(reinterpret_cast<const uint8_t*>(&next), sizeof(SavedName));
    file.flush();
    file.close();

    if (written == static_cast<int>(sizeof(SavedName))) {
        nameBuf_ = next;
        nameValid_ = true;
#ifdef VORTEX_DEBUG
        char printBuf[NAME_MAX_LEN + 1] = {};
        memcpy(printBuf, next.name, next.len);
        VDBG("StateStorage::saveName(): wrote OK, name=%s\n", printBuf);
#endif
    } else {
        VDBG("StateStorage::saveName(): short write, wrote %d\n", written);
    }
}

uint8_t StateStorage::nameChecksum(const SavedName& s) {
    uint8_t sum = s.magic + s.version + s.len;
    for (uint8_t i = 0; i < s.len; i++) sum += static_cast<uint8_t>(s.name[i]);
    return sum;
}

uint8_t StateStorage::checksum(const SavedState &state) {
    uint8_t sum = 0;
    sum += state.magic;
    sum += state.version;
    sum += state.mode;
    sum += state.brightness;
    sum += state.r;
    sum += state.g;
    sum += state.b;
    return sum;
}

void StateStorage::debugDump() {
    ensureFsMounted();
    if (!fsOk_) {
        Serial.println(F("StateStorage::debugDump(): FS not OK, skipping."));
        return;
    }

    File file(InternalFS);
    if (!file.open(kStateFile, FILE_O_READ)) {
        Serial.println(F("StateStorage::debugDump(): file not found. (/state.bin)"));
        return;
    }
    SavedState tmp{};
    int readBytes = file.read(&tmp, sizeof(SavedState));
    file.close();

    const uint8_t* p = reinterpret_cast<const uint8_t*>(&tmp);
    Serial.println("Raw bytes:");
    for (size_t i = 0; i < sizeof(SavedState); i++) {
        if (i % 16 == 0) Serial.println();
        if (p[i] < 16) Serial.print('0');
        Serial.print(p[i], HEX);
        Serial.print(' ');
    }
    Serial.println();
    Serial.println();

    Serial.print(F("StateStorage::debugDump(): OK\n"));
    Serial.print(F("magic: 0x")); Serial.println(tmp.magic, HEX);
    Serial.print(F("version: 0x")); Serial.println(tmp.version, HEX);
    Serial.print(F("mode: ")); Serial.println(tmp.mode);
    Serial.print(F("brightness: ")); Serial.println(tmp.brightness);
    Serial.print(F("rgb=")); Serial.print(tmp.r); Serial.print(','); Serial.print(tmp.g); Serial.print(','); Serial.println(tmp.b);

    const uint8_t calc = checksum(tmp);
    Serial.print(F("checksum(file): 0x")); Serial.println(tmp.checksum, HEX);
    Serial.print(F("checksum(calc): 0x")); Serial.println(calc, HEX);

    if (tmp.magic != kMagic || tmp.version != kVersion) {
        Serial.println(F("VALIDATION: FAIL (bad magic/version)"));
        return;
    }
    if (calc != tmp.checksum) {
        Serial.println(F("VALIDATION: FAIL (checksum mismatch)"));
        return;
    }

    Serial.println(F("VALIDATION: OK"));
}