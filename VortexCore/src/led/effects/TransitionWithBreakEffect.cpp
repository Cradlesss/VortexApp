#include "TransitionWithBreakEffect.h"
#include "led/core/LedStrip.h"

void TransitionWithBreakEffect::reset(LedStrip& strip) {
    cursor_ = 0;
    forward_ = true;
    last_ = 0;
    strip.fill(93, 58, 147);
}

void TransitionWithBreakEffect::update(LedStrip& strip, uint32_t nowMs) {
    constexpr uint32_t interval = 50;
    if (nowMs - last_ < interval) return;
    last_ = nowMs;

    const auto N = static_cast<int32_t>(strip.size());
    strip.fill(93, 58, 147);

    if (forward_) {
        if (cursor_ >= 0)
            strip.setPixel(cursor_ -1, 0, 0, 0);
        strip.setPixel(cursor_, 127, 1, 247);
        if (++cursor_ >= N) {
            cursor_ = N - 1;
            forward_ = false;
        }
    } else {
        if (cursor_ + 1 < N)
            strip.setPixel(cursor_ + 1, 0, 0, 0);
        strip.setPixel(cursor_, 41, 134, 204);
        if (cursor_ == 0)
            forward_ = true;
        else
            --cursor_;
    }
}
