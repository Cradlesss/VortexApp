#include "TwinkleEffect.h"
#include "led/core/LedStrip.h"


namespace {
    constexpr uint8_t TWINKLE_SPEED = 3U;
    constexpr uint8_t TWINKLE_DENSITY = 8U;
    constexpr uint32_t BACKGROUND_COLOR = 0x000000UL;

    uint8_t fastSin8(uint8_t x) {
        const float angle = static_cast<float>(x) * 6.28318530718f / 255.f;
        float s = sinf(angle) * 127.5f + 127.5f;
        int v = static_cast<int>(s + 0.5f);

        if (v < 0) v = 0;
        if (v > 255) v = 255;
        return static_cast<uint8_t>(v);
    }

    uint8_t attackDecayWave8(uint8_t i) {
        if (i < 86U)
            return static_cast<uint8_t>(i * 3U);
        else {
            i = static_cast<uint8_t>(i - 86U);
            return static_cast<uint8_t>(255U - static_cast<uint8_t>(i + i / 2U));
        }
    }

    uint32_t coolLikeIncandescent(uint32_t c, uint8_t phase) {
        if (phase < 128U) return c;

        uint32_t r = static_cast<uint8_t>((c >> 16) & 0xFFU);
        uint32_t g = static_cast<uint8_t>((c >> 8) & 0xFFU);
        uint32_t b = static_cast<uint8_t>(c & 0xFFU);

        const uint8_t cooling = static_cast<uint8_t>((phase - 128U) >> 4);

        if (g > cooling)
            g = static_cast<uint8_t>(g - cooling);
        else
            g = 0U;

        const uint8_t cb = static_cast<uint8_t>(cooling * 2U);
        if (b > cb)
            b = static_cast<uint8_t>(b - cb);
        else
            b = 0U;

        return (static_cast<uint32_t>(r) << 16) | (static_cast<uint32_t>(g) << 8) | (static_cast<uint32_t>(b));
    }

    uint8_t computeTwinkle(uint32_t ms, uint8_t salt, uint8_t& outHue, uint8_t& outPhase) {
        uint16_t ticks = static_cast<uint16_t>(ms >> (8U - TWINKLE_SPEED));
        uint8_t fastcycle8 = static_cast<uint8_t>(ticks);
        uint16_t slowcycle16 = static_cast<uint16_t>((ticks >> 8) + salt);

        slowcycle16 = static_cast<uint16_t>(slowcycle16 + fastSin8(static_cast<uint8_t>(slowcycle16)));
        slowcycle16 = static_cast<uint16_t>(slowcycle16 * 2053 + 1384U);
        uint8_t slowcycle8 = static_cast<uint8_t>((slowcycle16 & 0xFFU) + (slowcycle16 >> 8));

        uint8_t bright = 0U;
        if (((slowcycle8 & 0x0EU) >> 1) < TWINKLE_DENSITY)
            bright = attackDecayWave8(fastcycle8);

        outHue = static_cast<uint8_t>(slowcycle8 - salt);
        outPhase = fastcycle8;
        return bright;
    }
}

void TwinkleEffect::reset(LedStrip& strip) {
    strip.clear();
}

void TwinkleEffect::update(LedStrip& strip, uint32_t nowMs) {
    const auto N = strip.size();
    if (N == 0U) return;

    uint16_t PRNG16 = 11337U;
    const uint32_t clock32 = nowMs;

    for (uint16_t i = 0; i < N; ++i) {
        PRNG16 = static_cast<uint16_t>(PRNG16 * 2053U + 1384U);
        const uint16_t myclockoffset16 = PRNG16;

        PRNG16 = static_cast<uint16_t>(PRNG16 * 2053U + 1384U);
        const uint8_t myspeedmultiplierQ5_3 = static_cast<uint8_t>(
          ((((PRNG16 & 0xFFU) >> 4) + (PRNG16 & 0xFU)) & 0x0FU) + 0x08U
        );

        const uint32_t myclock30 = ((clock32 * myspeedmultiplierQ5_3) >> 3) + myclockoffset16;

        const uint8_t myunique8 = static_cast<uint8_t>(PRNG16 >> 8);

        uint8_t hue8;
        uint8_t phase;

        const uint8_t bright = computeTwinkle(myclock30, myunique8, hue8, phase);

        uint32_t color;
        if (bright > 0U) {
            const uint16_t hue16 = static_cast<uint16_t>(hue8) * 257U;
            color = strip.hsv(hue16, 255, bright);
            color = coolLikeIncandescent(color, phase);
        } else
            color = BACKGROUND_COLOR;

        strip.setPixel(i, color);
    }
}