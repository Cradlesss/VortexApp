#pragma once
#include <cstdint>
#include "core/EffectEngine.h"
#include "core/LedStrip.h"
#include "core/Commands/Commands.h"

class LedController {
public:
    struct State {
        uint8_t mode{static_cast<uint8_t>(Command::OFF)};
        uint8_t brightness{128};
        uint8_t r{0};
        uint8_t g{0};
        uint8_t b{0};
    };

    struct Config {
        uint16_t ledCount;
        uint8_t pin;
        uint8_t defaultBrightness;
        uint8_t fps;
    };

    explicit LedController(const Config& cfg);
    void begin();
    void tick(uint32_t nowMs);

    void setAnimation(uint8_t m);
    void setBrightness(uint8_t v);
    void setRgb(uint8_t r_, uint8_t g_, uint8_t b_);

    [[nodiscard]] const State& state() const noexcept {
        return current_;
    }
private:
    void restoreFromStorage_();
    void scheduleSave_();
    void maybeSave_(uint32_t nowMs);

    Config cfg_;
    LedStrip strip_;
    EffectEngine engine_;
    State current_{};
    uint32_t lastFrameMs_{0};
    bool savePending_{false};
    uint32_t lastChangeMs_{0};
    static constexpr uint32_t SAVE_DEBOUNCE_MS = 400;
};