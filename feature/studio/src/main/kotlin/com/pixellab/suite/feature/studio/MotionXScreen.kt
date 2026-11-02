package com.pixellab.suite.feature.studio

import android.Manifest
import android.graphics.Bitmap
import android.hardware.Sensor
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixellab.suite.core.common.ExportManager
import com.pixellab.suite.core.designsystem.GlassCard
import com.pixellab.suite.core.designsystem.PixelLabColors
import com.pixellab.suite.core.designsystem.SectionHeader
import com.pixellab.suite.core.designsystem.StatusPill
import com.pixellab.suite.core.designsystem.TelemetryKeyValueRow
import com.pixellab.suite.data.PixelLabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class StudioScenePreset(val title: String) {
    TENSOR_HYPERCUBE("Tensor 4D Hypercube"),
    TOROIDAL_KNOT("Toroidal Flux Knot"),
    GEODESIC_SPHERE("Geodesic Dyson Sphere"),
    QUANTUM_PARTICLES("Quantum Particle Field")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MotionXScreen(repository: PixelLabRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPreset by remember { mutableStateOf(StudioScenePreset.TENSOR_HYPERCUBE) }
    var paletteIndex by remember { mutableIntStateOf(0) }
    var motionIntensity by remember { mutableFloatStateOf(1.0f) }
    var gyroReactive by remember { mutableStateOf(true) }
    var lowPowerMode by remember { mutableStateOf(false) }
    var micReactiveEnabled by remember { mutableStateOf(false) }
    var audioAmplitude by remember { mutableFloatStateOf(0f) }

    var phaseRad by remember { mutableFloatStateOf(0f) }
    var dragRotX by remember { mutableFloatStateOf(0.4f) }
    var dragRotY by remember { mutableFloatStateOf(0.6f) }
    var fpsEstimate by remember { mutableIntStateOf(120) }
    var frameTimeMs by remember { mutableFloatStateOf(8.3f) }
    var exportNotice by remember { mutableStateOf<String?>(null) }

    val palettes = listOf(
        "Obsidian Cyber" to Pair(PixelLabColors.ElectricViolet, PixelLabColors.CyanPulse),
        "Emerald Matrix" to Pair(PixelLabColors.EmeraldNominal, PixelLabColors.CyanPulse),
        "Solar Flare" to Pair(PixelLabColors.AmberCaution, PixelLabColors.CrimsonAlert)
    )
    val activeColors = palettes[paletteIndex % palettes.size].second

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micReactiveEnabled = granted
    }

    // Gyroscope tilt collection when enabled
    LaunchedEffect(gyroReactive) {
        if (gyroReactive) {
            repository.sensorProvider.observeSensor(Sensor.TYPE_GYROSCOPE).collect { reading ->
                dragRotX += reading.x * 0.03f
                dragRotY += reading.y * 0.03f
            }
        }
    }

    // Optional Opt-In Microphone Reactivity — Strictly foreground-only, cleaned up onDispose
    DisposableEffect(micReactiveEnabled) {
        var job: kotlinx.coroutines.Job? = null
        if (micReactiveEnabled && repository.permissionCoordinator.isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
            job = scope.launch(Dispatchers.IO) {
                val sampleRate = 16000
                val minBuf = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(1024)
                val recorder = runCatching {
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuf
                    )
                }.getOrNull()

                if (recorder?.state == AudioRecord.STATE_INITIALIZED) {
                    val shortBuf = ShortArray(512)
                    recorder.startRecording()
                    try {
                        while (isActive && micReactiveEnabled) {
                            val read = recorder.read(shortBuf, 0, shortBuf.size)
                            if (read > 0) {
                                var sumSq = 0.0
                                for (i in 0 until read) {
                                    sumSq += shortBuf[i].toDouble() * shortBuf[i].toDouble()
                                }
                                val rms = sqrt(sumSq / read) / 32768.0
                                audioAmplitude = (rms.toFloat() * 4f).coerceIn(0f, 1.5f)
                            }
                            delay(32)
                        }
                    } finally {
                        runCatching {
                            recorder.stop()
                            recorder.release()
                        }
                    }
                }
            }
        } else {
            audioAmplitude = 0f
        }
        onDispose {
            job?.cancel()
            audioAmplitude = 0f
        }
    }

    // Native Compose frame loop with FPS & frame-time diagnostics
    LaunchedEffect(motionIntensity, lowPowerMode) {
        var lastFrameNs = System.nanoTime()
        while (isActive) {
            withFrameNanos { nowNs ->
                val dtMs = ((nowNs - lastFrameNs) / 1_000_000f).coerceAtLeast(1f)
                lastFrameNs = nowNs
                frameTimeMs = frameTimeMs * 0.85f + dtMs * 0.15f
                fpsEstimate = (1000f / frameTimeMs).roundToInt().coerceIn(1, 120)
                phaseRad = (phaseRad + 0.025f * motionIntensity * (1f + audioAmplitude)) % (2f * Math.PI.toFloat() * 10f)
            }
            if (lowPowerMode) {
                delay(24) // Throttle to ~30-40 FPS in Low-Power mode
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "MODULE 07 — MOTIONX",
                subtitle = "Creative 3D Procedural Geometry, Particle Physics & FPS Diagnostics",
                badge = "$fpsEstimate FPS (${String.format(Locale.US, "%.1f ms", frameTimeMs)})"
            )
        }

        // Interactive 3D & Particle Stage
        item {
            GlassCard(accentColor = activeColors.first) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedPreset.title, style = MaterialTheme.typography.titleMedium)
                    if (micReactiveEnabled) {
                        StatusPill(label = "MIC ACTIVE (FOREGROUND ONLY)", color = PixelLabColors.CrimsonAlert)
                    } else {
                        StatusPill(label = if (lowPowerMode) "ECO 30FPS" else "HIGH-REFRESH", color = activeColors.second)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                dragRotY += dragAmount.x * 0.01f
                                dragRotX += dragAmount.y * 0.01f
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val scale = minOf(size.width, size.height) * (0.34f + audioAmplitude * 0.08f)

                        val vertices3D = mutableListOf<Triple<Float, Float, Float>>()
                        val edges = mutableListOf<Pair<Int, Int>>()

                        when (selectedPreset) {
                            StudioScenePreset.TENSOR_HYPERCUBE -> {
                                for (i in 0 until 16) {
                                    val x = if ((i and 1) != 0) 1f else -1f
                                    val y = if ((i and 2) != 0) 1f else -1f
                                    val z = if ((i and 4) != 0) 1f else -1f
                                    val w = if ((i and 8) != 0) 0.55f else -0.55f
                                    val wFactor = 1f / (2.2f - w * cos(phaseRad))
                                    vertices3D.add(Triple(x * wFactor, y * wFactor, z * wFactor))
                                }
                                for (i in 0 until 16) {
                                    for (bit in 0..3) {
                                        val j = i xor (1 shl bit)
                                        if (i < j) edges.add(i to j)
                                    }
                                }
                            }
                            StudioScenePreset.TOROIDAL_KNOT -> {
                                val steps = 72
                                for (i in 0 until steps) {
                                    val t = (i.toFloat() / steps) * 2f * Math.PI.toFloat()
                                    val r = 0.65f + 0.28f * cos(3f * t + phaseRad)
                                    val x = r * cos(2f * t)
                                    val y = r * sin(2f * t)
                                    val z = 0.35f * sin(3f * t + phaseRad)
                                    vertices3D.add(Triple(x, y, z))
                                    edges.add(i to ((i + 1) % steps))
                                }
                            }
                            StudioScenePreset.GEODESIC_SPHERE -> {
                                val rings = 8
                                val sectors = 12
                                for (r in 0..rings) {
                                    val phi = Math.PI * r / rings
                                    for (s in 0 until sectors) {
                                        val theta = 2.0 * Math.PI * s / sectors
                                        val x = (sin(phi) * cos(theta)).toFloat() * 0.8f
                                        val y = cos(phi).toFloat() * 0.8f
                                        val z = (sin(phi) * sin(theta)).toFloat() * 0.8f
                                        vertices3D.add(Triple(x, y, z))
                                        val cur = r * sectors + s
                                        val nextS = r * sectors + ((s + 1) % sectors)
                                        edges.add(cur to nextS)
                                    }
                                }
                            }
                            StudioScenePreset.QUANTUM_PARTICLES -> {
                                val count = 64
                                for (i in 0 until count) {
                                    val a = i * 0.37f + phaseRad * 0.5f
                                    val rad = 0.25f + (i % 9) * 0.08f
                                    vertices3D.add(
                                        Triple(
                                            rad * cos(a),
                                            sin(i * 0.5f + phaseRad) * 0.7f,
                                            rad * sin(a)
                                        )
                                    )
                                }
                            }
                        }

                        val rotY = dragRotY + phaseRad * 0.35f
                        val rotX = dragRotX + phaseRad * 0.22f
                        val cosY = cos(rotY)
                        val sinY = sin(rotY)
                        val cosX = cos(rotX)
                        val sinX = sin(rotX)

                        val projected = vertices3D.map { (vx, vy, vz) ->
                            val x1 = vx * cosY - vz * sinY
                            val z1 = vx * sinY + vz * cosY
                            val y2 = vy * cosX - z1 * sinX
                            val z2 = vy * sinX + z1 * cosX
                            val perspective = 2.4f / (2.8f - z2).coerceAtLeast(0.5f)
                            Offset(cx + x1 * scale * perspective, cy + y2 * scale * perspective)
                        }

                        edges.forEach { (u, v) ->
                            if (u < projected.size && v < projected.size) {
                                drawLine(
                                    color = activeColors.first.copy(alpha = 0.75f),
                                    start = projected[u],
                                    end = projected[v],
                                    strokeWidth = 2.6f
                                )
                            }
                        }

                        projected.forEachIndexed { idx, pt ->
                            drawCircle(
                                color = if (idx % 2 == 0) activeColors.second else activeColors.first,
                                radius = 5.5f + audioAmplitude * 4f,
                                center = pt
                            )
                        }
                    }
                }
            }
        }

        // Scene & Shader Controls
        item {
            GlassCard(accentColor = PixelLabColors.CyanPulse) {
                SectionHeader(title = "3D Geometry Preset & Color Environment")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StudioScenePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset },
                            label = { Text(preset.title) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    palettes.forEachIndexed { idx, (name, _) ->
                        FilterChip(
                            selected = paletteIndex == idx,
                            onClick = { paletteIndex = idx },
                            label = { Text(name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Motion Intensity: ${String.format(Locale.US, "%.1fx", motionIntensity)}", style = MaterialTheme.typography.labelMedium)
                Slider(value = motionIntensity, onValueChange = { motionIntensity = it }, valueRange = 0f..2.5f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gyroscope Tilt Reactivity", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = gyroReactive, onCheckedChange = { gyroReactive = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Low-Power Thermal Throttle Mode", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = lowPowerMode, onCheckedChange = { lowPowerMode = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Opt-In Microphone Audio Reactivity", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = micReactiveEnabled,
                        onCheckedChange = { wantEnable ->
                            if (wantEnable) {
                                if (repository.permissionCoordinator.isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
                                    micReactiveEnabled = true
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                micReactiveEnabled = false
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // Render high-resolution 1080x1080 PNG frame to app exports
                        val bmp = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bmp)
                        canvas.drawColor(android.graphics.Color.parseColor("#07090F"))
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#00E5FF")
                            strokeWidth = 5f
                            style = android.graphics.Paint.Style.STROKE
                        }
                        for (i in 1..8) {
                            canvas.drawCircle(540f, 540f, i * 55f, paint)
                        }
                        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
                        val outFile = File(exportDir, "motionx_frame_${System.currentTimeMillis()}.png")
                        FileOutputStream(outFile).use { fos ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        }
                        exportNotice = "Exported 1080×1080 PNG artwork: ${outFile.name}"
                        scope.launch {
                            repository.recordActivity("studio", "Exported MotionX Artwork", outFile.name)
                        }
                        ExportManager.shareExportedFile(context, outFile, "image/png")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export High-Res Procedural Frame (.PNG)")
                }

                if (exportNotice != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(exportNotice!!, style = MaterialTheme.typography.bodyMedium, color = PixelLabColors.EmeraldNominal)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
