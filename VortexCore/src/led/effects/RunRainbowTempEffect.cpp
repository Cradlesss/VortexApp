#include "RunRainbowTempEffect.h"
#include "led/core/LedStrip.h"
#include "led/util/ColorUtil.h"

namespace {
    uint16_t beatSawU16(uint16_t bpm, uint16_t lo, uint16_t hi, uint32_t nowMs) {
        if (hi <= lo) return lo;

        const auto span = static_cast<uint32_t>(hi - lo);

        const auto scaled = static_cast<uint64_t>(nowMs) * static_cast<uint64_t>(bpm) * 65536ULL;

        const auto beat = static_cast<uint32_t>(scaled / 60000UL);
        const auto base16 = static_cast<uint16_t>(beat & 0xFFFFU);

        const auto pos = static_cast<uint16_t>(lo + (static_cast<uint32_t>(base16) * span) / 65535UL);
        return pos;
    }
}

void RunRainbowTempEffect::reset(LedStrip& strip) {
    strip.clear();
    hue_ = 0;
    lastHueMs_ = 0;
}

void RunRainbowTempEffect::update(LedStrip& strip, uint32_t nowMs) {
    const auto N = strip.size();
    if (N == 0U) return;

    const uint16_t pos = beatSawU16(40U, 0U, static_cast<uint16_t>(N - 1U), nowMs);

    for (uint16_t i = 0; i < N; ++i)
        strip.setPixel(i, color::fade(strip.getPixel(i), 3));

    const uint16_t hue16 = static_cast<uint16_t>(hue_) << 8;
    strip.setPixel(pos, strip.hsv(hue16, 200, 255));

    if (nowMs - lastHueMs_ >= 10U) {
        ++hue_;
        lastHueMs_ = nowMs;
    }
}