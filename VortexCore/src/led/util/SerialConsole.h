#pragma once
#include <Arduino.h>
#include <led/core/StateStorage.h>

class SerialConsole {
public:
    typedef void (*SetModeFn)(uint8_t);
    typedef void (*SetBrightnessFn)(uint8_t);
    typedef void (*PrintStatusFn)();
    typedef void (*SetStaticColorFn)(uint8_t, uint8_t, uint8_t);
    typedef void (*PrintNameFn)();

    SerialConsole();

    void attach(SetModeFn sm, SetBrightnessFn sb, PrintStatusFn ps, SetStaticColorFn sc, PrintNameFn pn);
    void begin(unsigned long baud);
    void poll();

    void setEnabled(bool en);
    bool enabled() const {
        return enabled_;
    }

    private:
    bool enabled_;
    SetModeFn setMode_;
    SetBrightnessFn setBrightness_;
    PrintStatusFn printStatus_;
    SetStaticColorFn setStaticColor_;
    PrintNameFn printName_;
    char buf_[64];
    uint8_t len_;

    void processLine_(const char* line);
    void printHelp_();
    bool serialReady_() const;
};