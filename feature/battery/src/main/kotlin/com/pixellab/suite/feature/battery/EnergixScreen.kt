package com.pixellab.suite.feature.battery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixellab.suite.core.common.ExportManager
import com.pixellab.suite.core.common.FormatUtils
import com.pixellab.suite.core.database.BatterySampleEntity
import com.pixellab.suite.core.datastore.UserSettingsState
import com.pixellab.suite.core.designsystem.GlassCard
import com.pixellab.suite.core.designsystem.PixelLabColors
import com.pixellab.suite.core.designsystem.SectionHeader
import com.pixellab.suite.core.designsystem.TelemetryKeyValueRow
import com.pixellab.suite.data.PixelLabRepository
import kotlinx.coroutines.launch

@Composable
fun EnergixScreen(repository: PixelLabRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val liveBattery by repository.hardwareProvider.observeBatteryThermal().collectAsState(
        initial = remember { repository.hardwareProvider.getBatteryThermalSnapshot() }
    )
    val historySamples by repository.dao.observeBatterySamples().collectAsState(initial = emptyList())
    val settings by repository.preferences.settingsFlow.collectAsState(initial = UserSettingsState())

    var lowSlider by remember(settings.batteryLowThreshold) { mutableFloatStateOf(settings.batteryLowThreshold.toFloat()) }
    var highSlider by remember(settings.batteryHighThreshold) { mutableFloatStateOf(settings.batteryHighThreshold.toFloat()) }
    var tempSlider by remember(settings.batteryTempThresholdC) { mutableFloatStateOf(settings.batteryTempThresholdC.toFloat()) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "MODULE 06 — ENERGIX",
                subtitle = "Real-Time Battery Intelligence, Electrical Telemetry & History",
                badge = "${liveBattery.levelPercent}% ${liveBattery.statusLabel.uppercase()}"
            )
        }

        // Live Electrical & Thermal Telemetry
        item {
            GlassCard(accentColor = PixelLabColors.EmeraldNominal) {
                SectionHeader(
                    title = "Live Battery Pack & PMIC Telemetry",
                    subtitle = "Queried from sticky ACTION_BATTERY_CHANGED & BatteryManager",
                    badge = "${liveBattery.temperatureCelsius}°C"
                )
                LinearProgressIndicator(
                    progress = { liveBattery.levelPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    color = PixelLabColors.EmeraldNominal
                )
                TelemetryKeyValueRow("Charge Level / Status", "${liveBattery.levelPercent}% • ${liveBattery.statusLabel}")
                TelemetryKeyValueRow("Power Source", liveBattery.plugSourceLabel)
                TelemetryKeyValueRow("Terminal Voltage", "${liveBattery.voltageMillivolts} mV")
                TelemetryKeyValueRow("Pack Temperature", "${liveBattery.temperatureCelsius} °C")
                TelemetryKeyValueRow(
                    "Instantaneous Current",
                    liveBattery.currentNowMicroamps?.let { "${it / 1000} mA ($it µA)" } ?: "Unavailable via standard Android API"
                )
                TelemetryKeyValueRow(
                    "Average Current",
                    liveBattery.currentAvgMicroamps?.let { "${it / 1000} mA ($it µA)" } ?: "Unavailable via standard Android API"
                )
                TelemetryKeyValueRow(
                    "Coulomb Charge Counter",
                    liveBattery.chargeCounterMicroampHours?.let { "${it / 1000} mAh ($it µAh)" } ?: "Unavailable via standard Android API"
                )
                TelemetryKeyValueRow("Reported Health / Chemistry", "${liveBattery.healthLabel} (${liveBattery.technology})")
                TelemetryKeyValueRow(
                    "Cycle Count (Android 14+ Extra)",
                    liveBattery.cycleCountExposed?.toString() ?: "Unavailable (OS Restricted to System Permission)"
                )
                TelemetryKeyValueRow("System Thermal Status", liveBattery.thermalStatusLabel)

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            repository.dao.insertBatterySample(
                                BatterySampleEntity(
                                    timestamp = System.currentTimeMillis(),
                                    levelPercent = liveBattery.levelPercent,
                                    isCharging = liveBattery.isCharging,
                                    plugType = liveBattery.plugSourceLabel,
                                    temperatureCelsius = liveBattery.temperatureCelsius,
                                    voltageMillivolts = liveBattery.voltageMillivolts,
                                    currentMicroamps = liveBattery.currentNowMicroamps,
                                    healthLabel = liveBattery.healthLabel
                                )
                            )
                            repository.recordActivity("battery", "Logged Battery Sample", "${liveBattery.levelPercent}% @ ${liveBattery.temperatureCelsius}°C")
                        }
                    }) {
                        Text("Log Snapshot to History")
                    }

                    OutlinedButton(onClick = {
                        val csv = buildString {
                            appendLine("timestamp_ms,level_pct,is_charging,plug_type,temp_c,voltage_mv,current_ua,health")
                            historySamples.forEach { s ->
                                appendLine("${s.timestamp},${s.levelPercent},${s.isCharging},${FormatUtils.escapeCsvField(s.plugType)},${s.temperatureCelsius},${s.voltageMillivolts},${s.currentMicroamps ?: ""},${s.healthLabel}")
                            }
                        }
                        val file = ExportManager.writeExportFile(context, "energix_battery_${System.currentTimeMillis()}.csv", csv)
                        exportStatus = "Exported ${historySamples.size} battery records to ${file.name}"
                        ExportManager.shareExportedFile(context, file, "text/csv")
                    }) {
                        Text("Export CSV")
                    }
                }
                if (exportStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(exportStatus!!, style = MaterialTheme.typography.bodyMedium, color = PixelLabColors.EmeraldNominal)
                }
            }
        }

        // Historical Trend Chart
        item {
            GlassCard(accentColor = PixelLabColors.CyanPulse) {
                SectionHeader(
                    title = "Recorded Charge & Temperature Trend",
                    subtitle = "Based strictly on local measurements stored in Room (never fabricated)",
                    badge = "${historySamples.size} POINTS"
                )
                if (historySamples.size < 2) {
                    Text(
                        text = "Tap 'Log Snapshot to History' above to accumulate local trend points over time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val ordered = historySamples.reversed()
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val stepX = size.width / (ordered.size - 1).coerceAtLeast(1)
                        val levelPath = Path()
                        ordered.forEachIndexed { idx, sample ->
                            val x = idx * stepX
                            val y = size.height - (sample.levelPercent / 100f) * size.height
                            if (idx == 0) levelPath.moveTo(x, y) else levelPath.lineTo(x, y)
                            drawCircle(
                                color = PixelLabColors.EmeraldNominal,
                                radius = 4f,
                                center = Offset(x, y)
                            )
                        }
                        drawPath(levelPath, color = PixelLabColors.EmeraldNominal, style = Stroke(width = 3f))
                    }
                }
            }
        }

        // Threshold Alerts Configuration
        item {
            GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                SectionHeader(
                    title = "Custom Battery & Thermal Threshold Alerts",
                    subtitle = "Persisted in DataStore and evaluated by Autopilot rules"
                )
                Text("Low Battery Alert: ${lowSlider.toInt()}%", style = MaterialTheme.typography.labelMedium)
                Slider(value = lowSlider, onValueChange = { lowSlider = it }, valueRange = 5f..50f)

                Text("High Charge Alert: ${highSlider.toInt()}%", style = MaterialTheme.typography.labelMedium)
                Slider(value = highSlider, onValueChange = { highSlider = it }, valueRange = 60f..100f)

                Text("High Temperature Alert: ${tempSlider.toInt()} °C", style = MaterialTheme.typography.labelMedium)
                Slider(value = tempSlider, onValueChange = { tempSlider = it }, valueRange = 35f..50f)

                Button(
                    onClick = {
                        scope.launch {
                            repository.preferences.updateBatteryThresholds(
                                low = lowSlider.toInt(),
                                high = highSlider.toInt(),
                                tempC = tempSlider.toInt()
                            )
                            exportStatus = "Saved battery alert thresholds (${lowSlider.toInt()}% / ${highSlider.toInt()}% / ${tempSlider.toInt()}°C)."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Alert Thresholds")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
