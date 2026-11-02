# PIXEL LAB — Implementation Roadmap (`docs/IMPLEMENTATION_ROADMAP.md`)

## 1. Executive Summary

This roadmap governs the end-to-end engineering of **PIXEL LAB** (`com.pixellab.suite`) targeting the **Google Pixel 9 Pro XL** (`komodo`, Google Tensor G4, 16 GB LPDDR5X RAM, 6.8" Super Actua LTPO OLED 1–120 Hz, Android 15 / API 35).

## 2. Phased Engineering Milestones

| Phase | Milestone | Key Deliverables | Status |
| :--- | :--- | :--- | :--- |
| **Phase 0** | Repository Audit & Architecture Specification | `docs/REPOSITORY_AUDIT.md`, `docs/IMPLEMENTATION_ROADMAP.md`, `docs/ARCHITECTURE.md`, `docs/LOCAL_AI_COMPATIBILITY.md` | Completed |
| **Phase 1** | Foundation, Build System & Design System | `settings.gradle.kts`, `gradle/libs.versions.toml`, `.github/workflows/android-ci.yml`, `core/common`, `core/designsystem` (Obsidian + Electric Violet + Cyan + Glass), `core/database` (Room), `core/datastore`, `core/permissions` | Completed |
| **Phase 2A** | Hardware, Sensor & Energy Telemetry | `core/device`, `core/diagnostics`, **Unified Dashboard** (`feature/dashboard`), **Module 01: Pixel Twin** (`feature/device`), **Module 03: Sensorium** (`feature/sensors`), **Module 06: Energix** (`feature/battery`) | Completed |
| **Phase 2B** | Automation, Network & Local AI | **Module 02: Autopilot** (`feature/automation`), **Module 04: LocalMind** (`feature/localai`), **Module 05: SignalX** (`feature/network`) | Completed |
| **Phase 2C** | Creative Studio, Productivity & Privacy | **Module 07: MotionX** (`feature/studio`), **Module 08: EdgeFlow** (`feature/productivity`), **Privacy Center** (`feature/privacy`), **Settings** (`feature/settings`) | Completed |
| **Phase 3** | QA Verification, Unit/UI Testing & Packaging | JUnit 4 test suite (`src/test/kotlin`), Compose UI instrumentation tests (`src/androidTest/kotlin`), static integrity verifier, repository bundle | Completed |

## 3. Recommended Implementation Order & Dependency Graph

```
[core/common] ──► [core/designsystem]
      │                   │
      ▼                   ▼
[core/database]   [core/datastore]   [core/permissions]
      │                   │                   │
      └─────────┬─────────┴─────────┬─────────┘
                ▼                   ▼
          [core/device]     [core/diagnostics]
                │                   │
                └─────────┬─────────┘
                          ▼
               [domain] ◄───► [data]
                          │
      ┌─────────┬─────────┼─────────┬─────────┐
      ▼         ▼         ▼         ▼         ▼
 [dashboard] [device] [sensors] [battery] [network]
      │         │         │         │         │
      ▼         ▼         ▼         ▼         ▼
[automation] [localai] [studio] [productivity] [privacy/settings]
                          │
                          ▼
                        [app]
```

## 4. Module-Level Acceptance Criteria

1. **Unified Dashboard**: Displays real-time device identity, battery, RAM, storage, thermal state, display refresh rate, active sensor count, local AI status, automation routine count, reorderable/toggleable cards, search bar, favorites, and activity log.
2. **Module 01 — Pixel Twin**: Queries real Android `Build`, `Display`, `ActivityManager`, `StatFs`, `BatteryManager`, `PowerManager`, and `PackageManager` APIs; renders interactive 3D Pixel 9 Pro XL model with rotation, zoom, front/rear flip, and 6 component hotspots; exports JSON/Markdown hardware reports.
3. **Module 02 — Autopilot**: Persists routines in Room; supports Time, Battery Level, Charging State, Boot, Sensor, and Manual triggers; evaluates composite `AND`/`OR` conditions; executes Notification, Reminder, Timer, App Launch, Settings Intent, and Internal Log actions via `WorkManager` / `AlarmManager`.
4. **Module 03 — Sensorium**: Enumerates all hardware/software sensors from `SensorManager`; renders live 3-axis waveforms (`X`, `Y`, `Z`) and magnitude; supports `UI`, `NORMAL`, `GAME`, and `FASTEST` sampling rates; records timestamped samples and exports RFC-4180 CSV files.
5. **Module 04 — LocalMind**: Imports `.gguf` and `.tflite` models via Storage Access Framework; validates GGUF binary headers and estimates memory footprint against available RAM on Pixel 9 Pro XL; provides honest model-not-loaded guidance while offering full offline document summarization (TF-IDF extractive), document Q&A, text rewriting utilities, and hardware throughput benchmarking.
6. **Module 05 — SignalX**: Inspects `NetworkInterface` and `ConnectivityManager` / `LinkProperties` offline; supports Strict Offline Profile; executes local DNS resolution and socket/ICMP latency checks only when permitted; exports CSV network reports.
7. **Module 06 — Energix**: Monitors battery percentage, charging status, plug source, voltage (mV), temperature (°C), current (µA), and health; logs historical battery events in Room; supports configurable high/low/temperature threshold alerts.
8. **Module 07 — MotionX**: Renders interactive 3D procedural meshes, particle systems, and fluid wave shaders on Compose Canvas; supports touch drag and live gyroscope tilt reactivity; displays real-time FPS/frame-time telemetry and exports high-resolution PNG frames.
9. **Module 08 — EdgeFlow**: Provides standard & scientific calculator, Room-backed searchable notes, clipboard snippet vault, timer & lap stopwatch, multi-domain unit converter, pure-Kotlin QR code generator & scanner analyzer, and offline text/crypto utilities.
10. **Privacy Center & Settings**: Audits granted/denied permissions, calculates per-category local storage usage, manages imported AI models, exports/imports JSON backups, and provides atomic data wipe controls.
