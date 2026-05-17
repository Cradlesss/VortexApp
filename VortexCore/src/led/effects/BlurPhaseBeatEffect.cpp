#include "WaveEffect.h"
#include "led/core/LedStrip.h"
#include "led/util/ColorUtil.h"
#include "led/util/MathBeat.h"

//BeatR
void WaveEffect::reset(LedStrip& strip) {
    strip.clear();
}

void WaveEffect::update(LedStrip &strip, uint32_t nowMs) {
    const auto N = strip.size();
    if (N == 0U) return;

    const uint16_t posBeat1 = beat::sinU8(30.f, 0, 255, nowMs);
    const uint16_t posBeat2 = beat::sinU8(60.f, 0, 255, nowMs);

    const uint32_t period30 = 60000UL / 30UL;
    const uint32_t period60 = 60000UL / 60UL;
    const uint8_t posBeat3 = beat::sinU8(30.f, 0, 255, nowMs + period30 / 2U);
    const uint8_t posBeat4 = beat::sinU8(60.f, 0, 255, nowMs + period60 / 2U);

    const uint8_t colBeat = beat::sinU8(45.f, 0, 255, nowMs);

    const uint8_t mix1 = static_cast<uint8_t>((static_cast<uint16_t>(posBeat1) + posBeat2) / 2U);
    const uint8_t mix2 = static_cast<uint8_t>((static_cast<uint16_t>(posBeat3) + posBeat4) / 2U);

    const uint16_t idx1 = static_cast<uint16_t>(mix1) * (N - 1U) / 255U;
    const uint16_t idx2 = static_cast<uint16_t>(mix2) * (N - 1U) / 255U;

    const uint16_t hue16 = static_cast<uint16_t>(colBeat) * 257U;

    strip.setPixel(idx1, strip.hsv(hue16, 255, 255));
    strip.setPixel(idx2, strip.hsv(hue16, 255, 255));

    for (uint16_t i = 0; i < N; ++i)
        strip.setPixel(i, color::fade(strip.getPixel(i), 10));
}