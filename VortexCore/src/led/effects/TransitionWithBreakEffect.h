#pragma once

#include "IEffect.h"

class TransitionWithBreakEffect : public IEffect {
public:
    void reset(LedStrip& strip) override;
    void update(LedStrip& strip, uint32_t nowMs) override;

private:
    int32_t cursor_ = 0;
    bool forward_ = true;
    uint32_t last_ = 0;
};