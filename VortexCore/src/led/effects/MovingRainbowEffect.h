#pragma once

#include "IEffect.h"

class MovingRainbowEffect : public IEffect {
public:
    void reset(LedStrip& strip) override;
    void update(LedStrip& strip, uint32_t nowMs) override;

private:
    uint16_t last_pos_ = 0xFFFF;
};