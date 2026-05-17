#include "BlurPhaseBeatEffect.h"
#include "led/core/LedStrip.h"
#include "led/util/MathBeat.h"
#include "led/util/ColorUtil.h"

//Wave
namespace {
    // const uint16_t kMaxBlurLeds = 100;
    uint32_t sBlurBuf[LedStrip::Cfg::kMaxLeds];

    uint32_t addColor(uint32_t base, uint8_t r, uint8_t g, uint8_t b) {
        uint8_t br = static_cast<uint8_t>((base >> 16) & 0xFFU);
        uint8_t bg = static_cast<uint8_t>((base >> 8) & 0xFFU);
        uint8_t bb = static_cast<uint8_t>(base & 0xFFU);

        uint16_t nr = static_cast<uint16_t>(br) + r;
        uint16_t ng = static_cast<uint16_t>(bg) + g;
        uint16_t nb = static_cast<uint16_t>(bb) + b;

        if (nr > 255U) nr = 255U;
        if (ng > 255U) ng = 255U;
        if (nb > 255U) nb = 255U;

        return (nr << 16) | (ng << 8) | nb;
    }
}

void BlurPhaseBeatEffect::reset(LedStrip &strip) {
    strip.clear();
}

void BlurPhaseBeatEffect::update(LedStrip &strip, uint32_t nowMs) {
    const auto N = strip.size();
    if (N == 0U) return;

    const uint16_t maxN = (N < LedStrip::Cfg::kMaxLeds) ? N : LedStrip::Cfg::kMaxLeds;

    const uint16_t period = 60000UL / 30UL;
    const uint16_t p1 = beat::sinU16(30.0f, 0U, static_cast<uint16_t>(maxN - 1U), nowMs);
    const uint16_t p2 = beat::sinU16(30.0f, 0U, static_cast<uint16_t>(maxN - 1U), nowMs + period / 3U);
    const uint16_t p3 = beat::sinU16(30.0f, 0U, static_cast<uint16_t>(maxN - 1U), nowMs + (2U * period) / 3U);

    strip.setPixel(p1, addColor(strip.getPixel(p1), 0, 255,0));
    strip.setPixel(p2, addColor(strip.getPixel(p2), 0, 0,255));
    strip.setPixel(p3, addColor(strip.getPixel(p3), 255,0,0));

    for (uint16_t i = 0; i < maxN; ++i)
        sBlurBuf[i] = strip.getPixel(i);

    for (uint8_t pass = 0; pass < 4U; ++pass) {
        for (uint16_t i = 0; i < maxN; ++i) {
            const uint32_t cL = sBlurBuf[(i == 0) ? 0U : i - 2U];
            const uint32_t cC = sBlurBuf[i];
            const uint32_t cR = sBlurBuf[(i + 1U < maxN) ? i + 1U : maxN - 1U];

            const uint16_t r = static_cast<uint16_t>((cL >> 16) & 0xFFU) + 2U * static_cast<uint16_t>((cC >> 16) & 0xFFU) + static_cast<uint16_t>((cR >> 16) & 0xFFU);
            const uint16_t g = static_cast<uint16_t>((cL >> 8) & 0xFFU) + 2U * static_cast<uint16_t>((cC >> 8) & 0xFFU) + static_cast<uint16_t>((cR >> 8) & 0xFFU);
            const uint16_t b = static_cast<uint16_t>(cL & 0xFFU) + 2U * static_cast<uint16_t>(cC & 0xFFU) + static_cast<uint16_t>(cR & 0xFFU);

            const uint8_t nr = static_cast<uint8_t>(r >> 2);
            const uint8_t ng = static_cast<uint8_t>(g >> 2);
            const uint8_t nb = static_cast<uint8_t>(b >> 2);

            sBlurBuf[i] = (static_cast<uint32_t>(nr) << 16) | (static_cast<uint32_t>(ng) << 8) | (static_cast<uint32_t>(nb));
        }
    }

    for (uint16_t i = 0; i < maxN; ++i)
        strip.setPixel(i, sBlurBuf[i]);
}