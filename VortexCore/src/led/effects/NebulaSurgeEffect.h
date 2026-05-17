#pragma once

#include "IEffect.h"

class NebulaSurgeEffect : public IEffect {
public:
    void reset(LedStrip& strip) override;
    void update(LedStrip& strip, uint32_t nowMs) override;
private:
    uint8_t wave_{0};
};