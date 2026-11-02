# Pookiejkys

## PIXEL LAB — The Ultimate All-in-One Offline Pixel Device Intelligence & Productivity Suite

**Target Hardware**: Google Pixel 9 Pro XL (`komodo` • Google Tensor G4 • 16 GB LPDDR5X RAM • 6.8" Super Actua LTPO OLED 1–120 Hz)  
**Platform**: Native Android (Kotlin 2.0.21 • Jetpack Compose • Material 3 • Room • DataStore • WorkManager • API 29–35)  
**Connectivity Philosophy**: 100% Offline-First • Zero Cloud Dependencies • Zero Telemetry

### Architecture & Phase Zero Documentation

- [`docs/REPOSITORY_AUDIT.md`](docs/REPOSITORY_AUDIT.md) — Phase Zero Repository Audit & Risk Assessment
- [`docs/IMPLEMENTATION_ROADMAP.md`](docs/IMPLEMENTATION_ROADMAP.md) — Phased Engineering Roadmap & Module Dependency Graph
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — Modular Clean Architecture, Unidirectional MVVM & Design Tokens
- [`docs/LOCAL_AI_COMPATIBILITY.md`](docs/LOCAL_AI_COMPATIBILITY.md) — LocalMind GGUF/LiteRT Verification & Pixel 9 Pro XL RAM Budget Matrix

### The Eight Core Modules + Command Center

| Module | Package / Directory | Core Capabilities |
| :--- | :--- | :--- |
| **Command Center** | `feature/dashboard/` | Reorderable/hideable live telemetry cards, global module & tool search, favorites bar, and local activity log. |
| **01 — PIXEL TWIN** | `feature/device/` | Real hardware/OS/display/CPU/RAM/storage/thermal discovery, interactive 3D Pixel 9 Pro XL Canvas model with rotation/zoom/flip and 6 subsystem hotspots, plus Markdown report export. |
| **02 — AUTOPILOT** | `feature/automation/` | Offline routine builder with `AND`/`OR` multi-clause condition evaluation, `BroadcastReceiver` system triggers, `NotificationManager` alerts, and Android 15 exact alarm / battery optimization diagnostics. |
| **03 — SENSORIUM** | `feature/sensors/` | Dynamic `SensorManager.getSensorList(Sensor.TYPE_ALL)` discovery, sampling rate selector, live 3-axis Compose waveform oscilloscope, session recorder, and RFC-4180 CSV export. |
| **04 — LOCALMIND** | `feature/localai/` | SAF `.gguf` / `.tflite` binary header inspector & RAM headroom analyzer for 16 GB LPDDR5X, honest unloaded-model guidance, offline TF-IDF document summarizer, passage Q&A, and real multi-core matrix throughput benchmark. |
| **05 — SIGNALX** | `feature/network/` | Local `NetworkInterface` & `ConnectivityManager`/`LinkProperties` inspector, Strict Offline Profile lock, optional explicit DNS + TCP/443 reachability latency probe, and CSV export. |
| **06 — ENERGIX** | `feature/battery/` | Sticky `ACTION_BATTERY_CHANGED` & `BatteryManager` electrical telemetry (mV, °C, µA, µAh, health, thermal state), Room historical trend chart, and custom threshold alerts. |
| **07 — MOTIONX** | `feature/studio/` | Native 3D procedural geometry & particle studio (`Tensor 4D Hypercube`, `Toroidal Knot`, `Geodesic Sphere`, `Quantum Particles`), touch & gyroscope tilt reactivity, opt-in foreground-only microphone reactivity, live FPS counter, and 1080×1080 PNG export. |
| **08 — EDGEFLOW** | `feature/productivity/` | Recursive-descent Scientific Calculator, searchable Room Notes, Clipboard Snippets, Stopwatch with Laps, 5-category Unit Converter, pure-Kotlin ISO/IEC 18004 QR Code Generator, and SHA-256/MD5/Base64 utilities. |
| **Privacy & Settings** | `feature/privacy/`, `feature/settings/` | Runtime permission audit, local SQLite/export storage breakdown, JSON backup, atomic history wipe, Obsidian Dark/Light/High-Contrast themes, and Reduced Motion mode. |

### Building, Testing & Pushing to GitHub

```bash
# Run Unit Tests
./gradlew testDebugUnitTest

# Run Android Lint
./gradlew lintDebug

# Assemble Debug & Release APKs
./gradlew assembleDebug assembleRelease
```

GitHub Actions workflows in `.github/workflows/android-ci.yml` automatically build, lint, test, and upload the APK artifacts on every push to `main`. Production GitHub Releases in `.github/workflows/release.yml` require explicit manual authorization (`authorize_production == 'AUTHORIZE'`) in compliance with Operating Principle 17.
