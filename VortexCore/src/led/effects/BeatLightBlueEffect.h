#pragma once

#include "IEffect.h"

class BeatLightBlueEffect : public IEffect {
public:
    void reset(LedStrip& strip) override;
    void update(LedStrip& strip, uint32_t nowMs) override;
};