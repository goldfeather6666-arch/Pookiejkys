package com.pixellab.suite.feature.sensors

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.pixellab.suite.core.database.SensorSessionEntity
import com.pixellab.suite.core.designsystem.GlassCard
import com.pixellab.suite.core.designsystem.PixelLabColors
import com.pixellab.suite.core.designsystem.SectionHeader
import com.pixellab.suite.core.designsystem.TelemetryKeyValueRow
import com.pixellab.suite.core.diagnostics.LiveSensorReading
import com.pixellab.suite.data.PixelLabRepository
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensoriumScreen(repository: PixelLabRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allSensors = remember { repository.sensorProvider.discoverAllSensors() }
    val recordedSessions by repository.dao.observeSensorSessions().collectAsState(initial = emptyList())

    var selectedSensorType by remember {
        mutableIntStateOf(allSensors.firstOrNull { it.type == Sensor.TYPE_ACCELEROMETER }?.type ?: (allSensors.firstOrNull()?.type ?: Sensor.TYPE_ACCELEROMETER))
    }
    var samplingDelay by remember { mutableIntStateOf(SensorManager.SENSOR_DELAY_UI) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recordingStartMillis by remember { mutableStateOf(0L) }
    var csvExportMessage by remember { mutableStateOf<String?>(null) }

    val waveformHistory = remember { mutableStateListOf<LiveSensorReading>() }
    val recordedBuffer = remember { mutableStateListOf<LiveSensorReading>() }

    val activeSensorSpec = allSensors.firstOrNull { it.type == selectedSensorType }

    // Lifecycle-safe collection: automatically stops listening when composable leaves composition
    LaunchedEffect(selectedSensorType, samplingDelay) {
        waveformHistory.clear()
        repository.sensorProvider.observeSensor(selectedSensorType, samplingDelay).collect { reading ->
            waveformHistory.add(reading)
            if (waveformHistory.size > 90) {
                waveformHistory.removeAt(0)
            }
            if (isRecording && !isPaused) {
                recordedBuffer.add(reading)
            }
        }
    }

    val latest = waveformHistory.lastOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "MODULE 03 — SENSORIUM",
                subtitle = "Dynamic SensorManager Discovery, Oscilloscope & CSV Recorder",
                badge = "${allSensors.size} SENSORS"
            )
        }

        // Sensor Selector & Sampling Rate
        item {
            GlassCard(accentColor = PixelLabColors.EmeraldNominal) {
                SectionHeader(title = "Select Active Hardware/Fused Sensor", badge = activeSensorSpec?.typeName ?: "NONE")
                if (allSensors.isEmpty()) {
                    Text(
                        text = "Unavailable: Android SensorManager reported 0 hardware sensors on this device/emulator.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelLabColors.AmberCaution
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allSensors.take(14).forEach { spec ->
                            FilterChip(
                                selected = spec.type == selectedSensorType,
                                onClick = { selectedSensorType = spec.type },
                                label = { Text(spec.typeName) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Sampling Rate Control", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SensorManager.SENSOR_DELAY_NORMAL to "NORMAL",
                        SensorManager.SENSOR_DELAY_UI to "UI (60Hz)",
                        SensorManager.SENSOR_DELAY_GAME to "GAME"
                    ).forEach { (rate, label) ->
                        FilterChip(
                            selected = samplingDelay == rate,
                            onClick = { samplingDelay = rate },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Live 3-Axis Oscilloscope & Numerical Readout
        item {
            GlassCard(accentColor = PixelLabColors.CyanPulse) {
                SectionHeader(
                    title = "Live 3-Axis Waveform & Vector Magnitude",
                    subtitle = activeSensorSpec?.let { "${it.name} • Vendor: ${it.vendor} • Power: ${it.powerMa} mA" } ?: "Awaiting sensor stream",
                    badge = if (latest != null) String.format(Locale.US, "|V| = %.2f", latest.magnitude) else "NO STREAM"
                )

                TelemetryKeyValueRow("Axis X (Electric Violet)", String.format(Locale.US, "%.4f", latest?.x ?: 0f), PixelLabColors.ElectricVioletSoft)
                TelemetryKeyValueRow("Axis Y (Cyan Pulse)", String.format(Locale.US, "%.4f", latest?.y ?: 0f), PixelLabColors.CyanPulse)
                TelemetryKeyValueRow("Axis Z (Emerald)", String.format(Locale.US, "%.4f", latest?.z ?: 0f), PixelLabColors.EmeraldNominal)

                Spacer(modifier = Modifier.height(8.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    val midY = size.height / 2f
                    drawLine(
                        color = PixelLabColors.GlassBorder,
                        start = Offset(0f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 1.5f
                    )
                    if (waveformHistory.size >= 2) {
                        val maxAbs = waveformHistory.maxOf {
                            maxOf(kotlin.math.abs(it.x), kotlin.math.abs(it.y), kotlin.math.abs(it.z), 1f)
                        }
                        val stepX = size.width / (waveformHistory.size - 1).coerceAtLeast(1)

                        fun drawAxisPath(selector: (LiveSensorReading) -> Float, color: androidx.compose.ui.graphics.Color) {
                            val path = Path()
                            waveformHistory.forEachIndexed { idx, sample ->
                                val px = idx * stepX
                                val normalized = (selector(sample) / maxAbs).coerceIn(-1f, 1f)
                                val py = midY - normalized * (size.height * 0.44f)
                                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            drawPath(path = path, color = color, style = Stroke(width = 3f))
                        }

                        drawAxisPath({ it.x }, PixelLabColors.ElectricVioletSoft)
                        drawAxisPath({ it.y }, PixelLabColors.CyanPulse)
                        drawAxisPath({ it.z }, PixelLabColors.EmeraldNominal)
                    }
                }
            }
        }

        // Recording Session & CSV Export Controls
        item {
            GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                SectionHeader(
                    title = "Laboratory Session Recorder & CSV Export",
                    subtitle = "Captures timestamped X/Y/Z/Magnitude samples to RFC-4180 CSV",
                    badge = "${recordedBuffer.size} SAMPLES"
                )
                if (csvExportMessage != null) {
                    Text(
                        text = csvExportMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelLabColors.EmeraldNominal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isRecording) {
                        Button(onClick = {
                            recordedBuffer.clear()
                            recordingStartMillis = System.currentTimeMillis()
                            isPaused = false
                            isRecording = true
                        }) {
                            Text("Start Recording")
                        }
                    } else {
                        OutlinedButton(onClick = { isPaused = !isPaused }) {
                            Text(if (isPaused) "Resume" else "Pause")
                        }
                        Button(onClick = {
                            isRecording = false
                            isPaused = false
                            val samples = recordedBuffer.toList()
                            if (samples.isNotEmpty()) {
                                val csv = buildString {
                                    appendLine("timestamp_ms,sensor_name,x,y,z,magnitude,accuracy")
                                    samples.forEach { s ->
                                        appendLine("${s.wallClockMillis},${FormatUtils.escapeCsvField(s.sensorName)},${s.x},${s.y},${s.z},${s.magnitude},${s.accuracy}")
                                    }
                                }
                                val file = ExportManager.writeExportFile(
                                    context,
                                    "sensorium_${selectedSensorType}_${System.currentTimeMillis()}.csv",
                                    csv
                                )
                                csvExportMessage = "Exported ${samples.size} samples to ${file.name}"
                                scope.launch {
                                    repository.dao.insertSensorSession(
                                        SensorSessionEntity(
                                            sensorName = activeSensorSpec?.name ?: "Sensor $selectedSensorType",
                                            sensorType = selectedSensorType,
                                            startedAt = recordingStartMillis,
                                            endedAt = System.currentTimeMillis(),
                                            sampleCount = samples.size,
                                            minMagnitude = samples.minOf { it.magnitude },
                                            maxMagnitude = samples.maxOf { it.magnitude },
                                            avgMagnitude = samples.map { it.magnitude }.average().toFloat(),
                                            exportedCsvPath = file.absolutePath
                                        )
                                    )
                                    repository.recordActivity("sensors", "Exported Sensor CSV", "${samples.size} samples (${file.name})")
                                }
                                ExportManager.shareExportedFile(context, file, "text/csv")
                            }
                        }) {
                            Text("Stop & Export CSV")
                        }
                    }
                }
            }
        }

        // Sensor Hardware Metadata Inventory
        if (activeSensorSpec != null) {
            item {
                GlassCard(accentColor = PixelLabColors.CyanPulse) {
                    SectionHeader(title = "Selected Sensor Hardware Metadata", badge = "v${activeSensorSpec.version}")
                    TelemetryKeyValueRow("Hardware Name", activeSensorSpec.name)
                    TelemetryKeyValueRow("Vendor", activeSensorSpec.vendor)
                    TelemetryKeyValueRow("Resolution", "${activeSensorSpec.resolution}")
                    TelemetryKeyValueRow("Maximum Range", "${activeSensorSpec.maximumRange}")
                    TelemetryKeyValueRow("Power Draw", "${activeSensorSpec.powerMa} mA")
                    TelemetryKeyValueRow("Min Delay", "${activeSensorSpec.minDelayMicros} µs")
                    TelemetryKeyValueRow("Wake-Up Sensor", "${activeSensorSpec.isWakeUpSensor}")
                }
            }
        }

        // Historical Recorded Sessions
        if (recordedSessions.isNotEmpty()) {
            item {
                GlassCard(accentColor = PixelLabColors.EmeraldNominal) {
                    SectionHeader(title = "Recorded Laboratory Sessions", badge = "${recordedSessions.size} SESSIONS")
                    recordedSessions.take(8).forEach { sess ->
                        TelemetryKeyValueRow(
                            label = "${sess.sensorName} (${sess.sampleCount} pts)",
                            value = "Avg |V|=${String.format(Locale.US, "%.2f", sess.avgMagnitude)}"
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
