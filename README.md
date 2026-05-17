# Project Void – v1

> **This is the original v1 release, preserved for reference. It is no longer actively developed.**
> v2 is a complete rewrite and supersedes this version.

---

Project Void is an open-source BLE ecosystem for controlling addressable LED strips from an Android device.

- **Vortex_App** – Android application (API 24+) that connects to and controls BLE devices.
- **Vortex_Controller** – Arduino firmware for Seeed XIAO nRF52840 Sense.

## Known issues

- Several animations have timing bugs and may behave incorrectly.
- BLE reconnect is not fully reliable across all Android versions.
- App was not thoroughly tested below API 26 despite `minSdk = 24`.
- No persistent state — active effect and color are lost on disconnect.

## Installation

### Vortex_App (Android)

1. Clone the repository.
2. Open `Vortex_app/` in Android Studio.
3. Build and install:

```
./gradlew installDebug
```

### Vortex_Controller (Firmware)

1. Open `Vortex_Controller/Vortex_controller/Vortex_controller.ino` in the Arduino IDE.
2. Install dependencies:
   - Adafruit Bluefruit nRF52 Library
   - FastLED
3. Select board: **Seeed XIAO nRF52840 Sense**
4. Compile and upload.

## License

[MIT](LICENSE)
