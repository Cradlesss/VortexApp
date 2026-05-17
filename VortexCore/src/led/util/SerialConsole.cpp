#include "SerialConsole.h"

#include <bluefruit.h>
#include <cctype>
#include <cstdio>
#include <cstring>

SerialConsole::SerialConsole()
    : enabled_(false),
    setMode_(0),
    setBrightness_(0),
    printStatus_(0),
    printName_(0),
    len_(0) {
    buf_[0] = '\0';
}

void SerialConsole::attach(SetModeFn sm, SetBrightnessFn sb, PrintStatusFn ps, SetStaticColorFn sc, PrintNameFn pn) {
    setMode_ = sm;
    setBrightness_ = sb;
    printStatus_ = ps;
    setStaticColor_ = sc;
    printName_ = pn;
}

void SerialConsole::begin(unsigned long baud) {
    Serial.begin(baud);
}

void SerialConsole::setEnabled(bool en) {
    if (enabled_ == en) return;
    enabled_ = en;
    if (enabled_)
        Serial.println(F("\n[console] Ready. Type 'help'."));
    else
        len_ = 0;
}

bool SerialConsole::serialReady_() const {
#if defined(ARDUINO_ARCH_NRF52)
    return Serial;
#else
    return (bool) Serial;
#endif
}

void SerialConsole::printHelp_() {
    Serial.println(F(
      "Commands:\n"
      "  m <id>        - set mode/effect id\n"
      "  b <0-255>     - set brightness\n"
      "  c <r> <g> <b> - set static color (0-255 each)\n"
      "  s             - status\n"
      "  name          - show active and queued device name\n"
      "  reboot        - reboot device\n"
      "  dump          - read and print /state.bin\n"
      "  help          - this help"
    ));
}

void SerialConsole::processLine_(const char* line) {
    while (*line == ' ' || *line == '\t') ++line;

    if (!*line || strcasecmp(line, "help") == 0) {
        printHelp_();
        return;
    }
    if (strcasecmp(line, "reboot") == 0) {
        Serial.println(F("Rebooting..."));
        Serial.flush();
        delay(1000);

        Bluefruit.disconnect(Bluefruit.connHandle());
        delay(200);
        NVIC_SystemReset();
    }
    if (strcasecmp(line, "dump") == 0) {
        Serial.println(F("Reading /state.bin..."));
        StateStorage::instance().debugDump();
        return;
    }
    if (strcasecmp(line, "name") == 0) {
        if (printName_) printName_();
        else Serial.println(F("ERR: name handler not attached"));
        return;
    }

    if (line[0] == 'm' || line[0] == 'M') {
        int id = -1;
        if (sscanf(line + 1, "%d", &id) == 1 && id >= 0) {
            if (setMode_) { setMode_((uint8_t)id); Serial.print(F("OK mode=")); Serial.println(id); }
            else Serial.println(F("ERR: mode handler not attached"));
        } else {
            Serial.println(F("ERR usage: m <id>"));
        }
        return;
    }

    if (line[0] == 'b' || line[0] == 'B') {
        int v = -1;
        if (sscanf(line + 1, "%d", &v) == 1) {
            if (v < 0) v = 0; if (v > 255) v = 255;
            if (setBrightness_) { setBrightness_((uint8_t)v); Serial.print(F("OK brightness=")); Serial.println(v); }
            else Serial.println(F("ERR: brightness handler not attached"));
        } else {
            Serial.println(F("ERR usage: b <0-255>"));
        }
        return;
    }

    if (line[0] == 's' || line[0] == 'S') {
        if (printStatus_) printStatus_();
        else Serial.println(F("ERR: status handler not attached"));
        return;
    }

    if (line[0] == 'c' || line[0] == 'C') {
        int r = -1, g = -1, b = -1;
        if (sscanf(line + 1, "%d %d %d", &r, &g, &b) == 3) {
            if (r < 0) r = 0; if (r > 255) r = 255;
            if (g < 0) g = 0; if (g > 255) g = 255;
            if (b < 0) b = 0; if (b > 255) b = 255;

            if (setStaticColor_) {
                setStaticColor_(static_cast<uint8_t>(r), static_cast<uint8_t>(g), static_cast<uint8_t>(b));
                Serial.print(F("OK color="));
                Serial.print(r); Serial.print(F(",")); Serial.print(g); Serial.print(F(",")); Serial.println(b);
            } else
                Serial.println(F("ERR: static color handler not attached"));
        } else
            Serial.println(F("ERR usage: c <r> <g> <b>"));
        return;
    }

    Serial.println(F("ERR unknown command. Type 'help'"));
}

void SerialConsole::poll() {
    const bool ready = serialReady_();
    if (ready && !enabled_)
        setEnabled(true);
    else if (!ready && enabled_)
        setEnabled(false);

    if (!enabled_) return;

    while (Serial.available()) {
        char c = static_cast<char>(Serial.read());
        if (c == '\r') continue;
        if (c == '\n') {
            buf_[len_] = '\0';
            processLine_(buf_);
            len_ = 0;
        } else if (len_ < sizeof(buf_) - 1) {
            buf_[len_++] = c;
        }
    }
}