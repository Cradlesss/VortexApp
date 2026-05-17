#pragma once

#include "IEffect.h"

class StaticColorEffect : public IEffect {
public:
    StaticColorEffect(uint8_t r, uint8_t g, uint8_t b) : r_(r), g_(g), b_(b) {}
    void reset(LedStrip& strip) override;
    void update(LedStrip& strip, uint32_t nowMs) override;

    void set(uint8_t r, uint8_t g, uint8_t b) {
        r_ = r;
        g_ = g;
        b_ = b;
    }
private:
    uint8_t r_;
    uint8_t g_;
    uint8_t b_;
};