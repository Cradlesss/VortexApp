#pragma once

#include "IEffect.h"

class RunRainbowTempEffect : public IEffect {
public:
    void reset(LedStrip& strip) override;
    void update(LedStrip& strip, uint32_t nowMs) override;

private:
    uint8_t hue_{0};
    uint32_t lastHueMs_{0};
};