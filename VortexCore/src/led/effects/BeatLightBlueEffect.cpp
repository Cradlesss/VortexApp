#include "BeatLightBlueEffect.h"
#include "led/core/LedStrip.h"
#include "led/util/MathBeat.h"
#include "led/util/ColorUtil.h"

void BeatLightBlueEffect::reset(LedStrip& strip) {
    strip.clear();
}

void BeatLightBlueEffect::update(LedStrip& strip, uint32_t nowMs) {
    const auto N = strip.size();
    uint16_t idx = beat::sinU16(30.0f, 0, N - 1, nowMs);

    for (uint16_t i = 0; i < N; ++i)
        strip.setPixel(i, color::fade(strip.getPixel(i), 10));

    strip.setPixel(idx, 61, 133, 198);
}
