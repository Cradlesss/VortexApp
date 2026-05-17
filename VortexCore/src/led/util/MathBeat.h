#pragma once
#include <cstdint>
#include <cmath>

namespace beat {
    constexpr float kTwoPi = 6.28318530717958647692f;

    inline uint16_t sinU16(float bpm, uint16_t lo, uint16_t hi, uint32_t t_ms) {
        const float phase = kTwoPi * (bpm / 60000.0f) * static_cast<float>(t_ms);
        float s = 0.5f * (sinf(phase) + 1.0f);
        if (s >= 0.9995f) return hi;
        if (s <= 0.0005f) return lo;
        const float pos = lo + s * (hi - lo);
        uint16_t idx = static_cast<uint16_t>(pos +0.5f);
        if (idx > hi) idx = hi;
        return idx;
    }

    inline uint8_t sinU8(float bpm, uint8_t lo, uint8_t hi, uint32_t t_ms) {
        return static_cast<uint8_t>(sinU16(bpm, lo, hi, t_ms));
    }
}