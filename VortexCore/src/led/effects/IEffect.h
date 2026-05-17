#pragma once

#include <cstdint>
#include "led/core/LedStrip.h"

class LedStrip;

class IEffect {
public:
    virtual ~IEffect() = default;
    virtual void reset(LedStrip& strip) = 0;
    virtual void update(LedStrip& strip, uint32_t nowMs) = 0;
};