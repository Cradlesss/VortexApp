#pragma once
#include <cstdint>

namespace color {
    inline uint8_t subSat(uint8_t v, uint8_t dec) {
        return (v > dec) ? (v - dec) : 0;
    }

    inline uint32_t fade(uint32_t c, uint8_t amount) {
        uint8_t r = (c >> 16) & 0xFF;
        uint8_t g = (c >> 8) & 0xFF;
        uint8_t b = (c >> 0) & 0xFF;

        r = subSat(r, amount);
        g = subSat(g, amount);
        b = subSat(b, amount);
        return (static_cast<uint32_t>(r) << 16) | (static_cast<uint32_t>(g) << 8) | b;
    }
}
