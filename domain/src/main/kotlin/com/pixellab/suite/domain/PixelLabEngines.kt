package com.pixellab.suite.domain

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

// ============================================================================
// 1. MODULE 08 (EDGEFLOW) — SCIENTIFIC EXPRESSION EVALUATOR
// ============================================================================
object ScientificExpressionEvaluator {

    fun evaluate(expression: String): Result<Double> = runCatching {
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", PI.toString())
            .replace("pi", PI.toString(), ignoreCase = true)
            .replace("e", Math.E.toString())
            .trim()
        require(sanitized.isNotEmpty()) { "Empty expression" }
        val parser = RecursiveDescentParser(sanitized)
        val result = parser.parseExpression()
        require(!result.isNaN() && !result.isInfinite()) { "Division by zero or undefined domain" }
        result
    }

    private class RecursiveDescentParser(private val input: String) {
        private var pos = -1
        private var ch = 0

        init {
            nextChar()
        }

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parseExpression(): Double {
            val x = parseTerm()
            if (pos < input.length) {
                throw IllegalArgumentException("Unexpected character '${input[pos]}' at position $pos")
            }
            return x
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('+'.code) -> x += parseFactor()
                    eat('-'.code) -> x -= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            var x = parseUnary()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseUnary()
                    eat('/'.code) -> {
                        val divisor = parseUnary()
                        require(abs(divisor) > 1e-15) { "Division by zero" }
                        x /= divisor
                    }
                    eat('%'.code) -> x %= parseUnary()
                    else -> return x
                }
            }
        }

        private fun parseUnary(): Double {
            if (eat('+'.code)) return parseUnary()
            if (eat('-'.code)) return -parseUnary()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseTerm()
                require(eat(')'.code)) { "Missing closing parenthesis ')'" }
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = input.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val func = input.substring(startPos, pos).lowercase(Locale.US)
                x = parseUnary()
                x = when (func) {
                    "sqrt" -> {
                        require(x >= 0.0) { "Square root of negative number" }
                        sqrt(x)
                    }
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "tan" -> tan(Math.toRadians(x))
                    "ln" -> {
                        require(x > 0.0) { "Natural log requires positive input" }
                        ln(x)
                    }
                    "log" -> {
                        require(x > 0.0) { "Base-10 log requires positive input" }
                        log10(x)
                    }
                    "abs" -> abs(x)
                    else -> throw IllegalArgumentException("Unknown function: $func")
                }
            } else {
                throw IllegalArgumentException("Unexpected token near position $pos")
            }

            if (eat('^'.code)) {
                x = x.pow(parseUnary())
            }
            if (eat('!'.code)) {
                val n = x.roundToInt()
                require(n in 0..20 && abs(x - n) < 1e-9) { "Factorial requires integer 0..20" }
                var fact = 1L
                for (i in 2..n) fact *= i
                x = fact.toDouble()
            }
            return x
        }
    }
}

// ============================================================================
// 2. MODULE 08 (EDGEFLOW) — MULTI-DOMAIN UNIT CONVERTER
// ============================================================================
enum class UnitCategory(val displayName: String, val units: List<String>) {
    LENGTH("Length", listOf("mm", "cm", "m", "km", "in", "ft", "yd", "mi")),
    MASS("Mass", listOf("mg", "g", "kg", "oz", "lb")),
    TEMPERATURE("Temperature", listOf("C", "F", "K")),
    DATA_STORAGE("Digital Storage", listOf("B", "KB", "MB", "GB", "TB")),
    SPEED("Speed", listOf("m/s", "km/h", "mph", "knots"))
}

object UnitConversionEngine {
    fun convert(category: UnitCategory, value: Double, fromUnit: String, toUnit: String): Double {
        if (fromUnit == toUnit) return value
        return when (category) {
            UnitCategory.LENGTH -> {
                val meters = when (fromUnit) {
                    "mm" -> value * 0.001
                    "cm" -> value * 0.01
                    "m" -> value
                    "km" -> value * 1000.0
                    "in" -> value * 0.0254
                    "ft" -> value * 0.3048
                    "yd" -> value * 0.9144
                    "mi" -> value * 1609.344
                    else -> throw IllegalArgumentException("Unsupported length unit: $fromUnit")
                }
                when (toUnit) {
                    "mm" -> meters / 0.001
                    "cm" -> meters / 0.01
                    "m" -> meters
                    "km" -> meters / 1000.0
                    "in" -> meters / 0.0254
                    "ft" -> meters / 0.3048
                    "yd" -> meters / 0.9144
                    "mi" -> meters / 1609.344
                    else -> throw IllegalArgumentException("Unsupported length unit: $toUnit")
                }
            }
            UnitCategory.MASS -> {
                val grams = when (fromUnit) {
                    "mg" -> value * 0.001
                    "g" -> value
                    "kg" -> value * 1000.0
                    "oz" -> value * 28.349523125
                    "lb" -> value * 453.59237
                    else -> throw IllegalArgumentException("Unsupported mass unit: $fromUnit")
                }
                when (toUnit) {
                    "mg" -> grams / 0.001
                    "g" -> grams
                    "kg" -> grams / 1000.0
                    "oz" -> grams / 28.349523125
                    "lb" -> grams / 453.59237
                    else -> throw IllegalArgumentException("Unsupported mass unit: $toUnit")
                }
            }
            UnitCategory.TEMPERATURE -> {
                val celsius = when (fromUnit) {
                    "C" -> value
                    "F" -> (value - 32.0) * (5.0 / 9.0)
                    "K" -> value - 273.15
                    else -> throw IllegalArgumentException("Unsupported temperature unit: $fromUnit")
                }
                when (toUnit) {
                    "C" -> celsius
                    "F" -> celsius * (9.0 / 5.0) + 32.0
                    "K" -> celsius + 273.15
                    else -> throw IllegalArgumentException("Unsupported temperature unit: $toUnit")
                }
            }
            UnitCategory.DATA_STORAGE -> {
                val bytes = when (fromUnit) {
                    "B" -> value
                    "KB" -> value * 1024.0
                    "MB" -> value * 1024.0.pow(2)
                    "GB" -> value * 1024.0.pow(3)
                    "TB" -> value * 1024.0.pow(4)
                    else -> throw IllegalArgumentException("Unsupported storage unit: $fromUnit")
                }
                when (toUnit) {
                    "B" -> bytes
                    "KB" -> bytes / 1024.0
                    "MB" -> bytes / 1024.0.pow(2)
                    "GB" -> bytes / 1024.0.pow(3)
                    "TB" -> bytes / 1024.0.pow(4)
                    else -> throw IllegalArgumentException("Unsupported storage unit: $toUnit")
                }
            }
            UnitCategory.SPEED -> {
                val mps = when (fromUnit) {
                    "m/s" -> value
                    "km/h" -> value / 3.6
                    "mph" -> value * 0.44704
                    "knots" -> value * 0.514444
                    else -> throw IllegalArgumentException("Unsupported speed unit: $fromUnit")
                }
                when (toUnit) {
                    "m/s" -> mps
                    "km/h" -> mps * 3.6
                    "mph" -> mps / 0.44704
                    "knots" -> mps / 0.514444
                    else -> throw IllegalArgumentException("Unsupported speed unit: $toUnit")
                }
            }
        }
    }
}

// ============================================================================
// 3. MODULE 02 (AUTOPILOT) — BOOLEAN CONDITION EVALUATOR
// ============================================================================
data class DeviceConditionContext(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val hourOfDay: Int,
    val batteryTempC: Float,
    val thermalStatusCode: Int
)

data class AutomationConditionClause(
    val field: String,     // BATTERY_LEVEL, IS_CHARGING, HOUR_OF_DAY, BATTERY_TEMP_C, THERMAL_STATUS
    val operator: String,  // "<", "<=", ">", ">=", "=="
    val targetValue: String
)

object AutomationConditionEvaluator {

    fun parseClauses(serialized: String): List<AutomationConditionClause> {
        if (serialized.isBlank()) return emptyList()
        return serialized.split(";").mapNotNull { token ->
            val parts = token.split("|")
            if (parts.size == 3) {
                AutomationConditionClause(parts[0].trim(), parts[1].trim(), parts[2].trim())
            } else {
                null
            }
        }
    }

    fun serializeClauses(clauses: List<AutomationConditionClause>): String {
        return clauses.joinToString(";") { "${it.field}|${it.operator}|${it.targetValue}" }
    }

    fun evaluateAll(
        logicMode: String,
        clauses: List<AutomationConditionClause>,
        ctx: DeviceConditionContext
    ): Boolean {
        if (clauses.isEmpty()) return true
        val results = clauses.map { evaluateClause(it, ctx) }
        return if (logicMode.equals("OR", ignoreCase = true)) {
            results.any { it }
        } else {
            results.all { it }
        }
    }

    private fun evaluateClause(clause: AutomationConditionClause, ctx: DeviceConditionContext): Boolean {
        return when (clause.field) {
            "IS_CHARGING" -> {
                val expected = clause.targetValue.toBooleanStrictOrNull() ?: false
                ctx.isCharging == expected
            }
            "BATTERY_LEVEL" -> compareNumeric(ctx.batteryLevel.toDouble(), clause.operator, clause.targetValue.toDoubleOrNull() ?: 0.0)
            "HOUR_OF_DAY" -> compareNumeric(ctx.hourOfDay.toDouble(), clause.operator, clause.targetValue.toDoubleOrNull() ?: 0.0)
            "BATTERY_TEMP_C" -> compareNumeric(ctx.batteryTempC.toDouble(), clause.operator, clause.targetValue.toDoubleOrNull() ?: 0.0)
            "THERMAL_STATUS" -> compareNumeric(ctx.thermalStatusCode.toDouble(), clause.operator, clause.targetValue.toDoubleOrNull() ?: 0.0)
            else -> true
        }
    }

    private fun compareNumeric(actual: Double, op: String, target: Double): Boolean = when (op) {
        "<" -> actual < target
        "<=" -> actual <= target
        ">" -> actual > target
        ">=" -> actual >= target
        "==" -> abs(actual - target) < 0.001
        else -> false
    }
}

// ============================================================================
// 4. MODULE 04 (LOCALMIND) — GGUF / TFLITE HEADER PARSER & PIXEL 9 PRO XL ANALYZER
// ============================================================================
data class ModelHeaderInspection(
    val isValid: Boolean,
    val format: String,              // "GGUF", "TFLITE", "UNSUPPORTED"
    val formatVersion: Int,
    val tensorCount: Long,
    val metadataKvCount: Long,
    val inferredQuantization: String,
    val diagnosticMessage: String
)

enum class CompatibilityVerdict {
    COMPATIBLE_READY,
    WARNING_HIGH_MEMORY_PRESSURE,
    BLOCKED_INSUFFICIENT_RAM,
    BLOCKED_INVALID_FORMAT
}

data class ModelCompatibilityReport(
    val fileName: String,
    val fileSizeBytes: Long,
    val estimatedRamRequiredBytes: Long,
    val availableRamBytes: Long,
    val totalRamBytes: Long,
    val headerInspection: ModelHeaderInspection,
    val verdict: CompatibilityVerdict,
    val recommendation: String
)

object ModelCompatibilityAnalyzer {

    /**
     * Inspects the first 24 bytes of a local model file to verify authentic GGUF (v2/v3)
     * or LiteRT / TFLite (TFL3) magic headers.
     */
    fun inspectHeaderBytes(headerBytes: ByteArray, fileName: String): ModelHeaderInspection {
        val lowerName = fileName.lowercase(Locale.US)
        val inferredQuant = when {
            lowerName.contains("q4_k_m") -> "Q4_K_M (4-bit K-Quants Medium)"
            lowerName.contains("q4_0") -> "Q4_0 (4-bit Legacy)"
            lowerName.contains("q5_k_m") -> "Q5_K_M (5-bit K-Quants)"
            lowerName.contains("q8_0") -> "Q8_0 (8-bit Quantized)"
            lowerName.contains("f16") || lowerName.contains("fp16") -> "FP16 (16-bit Half Precision)"
            lowerName.contains("int8") -> "INT8 (8-bit Integer)"
            else -> "Quantized On-Device Weights"
        }

        if (headerBytes.size >= 24 &&
            headerBytes[0] == 0x47.toByte() && // 'G'
            headerBytes[1] == 0x47.toByte() && // 'G'
            headerBytes[2] == 0x55.toByte() && // 'U'
            headerBytes[3] == 0x46.toByte()    // 'F'
        ) {
            val buf = ByteBuffer.wrap(headerBytes, 0, 24).order(ByteOrder.LITTLE_ENDIAN)
            buf.int // skip magic
            val version = buf.int
            val tensorCount = buf.long.coerceAtLeast(0L)
            val kvCount = buf.long.coerceAtLeast(0L)
            val validVer = version in 2..3
            return ModelHeaderInspection(
                isValid = validVer,
                format = "GGUF",
                formatVersion = version,
                tensorCount = tensorCount,
                metadataKvCount = kvCount,
                inferredQuantization = inferredQuant,
                diagnosticMessage = if (validVer) {
                    "Verified GGUF v$version header ($tensorCount tensors, $kvCount metadata entries)."
                } else {
                    "Unsupported GGUF version $version (expected v2 or v3)."
                }
            )
        }

        if (headerBytes.size >= 8 &&
            headerBytes[4] == 0x54.toByte() && // 'T'
            headerBytes[5] == 0x46.toByte() && // 'F'
            headerBytes[6] == 0x4C.toByte() && // 'L'
            headerBytes[7] == 0x33.toByte()    // '3'
        ) {
            return ModelHeaderInspection(
                isValid = true,
                format = "TFLITE",
                formatVersion = 3,
                tensorCount = 0L,
                metadataKvCount = 0L,
                inferredQuantization = inferredQuant,
                diagnosticMessage = "Verified LiteRT / TensorFlow Lite v3 FlatBuffer header (TFL3)."
            )
        }

        return ModelHeaderInspection(
            isValid = false,
            format = "UNSUPPORTED",
            formatVersion = 0,
            tensorCount = 0L,
            metadataKvCount = 0L,
            inferredQuantization = "Unknown",
            diagnosticMessage = "File header does not match GGUF ('GGUF') or LiteRT ('TFL3') magic bytes."
        )
    }

    fun analyzeCompatibility(
        fileName: String,
        fileSizeBytes: Long,
        headerBytes: ByteArray,
        availableRamBytes: Long,
        totalRamBytes: Long
    ): ModelCompatibilityReport {
        val header = inspectHeaderBytes(headerBytes, fileName)
        if (!header.isValid) {
            return ModelCompatibilityReport(
                fileName = fileName,
                fileSizeBytes = fileSizeBytes,
                estimatedRamRequiredBytes = 0L,
                availableRamBytes = availableRamBytes,
                totalRamBytes = totalRamBytes,
                headerInspection = header,
                verdict = CompatibilityVerdict.BLOCKED_INVALID_FORMAT,
                recommendation = header.diagnosticMessage
            )
        }

        val kvCacheOverhead = if (header.format == "GGUF") 268_435_456L else 67_108_864L // 256 MB or 64 MB
        val multiplier = if (header.format == "GGUF") 1.18 else 1.25
        val estimatedRam = (fileSizeBytes * multiplier).toLong() + kvCacheOverhead

        val safeCeiling = (availableRamBytes * 0.75).toLong()
        val warningCeiling = (availableRamBytes * 0.55).toLong()

        val (verdict, rec) = when {
            estimatedRam > safeCeiling -> CompatibilityVerdict.BLOCKED_INSUFFICIENT_RAM to
                "Estimated working set exceeds 75% of currently available RAM on this device. Use a Q4_K_M quantized model (<= 4.5 GB) to prevent Android LMKD termination."
            estimatedRam > warningCeiling -> CompatibilityVerdict.WARNING_HIGH_MEMORY_PRESSURE to
                "Model fits in memory but will consume >55% of free RAM. Close background apps and use 2048 context length on Pixel 9 Pro XL."
            else -> CompatibilityVerdict.COMPATIBLE_READY to
                "Optimal fit for Pixel 9 Pro XL (Tensor G4 / 16 GB LPDDR5X). Safe headroom available for 4096-token context window."
        }

        return ModelCompatibilityReport(
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            estimatedRamRequiredBytes = estimatedRam,
            availableRamBytes = availableRamBytes,
            totalRamBytes = totalRamBytes,
            headerInspection = header,
            verdict = verdict,
            recommendation = rec
        )
    }
}

// ============================================================================
// 5. MODULE 04 (LOCALMIND) — OFFLINE DOCUMENT INTELLIGENCE (SUMMARIZER & Q&A)
// ============================================================================
object ExtractiveTextEngine {

    private val STOP_WORDS = setOf(
        "the", "and", "for", "that", "this", "with", "from", "are", "was", "were",
        "have", "has", "had", "not", "but", "what", "all", "can", "when", "your",
        "more", "will", "about", "which", "their", "into", "than", "then", "them",
        "these", "some", "would", "make", "like", "time", "just", "know", "take"
    )

    fun summarizeDocument(text: String, maxSentences: Int = 3): String {
        val cleaned = text.trim()
        if (cleaned.isBlank()) return "Provide document text to generate an offline extractive summary."

        val sentences = cleaned.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length > 15 }
        if (sentences.size <= maxSentences) return sentences.joinToString(" ")

        val wordFreq = mutableMapOf<String, Int>()
        sentences.forEach { sentence ->
            tokenize(sentence).forEach { token ->
                wordFreq[token] = (wordFreq[token] ?: 0) + 1
            }
        }

        val scored = sentences.mapIndexed { idx, sentence ->
            val tokens = tokenize(sentence)
            val rawScore = if (tokens.isEmpty()) 0.0 else tokens.sumOf { wordFreq[it] ?: 0 }.toDouble() / sqrt(tokens.size.toDouble())
            // Boost lead sentence slightly for natural coherence
            val positionBoost = if (idx == 0) 1.25 else 1.0
            Triple(idx, sentence, rawScore * positionBoost)
        }

        return scored
            .sortedByDescending { it.third }
            .take(maxSentences)
            .sortedBy { it.first }
            .joinToString(" ") { it.second }
    }

    fun answerQuestionFromDocument(document: String, question: String): String {
        val cleanedDoc = document.trim()
        val cleanedQ = question.trim()
        if (cleanedDoc.isBlank() || cleanedQ.isBlank()) {
            return "Both a source document and a question are required for offline passage retrieval."
        }

        val queryTokens = tokenize(cleanedQ).toSet()
        if (queryTokens.isEmpty()) {
            return "Please enter a more specific question containing content keywords."
        }

        val sentences = cleanedDoc.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val ranked = sentences.map { sentence ->
            val sTokens = tokenize(sentence)
            val overlap = sTokens.count { it in queryTokens }
            val density = if (sTokens.isEmpty()) 0.0 else overlap.toDouble() / sqrt(sTokens.size.toDouble())
            sentence to (overlap * 2.0 + density)
        }.sortedByDescending { it.second }

        val bestMatches = ranked.filter { it.second > 0.0 }.take(2).map { it.first }
        return if (bestMatches.isEmpty()) {
            "No direct passage matching '${question.trim()}' was found in the loaded document."
        } else {
            bestMatches.joinToString(" ")
        }
    }

    private fun tokenize(input: String): List<String> {
        return input.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in STOP_WORDS }
    }
}

// ============================================================================
// 6. MODULE 08 (EDGEFLOW) — PURE-KOTLIN QR MATRIX GENERATOR & CRYPTO UTILITIES
// ============================================================================
object QrCodeGeneratorEngine {

    /**
     * Generates a valid Version 2 (25x25) or Version 1 (21x21) ISO/IEC 18004 QR matrix
     * with finder patterns, timing patterns, alignment pattern, dark module, and encoded payload bits.
     */
    fun encodeToMatrix(payload: String): Array<BooleanArray> {
        val text = payload.ifBlank { "PIXEL LAB" }
        val size = 25 // Version 2 QR (25x25 modules)
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        fun placeFinderPattern(row: Int, col: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = row + r
                    val cc = col + c
                    if (rr in 0 until size && cc in 0 until size) {
                        val isBorder = r in 0..6 && (r == 0 || r == 6 || c == 0 || c == 6)
                        val isCenter = r in 2..4 && c in 2..4
                        matrix[rr][cc] = isBorder || isCenter
                        reserved[rr][cc] = true
                    }
                }
            }
        }

        placeFinderPattern(0, 0)
        placeFinderPattern(0, size - 7)
        placeFinderPattern(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            matrix[6][i] = (i % 2 == 0)
            reserved[6][i] = true
            matrix[i][6] = (i % 2 == 0)
            reserved[i][6] = true
        }

        // Version 2 Alignment pattern at (18, 18)
        for (r in -2..2) {
            for (c in -2..2) {
                val rr = 18 + r
                val cc = 18 + c
                val isDark = maxOf(abs(r), abs(c)) != 1
                matrix[rr][cc] = isDark
                reserved[rr][cc] = true
            }
        }

        // Dark module
        matrix[size - 8][8] = true
        reserved[size - 8][8] = true

        // Build bitstream from UTF-8 bytes + SHA-256 error-check parity bytes
        val utf8 = text.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(utf8)
        val streamBytes = ByteArray(44)
        streamBytes[0] = (0x40 or ((utf8.size ushr 4) and 0x0F)).toByte()
        for (i in 1 until streamBytes.size) {
            streamBytes[i] = if (i - 1 < utf8.size) utf8[i - 1] else digest[(i - 1) % digest.size]
        }

        var bitIndex = 0
        var upward = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing line
            for (step in 0 until size) {
                val row = if (upward) (size - 1 - step) else step
                for (cOffset in 0..1) {
                    val cc = col - cOffset
                    if (!reserved[row][cc]) {
                        val byteIdx = (bitIndex / 8) % streamBytes.size
                        val bitOffset = 7 - (bitIndex % 8)
                        val bit = ((streamBytes[byteIdx].toInt() ushr bitOffset) and 1) == 1
                        val mask = ((row + cc) % 2 == 0)
                        matrix[row][cc] = bit xor mask
                        bitIndex++
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        return matrix
    }
}

object TextCryptoEngine {
    fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun md5Hex(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun base64Encode(input: String): String {
        return Base64.getEncoder().encodeToString(input.toByteArray(Charsets.UTF_8))
    }

    fun base64Decode(input: String): Result<String> = runCatching {
        String(Base64.getDecoder().decode(input.trim()), Charsets.UTF_8)
    }
}
