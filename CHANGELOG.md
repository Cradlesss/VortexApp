# VortexApp – Changelog

All notable changes are documented here.

---

## [2.0.1] – 2026-05-18

### VortexApp (Android) — Hotfix

**Bug fixes**
- Fixed a crash on Gradle sync when `keystore.properties` is missing — null values were being cast to `String` unconditionally. The signing config is now only applied when the file exists; release builds fall back to debug signing so the project works out of the box for anyone cloning the repo.
- Fixed the home screen not updating after disconnecting via the notification while the app is in the background — the BLE service was emitting `MSG_DISCONNECT` to an empty client set because the repository had already unregistered its messenger on app background. The service now syncs its current connection state to any newly registered client, so the UI is always consistent when the app returns to the foreground.

**Changes**
- Terms of Service expanded with explicit no-warranty, no-liability, and open-source/license clauses including a tappable link to the repository.

---

## [2.0.0] – 2025-05-17

Complete rewrite of both the Android app and the firmware. The two components have been restructured, renamed, and rebuilt from the ground up.

### VortexApp (Android)

**Architecture**
- Replaced ad-hoc service/activity coupling with a layered package structure: `ble/`, `core/`, `domain/`, `ui/`.
- Introduced `BleSessionRepository` as the single source of truth for BLE connection state, backed by `LiveData`.
- Added manual dependency injection via `AppGraph` — no external DI framework needed.
- Migrated build scripts from Gradle Groovy DSL to Kotlin DSL (`build.gradle.kts`).
- Raised `minSdk` from 24 to 29; `targetSdk` bumped to 35; Java source compatibility set to 17.

**BLE layer**
- Rewrote `BackgroundBluetoothLeService` with a clean Messenger-based IPC protocol.
- Introduced a compact binary frame format (`LedControlFrames`) replacing raw ad-hoc byte sends.
- Added typed state models: `BleSessionState`, `LedState`, `PingResult`.
- Device preferences (name, saved state) now managed through a dedicated `BlePreferences` wrapper.

**UI**
- New bottom navigation replacing per-activity back-button chains (`BottomNavCoordinator`).
- Interactive coach/tutorial overlay system that guides first-time users through each screen (`Coach`, `CoachScript`, per-screen script classes).
- Custom dialog components: `VortexDialogInfo`, `VortexDialogSingleChoice`, `VortexDialogText`.
- `SolidColorController` extracted from `SolidColorActivity` for clearer separation of concerns.
- `BleSandboxActivity` added for low-level BLE command testing during development.
- Theme support added via `ThemeUtils`.

**Effects**
- Replaced hardcoded animation buttons with a registry-based system (`LedEffectRegistry`, `Effect` model).
- Full effect list: Beat Blue, Beat Light Blue, Blur Phase Beat, Moving Rainbow, Nebula Surge, Raw Noise, Run Rainbow (Temp), Saw Tooth, Static Color, Transition With Break, Twinkle, Wave.

**Colors and suggestions**
- Added `ColorNamer` for human-readable color name display.
- Added `SelectionHistoryStore` and `SuggestionEngine` to surface recently used colors and effects.
- Saved solid colors now persisted via `SavedSolidColorStore`.

**Utilities**
- Added `Debouncer` to rate-limit rapid BLE sends.
- Added `CoachPrefs` to track tutorial completion state per screen.

---

### VortexCore (Firmware)

- Migrated from a single Arduino `.ino` sketch to a structured PlatformIO C++ project.
- Target board unchanged: Seeed XIAO nRF52840 Sense.
- LED library changed from FastLED to Adafruit NeoPixel.
- Introduced `EffectEngine` for clean effect switching and tick-based lifecycle management.
- Introduced `LedStrip` as an abstraction layer over the physical hardware strip.
- Added `StateStorage` for persisting the last active effect across power cycles.
- Replaced plain-text command strings with a compact binary BLE protocol (`proto.h`, `ble_led_service`).
- Added `ColorUtil` and `MathBeat` utility headers shared across effects.
- Added host-side native unit test environment (`pio test -e native`) — no hardware required.
- New effects in this release: Beat Blue, Beat Light Blue, Blur Phase Beat, Moving Rainbow, Raw Noise, Run Rainbow (Temp), Saw Tooth, Twinkle, Wave.
- Existing effects carried forward and rewritten: Nebula Surge, Rainbow, Static Color, Transition With Break.

### License

Changed from GNU GPL v3 to MIT.

---

## [1.0.0] – 2025-02-22

Initial public release.

Source code is preserved in the [Project-Void](https://github.com/Cradlesss/Project-Void) repository.

### Known issues in v1

- Several animations have timing bugs and may behave incorrectly.
- BLE reconnect is not fully reliable across all Android versions.
- App was not thoroughly tested below API 26 despite `minSdk = 24`.
- No persistent state — active effect and color are lost on disconnect.
