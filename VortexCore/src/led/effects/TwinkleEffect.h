#pragma once

#include "IEffect.h"

class TwinkleEffect : public IEffect {
    public:
    void reset(LedStrip& strip) override;
    void update(LedStrip& strip, uint32_t nowMs) override;

private:
    uint32_t last_ms_ = 0;
};