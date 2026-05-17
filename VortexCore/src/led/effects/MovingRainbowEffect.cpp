#include "MovingRainbowEffect.h"
#include "led/util/ColorUtil.h"

namespace {
    uint32_t sBackGround[LedStrip::Cfg::kMaxLeds];
    uint32_t sLastBlendMs = 0u;
    uint32_t sLastFrameMs = 0u;

    uint8_t noise1d(uint16_t x, uint32_t t) {
        uint32_t n = static_cast<uint32_t>(x) + t * 131u;
        n = (n <<13) ^ n;
        return static_cast<uint8_t>((n * (n * n * 15731u + 789221u) + 1376312589u) >> 24);
    }

    uint16_t noise1d16Smooth(uint16_t x, uint32_t t) {
        const uint8_t  kShift = 5u;
        const uint32_t kMask  = (1ul << kShift) - 1ul;

        const uint32_t base = t >> kShift;
        const uint8_t  frac = static_cast<uint8_t>(t & kMask);

        const uint16_t w2 = static_cast<uint16_t>(frac) * 255u / static_cast<uint16_t>(kMask);
        const uint16_t w1 = 255u - w2;

        const uint8_t n1 = noise1d(x, base);
        const uint8_t n2 = noise1d(x, base + 1u);

        uint16_t v = static_cast<uint16_t>(n1) * w1 + static_cast<uint16_t>(n2) * w2;
        v /= 255u;
        return v * 257u;
    }

    uint32_t mixColor(uint32_t a, uint32_t b, uint8_t amount) {
        const uint8_t ar = static_cast<uint8_t>((a >> 16) & 0xFFu);
        const uint8_t ag = static_cast<uint8_t>((a >> 8) & 0xFFu);
        const uint8_t ab = static_cast<uint8_t>(a & 0xFFu);

        const uint8_t br = static_cast<uint8_t>((b >> 16) & 0xFFu);
        const uint8_t bg = static_cast<uint8_t>((b >> 8) & 0xFFu);
        const uint8_t bb = static_cast<uint8_t>(b & 0xFFu);

        const uint16_t inv = static_cast<uint16_t>(255u - amount);

        const uint8_t r = static_cast<uint8_t>(
            (static_cast<uint16_t>(ar) * inv + static_cast<uint16_t>(br) * amount) / 255u
        );

        const uint8_t g = static_cast<uint8_t>(
            (static_cast<uint16_t>(ag) * inv + static_cast<uint16_t>(bg) * amount) / 255u
        );
        const uint8_t b2 = static_cast<uint8_t>(
            (static_cast<uint16_t>(ab) * inv + static_cast<uint16_t>(bb) * amount) / 255u
        );

        return (static_cast<uint32_t>(r) << 16) | (static_cast<uint32_t>(g) << 8) | static_cast<uint32_t>(b2);
    }
}

void MovingRainbowEffect::reset(LedStrip& strip) {
    const auto N = strip.size();
    if (N == 0u) return;

    const uint16_t maxN = (N < LedStrip::Cfg::kMaxLeds) ? N : LedStrip::Cfg::kMaxLeds;

    for (uint16_t i = 0; i < maxN; ++i)
        sBackGround[i] = 0u;

    last_pos_ = 0xFFFFu;
    sLastBlendMs = 0u;
    sLastFrameMs = 0u;
    strip.clear();
}

void MovingRainbowEffect::update(LedStrip& strip, const uint32_t nowMs) {
    const auto N = strip.size();
    if (N == 0U) return;

    if (sLastFrameMs != 0u) {
        const uint32_t dt = nowMs - sLastFrameMs;
        if (dt < 50u) return;
    }
    sLastFrameMs = nowMs;

    const uint16_t maxN = (N < LedStrip::Cfg::kMaxLeds) ? N : LedStrip::Cfg::kMaxLeds;

    const uint32_t tHue = nowMs / 3u;
    const uint32_t tVal = nowMs / 4u;

    for (uint16_t i = 0; i < maxN; ++i) {
        const uint16_t xHue = static_cast<uint16_t>(i * 30u);
        const uint16_t xVal = static_cast<uint16_t>(i * 30u + 123u);
        const uint16_t nHue16 = noise1d16Smooth(xHue, tHue);
        const uint16_t nVal16 = noise1d16Smooth(xVal, tVal);
        const uint8_t hue8Base = static_cast<uint8_t>(nHue16 >> 8);
        const uint8_t hue8 = static_cast<uint8_t>(hue8Base + 10u);
        const uint8_t val8Base = static_cast<uint8_t>(nVal16 >> 8);
        const uint8_t val = static_cast<uint8_t>(
            96u + (static_cast<uint16_t>(val8Base) * 160u) / 255u
        );

        const uint16_t hue16 = static_cast<uint16_t>(hue8) * 257u;

        sBackGround[i] = strip.hsv(hue16, 255u, val);
    }
    {
        const uint16_t rawNoise = noise1d16Smooth(0u, nowMs * 100u); //Maybe change to /100
        uint16_t pos = rawNoise;

        if (pos < 13000u) pos = 13000u;
        if (pos > 51000u) pos = 51000u;

        pos = static_cast<uint16_t>(
            (static_cast<uint32_t>(pos - 13000u) * static_cast<uint32_t>(N - 1u))/
            static_cast<uint32_t>(51000u - 13000u)
        );

        strip.setPixel(pos, 255u, 0u, 0u);
        last_pos_ = pos;
    }

    if (sLastBlendMs == 0u || (nowMs - sLastBlendMs) > 20u) {
        for (uint16_t i = 0; i < maxN; ++i) {
            uint32_t cur = strip.getPixel(i);
            cur = color::fade(cur, 10u);
            cur = mixColor(cur, sBackGround[i], 30u);
            strip.setPixel(i, cur);
        }
    }
}