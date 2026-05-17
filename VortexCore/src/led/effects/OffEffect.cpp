#include "OffEffect.h"
#include "led/core/LedStrip.h"

void OffEffect::reset(LedStrip& strip) {
    strip.clear();
}

void OffEffect::update(LedStrip& strip, uint32_t nowMs) {}
