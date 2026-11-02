package com.pixellab.suite.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PixelLabEnginesTest {

    @Test
    fun scientificCalculator_evaluatesArithmeticAndFunctionsCorrectly() {
        val r1 = ScientificExpressionEvaluator.evaluate("2 + 3 * 4").getOrThrow()
        assertEquals(14.0, r1, 1e-6)

        val r2 = ScientificExpressionEvaluator.evaluate("sqrt(144) + 2^5").getOrThrow()
        assertEquals(44.0, r2, 1e-6)

        val r3 = ScientificExpressionEvaluator.evaluate("5! / (2 * 3)").getOrThrow()
        assertEquals(20.0, r3, 1e-6)

        val r4 = ScientificExpressionEvaluator.evaluate("sin(30) + cos(60)").getOrThrow()
        assertEquals(1.0, r4, 1e-5)

        val divZero = ScientificExpressionEvaluator.evaluate("10 / 0")
        assertTrue(divZero.isFailure)
    }

    @Test
    fun unitConverter_convertsAcrossAllSupportedCategories() {
        assertEquals(1.0, UnitConversionEngine.convert(UnitCategory.LENGTH, 100.0, "cm", "m"), 1e-6)
        assertEquals(212.0, UnitConversionEngine.convert(UnitCategory.TEMPERATURE, 100.0, "C", "F"), 1e-5)
        assertEquals(0.0, UnitConversionEngine.convert(UnitCategory.TEMPERATURE, 273.15, "K", "C"), 1e-5)
        assertEquals(1024.0, UnitConversionEngine.convert(UnitCategory.DATA_STORAGE, 1.0, "GB", "MB"), 1e-5)
        assertEquals(1000.0, UnitConversionEngine.convert(UnitCategory.MASS, 1.0, "kg", "g"), 1e-5)
    }

    @Test
    fun automationEvaluator_evaluatesAndOrLogicAccurately() {
        val ctx = DeviceConditionContext(
            batteryLevel = 18,
            isCharging = false,
            hourOfDay = 22,
            batteryTempC = 36.5f,
            thermalStatusCode = 0
        )
        val clauses = AutomationConditionEvaluator.parseClauses("BATTERY_LEVEL|<=|20;IS_CHARGING|==|false")
        assertEquals(2, clauses.size)
        assertTrue(AutomationConditionEvaluator.evaluateAll("AND", clauses, ctx))

        val failingAnd = AutomationConditionEvaluator.parseClauses("BATTERY_LEVEL|<=|20;IS_CHARGING|==|true")
        assertFalse(AutomationConditionEvaluator.evaluateAll("AND", failingAnd, ctx))
        assertTrue(AutomationConditionEvaluator.evaluateAll("OR", failingAnd, ctx))
    }

    @Test
    fun modelCompatibilityAnalyzer_verifiesGgufHeaderAndPixel9ProXlRamBudget() {
        val ggufHeader = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(0x47.toByte()) // G
            put(0x47.toByte()) // G
            put(0x55.toByte()) // U
            put(0x46.toByte()) // F
            putInt(3)          // version 3
            putLong(291L)      // tensor count
            putLong(24L)       // kv count
        }.array()

        val totalRam16Gb = 16L * 1024L * 1024L * 1024L
        val availRam8Gb = 8L * 1024L * 1024L * 1024L
        val modelSize2Gb = 2L * 1024L * 1024L * 1024L

        val reportOk = ModelCompatibilityAnalyzer.analyzeCompatibility(
            fileName = "gemma-2-2b-it-Q4_K_M.gguf",
            fileSizeBytes = modelSize2Gb,
            headerBytes = ggufHeader,
            availableRamBytes = availRam8Gb,
            totalRamBytes = totalRam16Gb
        )
        assertEquals(CompatibilityVerdict.COMPATIBLE_READY, reportOk.verdict)
        assertEquals("GGUF", reportOk.headerInspection.format)

        val oversized12Gb = 12L * 1024L * 1024L * 1024L
        val reportBlocked = ModelCompatibilityAnalyzer.analyzeCompatibility(
            fileName = "oversized-f16.gguf",
            fileSizeBytes = oversized12Gb,
            headerBytes = ggufHeader,
            availableRamBytes = availRam8Gb,
            totalRamBytes = totalRam16Gb
        )
        assertEquals(CompatibilityVerdict.BLOCKED_INSUFFICIENT_RAM, reportBlocked.verdict)
    }

    @Test
    fun extractiveTextEngine_summarizesAndAnswersQuestionsLocally() {
        val doc = "Google Pixel 9 Pro XL uses the Tensor G4 processor and 16 GB LPDDR5X RAM. " +
            "The display is a 6.8-inch Super Actua LTPO panel supporting 1 to 120 Hz. " +
            "PIXEL LAB operates completely offline for maximum privacy."
        val summary = ExtractiveTextEngine.summarizeDocument(doc, maxSentences = 2)
        assertTrue(summary.isNotBlank())

        val answer = ExtractiveTextEngine.answerQuestionFromDocument(doc, "What processor and RAM does Pixel 9 Pro XL use?")
        assertTrue(answer.contains("Tensor G4"))
    }

    @Test
    fun qrAndCryptoEngines_produceDeterministicOutput() {
        val matrix = QrCodeGeneratorEngine.encodeToMatrix("PIXEL LAB")
        assertEquals(25, matrix.size)
        assertEquals(25, matrix[0].size)
        // Top-left finder pattern corner must be dark (true)
        assertTrue(matrix[0][0])
        assertTrue(matrix[0][6])
        assertTrue(matrix[6][0])

        val b64 = TextCryptoEngine.base64Encode("Pixel 9 Pro XL")
        assertEquals("Pixel 9 Pro XL", TextCryptoEngine.base64Decode(b64).getOrThrow())
        assertEquals(64, TextCryptoEngine.sha256Hex("PIXEL LAB").length)
    }
}
