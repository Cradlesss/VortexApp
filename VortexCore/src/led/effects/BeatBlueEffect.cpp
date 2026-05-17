#include "BeatBlueEffect.h"
#include "led/core/LedStrip.h"
#include "led/util/MathBeat.h"

void BlueBeatEffect::reset(LedStrip &strip) {
    strip.clear();
}

void BlueBeatEffect::update(LedStrip &strip, uint32_t nowMs) {
    uint16_t idx = beat::sinU16(30.f, 0, strip.size() -1, nowMs);

    for (uint16_t i = 0; i < strip.size(); ++i) {
        auto c = strip.getPixel(i);
        uint8_t r=(c>>16)&0xFF, g=(c>>8)&0xFF, b=c&0xFF;
        r = (r > 10) ? r - 10 : 0;
        g = (g > 10) ? g - 10 : 0;
        b = (b > 10) ? b - 10 : 0;
        strip.setPixel(i, r, g, b);
    }
    strip.setPixel(idx, 0, 0, 255);
}

