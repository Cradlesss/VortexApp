#include "LedStrip.h"

LedStrip::LedStrip(const Cfg& cfg)
    : cfg_(cfg),
    strip_(cfg_.numLeds, cfg_.pin, NEO_GRB + NEO_KHZ800),
    brightness_(cfg_.defaultBrightness) {}

void LedStrip::begin() {
    strip_.begin();
    strip_.clear();
    strip_.setBrightness(brightness_);
    strip_.show();
}

void LedStrip::clear() {
    strip_.clear();
}

void LedStrip::setPixel(uint16_t i, uint8_t r, uint8_t g, uint8_t b) {
    strip_.setPixelColor(i, strip_.Color(r, g, b));
}

void LedStrip::setPixel(uint16_t i, uint32_t grbPacked) {
    strip_.setPixelColor(i, grbPacked);
}

void LedStrip::fill(uint8_t r, uint8_t g, uint8_t b) {
    for (uint16_t i = 0; i < cfg_.numLeds; ++i)
        setPixel(i, r, g, b);
}

void LedStrip::show() {
    strip_.setBrightness(brightness_);
    strip_.show();
}

void LedStrip::setBrightness(uint8_t b) {
    brightness_ = b;
    strip_.setBrightness(brightness_);
}

uint8_t LedStrip::brightness() const {
    return brightness_;
}

uint16_t LedStrip::size() const {
    return cfg_.numLeds;
}

uint32_t LedStrip::getPixel(uint16_t i) const {
    return strip_.getPixelColor(i);
}

uint32_t LedStrip::pack(uint8_t r, uint8_t g, uint8_t b) {
    return Adafruit_NeoPixel::Color(r, g, b);
}

uint32_t LedStrip::hsv(uint16_t hue, uint8_t sat, uint8_t val) const {
    return strip_.gamma32(strip_.ColorHSV(hue, sat, val));
}