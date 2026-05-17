#include "StaticColorEffect.h"
#include "led/core/LedStrip.h"

void StaticColorEffect::reset(LedStrip& strip) {
    strip.fill(r_, g_, b_);
}

void StaticColorEffect::update(LedStrip &strip, uint32_t nowMs) {}

