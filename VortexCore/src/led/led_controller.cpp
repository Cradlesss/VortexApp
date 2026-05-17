#include "led_controller.h"
#include <Arduino.h>
#include "core/LedStrip.h"
#include "core/StateStorage.h"

namespace {
    bool isValidCommand(uint8_t cmd) {
        return cmd <=static_cast<uint8_t>(Command::SET_STATIC_COLOR);
    }
}

LedController::LedController(const Config& cfg)
    : cfg_(cfg),
      strip_(LedStrip::Cfg(cfg_.ledCount, cfg_.pin, cfg_.defaultBrightness)),
      engine_(strip_) {
    current_.brightness = cfg_.defaultBrightness;
}

void LedController::begin() {
    strip_.begin();
    engine_.begin();
    restoreFromStorage_();
}

void LedController::restoreFromStorage_() {
    auto& st = StateStorage::instance();
    st.load();

    if (st.valid()) {
        const auto& s = st.data();
        current_.mode = s.mode;
        current_.brightness = s.brightness;
        current_.r = s.r;
        current_.g = s.g;
        current_.b = s.b;

        strip_.setBrightness(s.brightness);

        const Command cmd = static_cast<Command>(current_.mode);
        if (cmd == Command::SET_STATIC_COLOR) {
            engine_.setSolidColor(current_.r, current_.g, current_.b);
        }

        engine_.setCommand(cmd);
        Serial.println(F("LedController: Restored state from flash."));
    } else {
        current_.mode = static_cast<uint8_t>(Command::RAINBOW);
        strip_.setBrightness(current_.brightness);
        engine_.setCommand(Command::RAINBOW);
        Serial.println(F("LedController: No saved state; using defaults."));
    }
}

void LedController::tick(uint32_t nowMs) {
    const uint32_t frameMs = 1000ul / (cfg_.fps ? cfg_.fps : 60);
    if (nowMs - lastFrameMs_ >= frameMs) {
        lastFrameMs_ = nowMs;
        engine_.tick(nowMs);
    }
    maybeSave_(nowMs);
}

void LedController::setAnimation(const uint8_t m) {
    if (!isValidCommand(m)){
        Serial.printf("LedController: Invalid animation mode %u\n", m);
        return;
    }
    current_.mode = m;

    const Command cmd = static_cast<Command>(m);
    if (cmd == Command::SET_STATIC_COLOR) {
        engine_.setSolidColor(current_.r, current_.g, current_.b);
    }
    engine_.setCommand(cmd);
    Serial.printf("LedController::setAnimation mode=%u\n", m);
    scheduleSave_();
}

void LedController::setBrightness(const uint8_t v) {
    current_.brightness = v;
    strip_.setBrightness(v);
    Serial.printf("LedController::setBrightness brightness=%u\n", v);
    scheduleSave_();
}

void LedController::setRgb(const uint8_t r_, const uint8_t g_, const uint8_t b_) {
    current_.r = r_;
    current_.g = g_;
    current_.b = b_;
    engine_.setSolidColor(r_, g_, b_);
    engine_.setCommand(Command::SET_STATIC_COLOR);
    current_.mode = static_cast<uint8_t>(Command::SET_STATIC_COLOR);
    Serial.printf("LedController::setRgb r=%u g=%u b=%u\n", r_, g_, b_);
    scheduleSave_();
}

void LedController::scheduleSave_() {
    savePending_ = true;
    lastChangeMs_ = millis();
}

void LedController::maybeSave_(uint32_t nowMs) {
    if (!savePending_) return;
    if (nowMs - lastChangeMs_ < SAVE_DEBOUNCE_MS) return;

    savePending_ = false;
    StateStorage::instance().save(
        static_cast<Command>(current_.mode),
        current_.brightness,
        current_.r,
        current_.g,
        current_.b
    );
}