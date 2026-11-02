package com.pixellab.suite.data

import android.content.Context
import com.pixellab.suite.core.database.ActivityLogEntity
import com.pixellab.suite.core.database.AiModelEntity
import com.pixellab.suite.core.database.AutomationLogEntity
import com.pixellab.suite.core.database.AutomationRoutineEntity
import com.pixellab.suite.core.database.BatterySampleEntity
import com.pixellab.suite.core.database.NoteEntity
import com.pixellab.suite.core.database.PixelLabDatabase
import com.pixellab.suite.core.database.SensorSessionEntity
import com.pixellab.suite.core.database.SnippetEntity
import com.pixellab.suite.core.datastore.PixelLabPreferences
import com.pixellab.suite.core.device.DeviceHardwareProvider
import com.pixellab.suite.core.diagnostics.NetworkDiagnosticsProvider
import com.pixellab.suite.core.diagnostics.SensorDiagnosticsProvider
import com.pixellab.suite.core.permissions.PermissionCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PixelLabRepository(val appContext: Context) {
    val database = PixelLabDatabase.getInstance(appContext)
    val dao = database.dao()
    val preferences = PixelLabPreferences(appContext)
    val hardwareProvider = DeviceHardwareProvider(appContext)
    val sensorProvider = SensorDiagnosticsProvider(appContext)
    val networkProvider = NetworkDiagnosticsProvider(appContext)
    val permissionCoordinator = PermissionCoordinator(appContext)

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repoScope.launch {
            seedInitialOfflineDataIfNeeded()
        }
    }

    private suspend fun seedInitialOfflineDataIfNeeded() {
        val existingRoutines = dao.observeRoutines().first()
        if (existingRoutines.isEmpty()) {
            dao.upsertRoutine(
                AutomationRoutineEntity(
                    name = "Pixel Thermal Guard Alert",
                    description = "Logs a diagnostic snapshot and alerts when battery temperature exceeds 40°C while charging.",
                    triggerType = "CHARGING_STATE",
                    triggerParameter = "CHARGING",
                    conditionLogic = "AND",
                    conditionsJson = "BATTERY_TEMP_C|>=|40.0;IS_CHARGING|==|true",
                    actionType = "NOTIFICATION",
                    actionPayload = "Pixel 9 Pro XL battery reached 40°C during charging. Consider switching MotionX or LocalMind to Low-Power Mode.",
                    isEnabled = true
                )
            )
            dao.upsertRoutine(
                AutomationRoutineEntity(
                    name = "Low Battery Energy Saver Prompt",
                    description = "Triggers when battery drops below 20% while discharging.",
                    triggerType = "BATTERY_BELOW",
                    triggerParameter = "20",
                    conditionLogic = "AND",
                    conditionsJson = "BATTERY_LEVEL|<=|20;IS_CHARGING|==|false",
                    actionType = "OPEN_SETTINGS",
                    actionPayload = "android.settings.BATTERY_SAVER_SETTINGS",
                    isEnabled = true
                )
            )
        }

        val existingNotes = dao.observeNotes().first()
        if (existingNotes.isEmpty()) {
            dao.upsertNote(
                NoteEntity(
                    title = "Pixel 9 Pro XL Hardware Reference (komodo)",
                    content = "SoC: Google Tensor G4 (1x Cortex-X4 @ 3.10GHz, 3x Cortex-A720 @ 2.60GHz, 4x Cortex-A520 @ 1.95GHz). RAM: 16 GB LPDDR5X. Display: 6.8-inch Super Actua LTPO OLED (1344 x 2992 px, 1–120 Hz adaptive). Security: Titan M2.",
                    category = "Hardware",
                    isPinned = true
                )
            )
            dao.upsertNote(
                NoteEntity(
                    title = "LocalMind Quantization Guidelines",
                    content = "For optimal offline inference on 16 GB RAM without triggering Android LMKD, import GGUF v3 models quantized with Q4_K_M or Q5_K_M up to 7B–8B parameters (e.g., Gemma-2-2B-IT or Qwen2.5-7B-Instruct).",
                    category = "AI Lab",
                    isPinned = false
                )
            )
        }

        val existingSnippets = dao.observeSnippets().first()
        if (existingSnippets.isEmpty()) {
            dao.upsertSnippet(
                SnippetEntity(
                    label = "ADB Thermal Status Dump",
                    textContent = "adb shell dumpsys thermalservice",
                    tag = "Diagnostics"
                )
            )
            dao.upsertSnippet(
                SnippetEntity(
                    label = "ADB Battery Historian Reset",
                    textContent = "adb shell dumpsys batterystats --reset",
                    tag = "Battery"
                )
            )
        }

        // Record initial real battery sample on startup
        val b = hardwareProvider.getBatteryThermalSnapshot()
        dao.insertBatterySample(
            BatterySampleEntity(
                timestamp = System.currentTimeMillis(),
                levelPercent = b.levelPercent,
                isCharging = b.isCharging,
                plugType = b.plugSourceLabel,
                temperatureCelsius = b.temperatureCelsius,
                voltageMillivolts = b.voltageMillivolts,
                currentMicroamps = b.currentNowMicroamps,
                healthLabel = b.healthLabel
            )
        )
    }

    suspend fun recordActivity(moduleId: String, title: String, detail: String) {
        dao.insertActivityLog(
            ActivityLogEntity(
                moduleId = moduleId,
                actionTitle = title,
                detail = detail,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
