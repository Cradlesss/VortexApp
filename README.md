# VortexApp

VortexApp is an open-source BLE ecosystem for controlling addressable LED strips from an Android device. It is made up of two components:

- **VortexApp** – Android application (API 29+) for discovering, connecting, and controlling compatible devices over Bluetooth Low Energy.
- **VortexCore** – Firmware for nRF52840-based microcontrollers that receives commands and drives the LED strip.

---

## How it works

1. Flash VortexCore onto a supported nRF52840 board.
2. Install VortexApp on your Android device.
3. Open the app, scan for nearby devices, and connect.
4. Pick a static color or choose an effect from the built-in library.
5. The app encodes your selection into a compact binary frame and sends it over BLE; VortexCore decodes it and updates the strip in real time.

---

## Supported hardware

| Component | Supported |
|-----------|-----------|
| Android | API 29 and above (Android 10+) |
| Microcontroller | Seeed XIAO nRF52840 / nRF52840 Sense |
| LED strips | Any NeoPixel-compatible strip (WS2812B, SK6812, etc.) |

---

## Installation

### VortexApp (Android)

The latest release APK is available on the [Releases](../../releases) page — no build step required.

To build from source:

1. Clone the repository.
2. Open the `VortexApp/` directory in Android Studio.
3. Connect an Android device (or use an emulator with BLE support).
4. Build and install:

```
./gradlew installDebug
```

Or use the Run button in Android Studio.

---

### VortexCore (Firmware)

The firmware uses [PlatformIO](https://platformio.org/). You can install it as a VS Code extension or via the CLI.

1. Open the `VortexCore/` directory in VS Code (with the PlatformIO extension) or in the PlatformIO IDE.
2. Connect your nRF52840 board via USB.
3. Flash the release build:

```
pio run -e seeed-release --target upload
```

To enable serial debug output:

```
pio run -e seeed-debug --target upload
```

To run the native unit tests (no hardware required):

```
pio test -e native
```

---

## LED Effects

VortexApp ships with the following built-in effects:

| Effect | Description |
|--------|-------------|
| Static Color | Solid user-selected color |
| Rainbow | Classic full-spectrum cycle |
| Moving Rainbow | Rainbow that flows along the strip |
| Nebula Surge | Pulsing color surge effect |
| Beat Blue | Blue-toned beat pulse |
| Beat Light Blue | Lighter beat pulse variant |
| Blur Phase Beat | Soft blurred beat phasing |
| Raw Noise | Organic noise-based randomness |
| Run Rainbow (Temp) | Travelling rainbow segment |
| Saw Tooth | Sharp sawtooth brightness wave |
| Transition With Break | Smooth color transition with pause |
| Twinkle | Random twinkling points |
| Wave | Smooth sine wave brightness |

New effects can be added to the firmware by implementing `IEffect` and registering them in the app's `LedEffectRegistry`.

---

## Project structure

```
VortexApp/
├── VortexApp/          Android application (Gradle, Java)
│   └── app/src/
│       ├── ble/        BLE service, session repository, protocol
│       ├── core/       DI, preferences, permissions, utilities
│       ├── domain/     Effect registry and models
│       └── ui/         Activities, adapters, dialogs, coach system
└── VortexCore/         Firmware (PlatformIO, C++)
    └── src/
        ├── ble/        BLE GATT service and protocol
        └── led/        Effect engine, LED strip abstraction, effects
```

---

## Contributing

Pull requests are welcome. For significant changes, please open an issue first to discuss the approach.

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for the full version history.

---

## License

[MIT](LICENSE)
