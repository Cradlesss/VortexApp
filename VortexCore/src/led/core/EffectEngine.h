#pragma once
#include <string>
#include "Commands/Commands.h"
#include "led/effects/IEffect.h"

class LedStrip;
class IEffect;

class EffectEngine {
public:
    explicit EffectEngine(LedStrip& strip);
    void begin();
    void tick(uint32_t nowMs);

    void setCommand(Command cmd);
    void setBrightness(uint8_t b);
    void setSolidColor(uint8_t r, uint8_t g, uint8_t b);
    std::string getCmd() const;

    ~EffectEngine(){}
private:
    void installEffect(Command cmd);

    LedStrip& strip_;
    IEffect* current_;
    Command currentCmd_{Command::OFF};

    uint8_t staticR_{0};
    uint8_t staticG_{0};
    uint8_t staticB_{0};
};