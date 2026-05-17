#include "NebulaSurgeEffect.h"
#include "led/core/LedStrip.h"
#include "led/util/MathBeat.h"

static uint8_t lerp8(const uint8_t a, const uint8_t b, const uint8_t t) {
    return static_cast<uint8_t>(( static_cast<uint16_t>(a)*(255 - t) + static_cast<uint16_t>(b)*t ) / 255);
}

void NebulaSurgeEffect::reset(LedStrip& strip) {
    strip.clear();
    wave_ = 0;
}

void NebulaSurgeEffect::update(LedStrip& strip, const uint32_t nowMs) {
    const auto pulse = beat::sinU8(20.0f, 128, 255, nowMs);
    const auto shift = beat::sinU8(10.0f, 0, 255, nowMs);
    const auto N = strip.size();

    for (uint16_t i = 0; i < N; ++i) {
        const auto idx = static_cast<uint8_t>(wave_ + (i * 15));
        const auto t1 = static_cast<uint8_t>(idx + shift);
        const auto t2 = static_cast<uint8_t>(idx - shift);

        auto r = lerp8(lerp8(0x8E, 0x00, t1), 0x16, t2);
        auto g = lerp8(lerp8(0x05, 0x61, t1), 0xA0, t2);
        auto b = lerp8(lerp8(0xC2, 0x3B, t1), 0x85, t2);

        r = static_cast<uint8_t>(r * pulse / 255);
        g = static_cast<uint8_t>(g * pulse / 255);
        b = static_cast<uint8_t>(b * pulse / 255);

        const auto prev = strip.getPixel(i);
        const uint8_t pr = (prev >> 16) & 0xFF;
        const uint8_t pg = (prev >> 8) & 0xFF;
        const uint8_t pb = prev & 0xFF;
        constexpr uint8_t alpha = 32;

        r = lerp8(pr, r, alpha);
        g = lerp8(pg, g, alpha);
        b = lerp8(pb, b, alpha);

        strip.setPixel(i, r, g, b);
    }
    wave_ += 2;
}