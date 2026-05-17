#include "EffectEngine.h"
#include "LedStrip.h"
#include "StateStorage.h"
#include "led/effects/IEffect.h"
#include "led/effects/OffEffect.h"
#include "led/effects/RainbowEffect.h"
#include "led/effects/StaticColorEffect.h"
#include "led/effects/TransitionWithBreakEffect.h"
#include "led/effects/BeatBlueEffect.h"
#include "led/effects/BeatLightBlueEffect.h"
#include "led/effects/BlurPhaseBeatEffect.h"
#include "led/effects/MovingRainbowEffect.h"
#include "led/effects/NebulaSurgeEffect.h"
#include "led/effects/RawNoiseEffect.h"
#include "led/effects/SawToothEffect.h"
#include "led/effects/TwinkleEffect.h"
#include "led/effects/WaveEffect.h"

EffectEngine::EffectEngine(LedStrip& strip) : strip_(strip), current_(0) {}

void EffectEngine::begin() {
    installEffect(Command::OFF);
    strip_.show();
}

void EffectEngine::tick(uint32_t nowMs) {
    if (current_) {
        current_->update(strip_, nowMs);
        strip_.show();
    }
}

void EffectEngine::setCommand(Command cmd) {
    if (cmd == currentCmd_) return;
    currentCmd_ = cmd;
    installEffect(cmd);

    Serial.printf("[cmd] %s\n", getCmd().c_str());
    StateStorage::instance().save(
      currentCmd_,
      strip_.brightness(),
      staticR_, staticG_, staticB_
    );
}

void EffectEngine::setBrightness(uint8_t b) {
    strip_.setBrightness(b);
    StateStorage::instance().save(
        currentCmd_,
        strip_.brightness(),
        staticR_, staticG_, staticB_
    );
}

void EffectEngine::setSolidColor(uint8_t r, uint8_t g, uint8_t b) {
    staticR_ = r;
    staticG_ = g;
    staticB_ = b;

    if (currentCmd_ == Command::SET_STATIC_COLOR) {
        if (current_) {
            delete current_;
            current_ = nullptr;
        }
        current_ = new StaticColorEffect(staticR_, staticG_, staticB_);
        current_->reset(strip_);
        strip_.show();
        StateStorage::instance().save(currentCmd_, strip_.brightness(), staticR_, staticG_, staticB_);
    }
}

void EffectEngine::installEffect(Command cmd) {
    if (current_) {
        delete current_;
        current_ = nullptr;
    }

    switch (cmd) {
        case Command::OFF: //0
            current_ = new OffEffect();
            break;
        case Command::TWINKLE: //1
            current_ = new TwinkleEffect();
            break;
        case Command::NEBULA_SURGE: //2
            current_ = new NebulaSurgeEffect();
            break;
        case Command::TRANSITION_WITH_BREAK: //3
            current_ = new TransitionWithBreakEffect();
            break;
        case Command::BLUEB: //4
            current_ = new BlueBeatEffect();
            break;
        case Command::RAINBOW: //5
            current_ = new RainbowEffect();
            break;
        case Command::WAVE: //6
            current_ = new BlurPhaseBeatEffect();
            break;
        case Command::RUNRED: //7
            current_ = new SawToothEffect();
            break;
        case Command::RAW_NOISE: //8
            current_ = new RawNoiseEffect();
            break;
        case Command::MOVING_RAINBOW: //9 (broken)
            current_ = new OffEffect();//MovingRainbowEffect();
            break;
        case Command::BEATR: //10
            current_ = new WaveEffect();
            break;
        case Command::BLIGHTB: //11
            current_ = new BeatLightBlueEffect();
            break;
        case Command::SET_STATIC_COLOR: //13
            current_ = new StaticColorEffect(staticR_, staticG_, staticB_);
            break;
        default:
            current_ = new OffEffect();
            break; //TODO: change
    }
    if (current_)current_->reset(strip_);
}

std::string EffectEngine::getCmd() const {
    switch (currentCmd_) {
        case Command::OFF:
            return "OFF";
        case Command::TWINKLE:
            return "TWINKLE";
        case Command::NEBULA_SURGE:
            return "NEBULA_SURGE";
        case Command::TRANSITION_WITH_BREAK:
            return "TRANSITION_WITH_BREAK";
        case Command::BLUEB:
            return "BLUEB";
        case Command::RAINBOW:
            return "RAINBOW";
        case Command::BEATR:
            return "BEATR";
        case Command::RUNRED:
            return "RUNRED";
        case Command::RAW_NOISE:
            return "RAW_NOISE";
        case Command::MOVING_RAINBOW:
            return "MOVING_RAINBOW";
        case Command::WAVE:
            return "WAVE";
        case Command::BLIGHTB:
            return "BLIGHTB";
        case Command::SET_BRIGHTNESS:
            return "SET_BRIGHTNESS";
        case Command::SET_STATIC_COLOR:
            return "SET_STATIC_COLOR";
        default:
            return "UNKNOWN";
    }
}