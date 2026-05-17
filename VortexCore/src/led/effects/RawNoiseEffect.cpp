#include "RawNoiseEffect.h"

#include "led/core/LedStrip.h"


namespace {
    const uint32_t kPalette[16] = {
        0x5500AB, 0x84007C, 0xB5004B, 0xE5001A,
        0xE81700, 0xB84700, 0xAB7700, 0xABAB00,
        0xAB5500, 0xDD2200, 0xF2000E, 0xC2003D,
        0x8F0070, 0x5F00A0, 0x2F00D0, 0x0007F6
    };

    uint32_t partyColor(uint8_t index, uint8_t brightness) {
        const uint8_t idx1 = index >> 4;
        const uint8_t idx2 = (idx1 + 1U) & 0x0FU;
        const uint8_t frac = index & 0x0FU;

        const uint32_t c1 = kPalette[idx1];
        const uint32_t c2 = kPalette[idx2];

        const uint8_t r1 = static_cast<uint8_t>((c1 >> 16) & 0xFFU);
        const uint8_t g1 = static_cast<uint8_t>((c1 >> 8) & 0xFFU);
        const uint8_t b1 = static_cast<uint8_t>(c1 & 0xFFU);

        const uint8_t r2 = static_cast<uint8_t>((c2 >> 16) & 0xFFU);
        const uint8_t g2 = static_cast<uint8_t>((c2 >> 8) & 0xFFU);
        const uint8_t b2 = static_cast<uint8_t>(c2 & 0xFFU);

        const uint16_t w1 = static_cast<uint16_t>(16U - frac);
        const uint16_t w2 = frac;

        uint16_t r = static_cast<uint16_t>(r1) * w1 + static_cast<uint16_t>(r2) * w2;
        uint16_t g = static_cast<uint16_t>(g1) * w1 + static_cast<uint16_t>(g2) * w2;
        uint16_t b = static_cast<uint16_t>(b1) * w1 + static_cast<uint16_t>(b2) * w2;

        r >>= 4;
        g >>= 4;
        b >>= 4;

        const uint16_t br = brightness;

        r = (r * br) / 255U;
        g = (g * br) / 255U;
        b = (b * br) / 255U;

        if (r > 255U) r = 255U;
        if (g > 255U) g = 255U;
        if (b > 255U) b = 255U;

        return (r << 16) | (g << 8) | b;
    }

    const uint16_t kMaxNoiseLeds = 100;
    uint8_t sNoise[kMaxNoiseLeds];

    uint8_t noise1d(uint16_t x, uint32_t t) {
        uint32_t n = static_cast<uint32_t>(x) + t * 131U;
        n = (n <<13) ^ n;
        return static_cast<uint8_t>((n * (n * n * 15731U + 789221U) + 1376312589U) >> 24);
    }
}

void RawNoiseEffect::reset(LedStrip &strip) {
    strip.clear();
}

void RawNoiseEffect::update(LedStrip &strip, uint32_t nowMs) {
    const auto N = strip.size();
    if (N == 0U) return;

    const uint16_t maxN = (N < kMaxNoiseLeds) ? N : kMaxNoiseLeds;

    const uint32_t tVal = nowMs >> 5;

    for (uint16_t i = 0; i < maxN; ++i)
        sNoise[i] = noise1d(static_cast<uint16_t>(i * 50U), tVal);

    for (uint8_t pass = 0; pass < 2U; ++pass) {
        uint8_t prev = sNoise[0];
        for (uint16_t i = 1; i < maxN; ++i) {
            const uint8_t cur = sNoise[i];
            sNoise[i - 1] = static_cast<uint8_t>((static_cast<uint16_t>(prev) + cur) / 2U);
            prev = cur;
        }
    }

    for (uint16_t i = 0; i < maxN; ++i) {
        const uint8_t idx = sNoise[i];
        const uint8_t bri = sNoise[maxN - 1U - i];
        strip.setPixel(i, partyColor(idx, bri));
    }
}