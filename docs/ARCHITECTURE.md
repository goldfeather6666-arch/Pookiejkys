# PIXEL LAB — System Architecture (`docs/ARCHITECTURE.md`)

## 1. Architectural Pattern: Modular Clean Architecture + Unidirectional MVVM

PIXEL LAB enforces strict separation of concerns across three primary layers:

1. **Presentation Layer (`feature/*`, `core/designsystem`, `core/ui`)**:
   - Built 100% with **Jetpack Compose** and **Material 3**.
   - Every screen observes a single immutable `UiState` data class exposed as a `StateFlow<T>` from a `@HiltViewModel` (or factory-injected `ViewModel`) using `collectAsStateWithLifecycle()`.
   - User interactions are dispatched via sealed `UiEvent` / intent methods on the `ViewModel`.
   - Zero Android hardware managers or database DAOs are referenced directly inside `@Composable` functions.

2. **Domain Layer (`domain/`)**:
   - Pure Kotlin models, repository contracts, and deterministic business logic engines:
     - `AutomationRuleEvaluator`: Evaluates composite `AND`/`OR` trigger and threshold rules.
     - `GgufHeaderParser` & `ModelCompatibilityAnalyzer`: Parses `.gguf` / `.tflite` binary headers and computes RAM safety margins for the Pixel 9 Pro XL (`16 GB` LPDDR5X).
     - `ScientificExpressionEvaluator`: Recursive-descent mathematical parser supporting trigonometric, logarithmic, factorial, power, and constant expressions.
     - `ExtractiveSummarizer` & `LocalDocumentQaEngine`: Pure Kotlin TF-IDF sentence scoring and passage retrieval for zero-cloud document intelligence.
     - `QrMatrixEncoder`: Pure Kotlin ISO/IEC 18004 Byte-mode QR matrix generator with Reed-Solomon error correction.

3. **Data & Hardware Abstraction Layer (`data/`, `core/device`, `core/diagnostics`, `core/database`, `core/datastore`)**:
   - **Room Database (`PixelLabDatabase`)**: Stores `AutomationRoutineEntity`, `AutomationLogEntity`, `SensorSessionEntity`, `BatterySampleEntity`, `NoteEntity`, `SnippetEntity`, `AiModelEntity`, and `ActivityLogEntity`.
   - **Preferences DataStore (`PixelLabPreferences`)**: Stores dashboard card order, hidden card IDs, favorite modules, theme mode (`OBSIDIAN_DARK`, `LIGHT`, `SYSTEM`), high-contrast toggle, reduced-motion toggle, thermal-aware mode, and strict offline network mode.
   - **Hardware Providers**: Lifecycle-safe `callbackFlow` wrappers around `SensorManager`, `BatteryManager` (`BroadcastReceiver` for `ACTION_BATTERY_CHANGED`), `PowerManager.OnThermalStatusChangedListener`, `ConnectivityManager.NetworkCallback`, and `DisplayManager.DisplayListener`.

## 2. Visual Design System (`core/designsystem`)

### Color Palette Tokens (Obsidian + Electric Violet + Cyan + Deep Navy + Glass)
- `ObsidianBackground`: `#07090F` (Primary background surface)
- `DeepNavySurface`: `#0E1322` (Elevated layer surface)
- `GlassSurface`: `#1A2238` at `72%` alpha with `1.dp` border (`#2E3B5E`)
- `ElectricViolet`: `#7C4DFF` (Primary accent & interactive highlight)
- `CyanPulse`: `#00E5FF` (Secondary telemetry & sensor accent)
- `EmeraldStatus`: `#00E676` (Healthy / Nominal / Active state)
- `AmberWarning`: `#FFAB00` (Thermal / Memory / Permission caution)
- `CrimsonAlert`: `#FF5252` (Critical threshold / Error state)

### Accessibility & Performance Guarantees
- **High-Contrast Mode**: Replaces translucent glass surfaces with opaque `#000000` / `#121212` backgrounds and `2.dp` high-luminance borders (`#FFFFFF` / `#00E5FF`).
- **Reduced Motion Mode**: Collapses spring animations to instant transitions and pauses ambient particle effects in MotionX and Dashboard headers.
- **Thermal-Aware Throttling**: Observes `PowerManager.getCurrentThermalStatus()`; when status reaches `THERMAL_STATUS_MODERATE` or higher, automatically lowers 3D Canvas target frame rate and sensor graph refresh frequency.

## 3. Offline, Privacy & Security Architecture

- **Zero Mandatory Network**: The app operates 100% offline. No cloud endpoints, analytics trackers, or remote LLM calls exist anywhere in the codebase.
- **Just-in-Time Permission Architecture**:
  - `POST_NOTIFICATIONS` (API 33+): Requested only when enabling notification actions in Autopilot or battery alerts in Energix.
  - `CAMERA`: Requested only when opening the live QR Scanner in EdgeFlow.
  - `RECORD_AUDIO`: Requested only when the user explicitly toggles on Microphone-Reactive Visuals in MotionX, and released immediately in `onDispose` / `ON_STOP`.
  - `BODY_SENSORS` / `ACTIVITY_RECOGNITION`: Requested only if the user selects Step Counter / Step Detector in Sensorium.
