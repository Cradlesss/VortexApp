#include "RainbowEffect.h"
#include "led/core/LedStrip.h"
#include "led/util/MathBeat.h"

void RainbowEffect::reset(LedStrip&) {}

void RainbowEffect::update(LedStrip& strip, const uint32_t nowMs) {
    const auto N = strip.size();
    if (N == 0U) return;

    const uint8_t beatA = beat::sinU8(30.f, 0, 255, nowMs);
    const uint8_t beatB = beat::sinU8(20.f, 0, 255, nowMs);
    const auto baseHue = static_cast<uint8_t>((beatA + beatB) /2U);

    for (uint16_t i = 0; i < N; ++i) {
        const auto hue8 = static_cast<uint8_t>((baseHue + static_cast<uint8_t>(i * 8U)));
        const auto hue16 = static_cast<uint16_t>(hue8) * 257U;
        strip.setPixel(i, strip.hsv(hue16, 255, 255));
    }
}