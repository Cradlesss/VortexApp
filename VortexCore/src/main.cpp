#include <Arduino.h>
#include "led/led_controller.h"
#include "ble/ble_led_service.h"
#include "led/util/SerialConsole.h"

namespace {
    constexpr uint8_t LED_PIN = D10;
    constexpr uint16_t LED_CNT = 100;
    constexpr uint8_t DEFAULT_BRIGHTNESS = 60;
    constexpr uint8_t FPS = 60;

    LedController g_controller({LED_CNT, LED_PIN, DEFAULT_BRIGHTNESS, FPS});
    BleLedService g_ble(g_controller);
    SerialConsole g_console;

    void cbSetMode(uint8_t id) {
        g_controller.setAnimation(id);
    }

    void cbSetBrightness(uint8_t val) {
        g_controller.setBrightness(val);
    }

    void cbSetStaticColor(uint8_t r, uint8_t g, uint8_t b) {
        g_controller.setRgb(r, g, b);
    }

    void cbPrintStatus() {
        const auto& st = g_controller.state();
        Serial.print(F("mode=")); Serial.print(st.mode);
        Serial.print(F(" brightness=")); Serial.println(st.brightness);
        Serial.print(F("rgb=")); Serial.print(st.r); Serial.print(','); Serial.print(st.g); Serial.print(','); Serial.println(st.b);
    }

    void cbPrintName() {
        const char* active  = g_ble.activeName();
        const char* pending = g_ble.pendingName();
        Serial.print(F("Active name:  ")); Serial.println(active[0] ? active : "(default)");
        if (pending[0]) {
            Serial.print(F("Queued name:  ")); Serial.print(pending);
            Serial.println(g_ble.isNameSavePending() ? F(" (pending write)") : F(" (saved, takes effect on next boot)"));
        } else {
            Serial.println(F("Queued name:  (none)"));
        }
    }
}

void setup() {
    Serial.begin(115200);
    const unsigned long t0 = millis();
    while (!Serial && (millis() - t0) < 1500) {}

    Serial.println("Vortex NeoPixel driver (BLE).");
    g_controller.begin();

    g_console.attach(cbSetMode, cbSetBrightness, cbPrintStatus, cbSetStaticColor, cbPrintName);

    g_ble.begin();
#ifdef VORTEX_DEBUG
    g_ble.startAdvertising("vortex_test", true);
#else
    g_ble.startAdvertising("vortex_led", true);
#endif
}
void loop() {
    const uint32_t now = millis();
    g_controller.tick(now);
    g_ble.tick();
    g_console.poll();
    delay(1);
}