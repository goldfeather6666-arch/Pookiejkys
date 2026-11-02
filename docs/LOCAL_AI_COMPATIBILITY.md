# PIXEL LAB — LocalMind On-Device AI Compatibility Specification (`docs/LOCAL_AI_COMPATIBILITY.md`)

## 1. Purpose & Honesty Charter

**LocalMind (Module 04)** is a 100% offline, on-device AI and document intelligence laboratory designed for the **Google Pixel 9 Pro XL** (`Google Tensor G4`, `16 GB LPDDR5X RAM`).

In strict compliance with Operating Principle 7 ("Do not fabricate hardware readings, benchmark results, AI capabilities, or successful tests") and Module 04 requirements:
- LocalMind **never** connects to remote cloud LLM APIs (no OpenAI, Gemini Cloud, or Anthropic endpoints).
- LocalMind **never** pretends a simulated response is coming from a neural network when no model file is loaded.
- When no `.gguf` or `.tflite` model has been imported by the user via Android's Storage Access Framework (SAF), LocalMind clearly displays an **`Unloaded / No Model Installed`** status banner with step-by-step import instructions, while keeping its deterministic **Offline Document Intelligence Suite** (TF-IDF Extractive Summarizer, Local Passage Q&A, Text Rewriter/Formatter, and Hardware Memory/Throughput Benchmark) 100% functional.

## 2. Target Hardware Profile: Google Pixel 9 Pro XL (`komodo`)

| Hardware Parameter | Specification | LocalMind Runtime Implication |
| :--- | :--- | :--- |
| **SoC** | Google Tensor G4 (`1x Cortex-X4 @ 3.10 GHz + 3x Cortex-A720 @ 2.60 GHz + 4x Cortex-A520 @ 1.95 GHz`) | Supports ARMv9-A `dotprod` and `i8mm` matrix instructions for quantized GGUF (`Q4_0`, `Q4_K_M`, `Q8_0`) CPU/NPU execution. |
| **System RAM** | `16 GB` LPDDR5X | Android OS + system UI + background apps typically reserve `6.5 GB – 8.5 GB`, leaving `7.5 GB – 9.5 GB` (`ActivityManager.MemoryInfo.availMem`) available before low-memory killer (`lmkd`) pressure. |
| **Safe Native Heap Ceiling** | `<= 5.5 GB` recommended (`<= 7.0 GB` hard ceiling) | Prevents system UI eviction and thermal throttling during sustained token generation. |

## 3. Verified Model Formats & Quantization Compatibility Matrix

LocalMind's built-in `ModelCompatibilityAnalyzer` inspects the binary magic header and file size of any user-selected model file before loading:

| Format | Extension | Binary Magic Header | Quantization / Precision | Recommended Parameter Range (Pixel 9 Pro XL) | Memory Overhead Formula | Compatibility Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **GGUF (v2/v3)** | `.gguf` | `0x47 0x47 0x55 0x46` (`"GGUF"`) | `Q4_K_M`, `Q4_0`, `Q5_K_M` | `0.5B – 8B` (e.g., Gemma-2-2B-IT, Llama-3.2-3B-Instruct, Qwen2.5-7B-Instruct Q4_K_M) | `FileSize * 1.18 + KV_Cache (256 MB)` | **Optimal / Verified** |
| **GGUF (v2/v3)** | `.gguf` | `0x47 0x47 0x55 0x46` (`"GGUF"`) | `Q8_0` | `0.5B – 4B` | `FileSize * 1.15 + KV_Cache (256 MB)` | **Supported (Moderate RAM)** |
| **GGUF (v2/v3)** | `.gguf` | `0x47 0x47 0x55 0x46` (`"GGUF"`) | `F16` / `BF16` | `<= 3B` (`>7B F16` rejected by safety check) | `FileSize * 1.12 + KV_Cache (512 MB)` | **Guarded (Blocked if > AvailMem * 0.75)** |
| **LiteRT / TFLite** | `.tflite` | `0x54 0x46 0x4C 0x33` (`"TFL3"` at offset 4) | `INT8` / `FP16` | MobileBERT, Universal Sentence Encoder, MobileNetV4, MediaPipe Tasks | `FileSize * 1.25 + Arena (64 MB)` | **Optimal / Verified** |

## 4. Safety Guards & Pre-Load Verification Protocol

Before attempting to map any model into memory, `ModelCompatibilityAnalyzer.analyze(file, memoryInfo)` executes:
1. **Header Verification**: Reads the first 24 bytes via `RandomAccessFile` / `ContentResolver.openInputStream` to confirm valid `GGUF` (version 2 or 3, tensor count, metadata KV count) or `TFL3` magic bytes. Corrupted or unsupported files (e.g., raw `.safetensors` or PyTorch `.bin` pickles) are rejected with an explanatory diagnostic message.
2. **Dynamic RAM Headroom Check**: Queries live `ActivityManager.MemoryInfo` (`availMem`, `totalMem`, `lowMemory`).
   - If `estimatedRequiredBytes > availMem * 0.75`: Status is **`BLOCKED_INSUFFICIENT_RAM`** to prevent OS crash.
   - If `estimatedRequiredBytes > availMem * 0.55`: Status is **`WARNING_HIGH_MEMORY_PRESSURE`** with quantization downsizing guidance.
   - Otherwise: Status is **`COMPATIBLE_READY`**.
3. **Thermal Guard**: Checks `PowerManager.getCurrentThermalStatus()`. If thermal status is `THERMAL_STATUS_SEVERE` or higher, LocalMind warns the user and limits worker thread count to 2 efficiency cores.
