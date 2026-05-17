#pragma once
#include <Adafruit_NeoPixel.h>
#include <Arduino.h>

class LedStrip {
public:
    struct Cfg {
        uint16_t numLeds{100};
        uint8_t pin{D6};
        uint8_t defaultBrightness{60};
        static constexpr uint16_t kMaxLeds = 1024;

        Cfg(uint8_t numLeds, uint8_t pin, uint8_t defaultBrightness) : numLeds(numLeds), pin(pin), defaultBrightness(defaultBrightness) {}
    };

    explicit LedStrip(const Cfg& cfg);

    void begin();
    void clear();
    void setPixel(uint16_t i, uint8_t r, uint8_t g, uint8_t b);
    void setPixel(uint16_t i, uint32_t grbPacked);
    void fill(uint8_t r, uint8_t g, uint8_t b);
    void show();
    void setBrightness(uint8_t b);
    uint8_t brightness() const;

    uint16_t size() const;
    uint32_t getPixel(uint16_t i) const;

    static uint32_t pack(uint8_t r, uint8_t g, uint8_t b);
    uint32_t hsv(uint16_t hue, uint8_t sat, uint8_t val) const;

private:
    Cfg cfg_;
    Adafruit_NeoPixel strip_;
    uint8_t brightness_;
};