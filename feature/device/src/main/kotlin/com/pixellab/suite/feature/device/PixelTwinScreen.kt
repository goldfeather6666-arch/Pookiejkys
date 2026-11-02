package com.pixellab.suite.feature.device

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixellab.suite.core.common.ExportManager
import com.pixellab.suite.core.common.FormatUtils
import com.pixellab.suite.core.designsystem.GlassCard
import com.pixellab.suite.core.designsystem.PixelLabColors
import com.pixellab.suite.core.designsystem.SectionHeader
import com.pixellab.suite.core.designsystem.TelemetryKeyValueRow
import com.pixellab.suite.data.PixelLabRepository
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class DeviceHotspot(
    val id: String,
    val title: String,
    val subsystem: String,
    val normalizedX: Float,
    val normalizedY: Float,
    val isRearSide: Boolean,
    val liveDetailBuilder: () -> String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PixelTwinScreen(repository: PixelLabRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val identity = remember { repository.hardwareProvider.getDeviceIdentity() }
    val display = remember { repository.hardwareProvider.getDisplayTelemetry() }
    val cpuMem = remember { repository.hardwareProvider.getCpuMemoryStorageTelemetry() }
    val batteryThermal by repository.hardwareProvider.observeBatteryThermal().collectAsState(
        initial = remember { repository.hardwareProvider.getBatteryThermalSnapshot() }
    )
    val features = remember { repository.hardwareProvider.getDeclaredHardwareFeatures() }
    val installedApps = remember { repository.hardwareProvider.getInstalledAppsInventory() }

    var yawDeg by remember { mutableFloatStateOf(18f) }
    var pitchDeg by remember { mutableFloatStateOf(-10f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var showRearView by remember { mutableStateOf(false) }
    var selectedHotspotIndex by remember { mutableIntStateOf(0) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Telemetry, 1: Features, 2: Apps
    var exportStatusBanner by remember { mutableStateOf<String?>(null) }

    val hotspots = remember(identity, display, cpuMem, batteryThermal) {
        listOf(
            DeviceHotspot(
                id = "TENSOR_G4",
                title = "Google Tensor G4 SoC",
                subsystem = "Processor & NPU",
                normalizedX = 0.0f,
                normalizedY = -0.12f,
                isRearSide = false
            ) {
                "SoC: ${identity.socManufacturer} ${identity.socModel} • ${cpuMem.availableProcessors} Cores • ABIs: ${cpuMem.supportedAbis.joinToString()}"
            },
            DeviceHotspot(
                id = "SUPER_ACTUA_OLED",
                title = "Super Actua LTPO Display",
                subsystem = "Display Engine",
                normalizedX = 0.0f,
                normalizedY = -0.55f,
                isRearSide = false
            ) {
                "${display.widthPx}×${display.heightPx} px (${display.densityDpi} dpi) • Active ${display.currentRefreshRateHz} Hz (Modes: ${display.supportedRefreshRatesHz.joinToString()} Hz)"
            },
            DeviceHotspot(
                id = "LPDDR5X_RAM",
                title = "16 GB LPDDR5X & UFS Storage",
                subsystem = "Memory Hierarchy",
                normalizedX = 0.35f,
                normalizedY = 0.05f,
                isRearSide = false
            ) {
                "RAM Free: ${FormatUtils.formatBytes(cpuMem.availableRamBytes)} / ${FormatUtils.formatBytes(cpuMem.totalRamBytes)} • Storage Free: ${FormatUtils.formatBytes(cpuMem.availableInternalStorageBytes)}"
            },
            DeviceHotspot(
                id = "REAR_CAMERA_BAR",
                title = "Pro Triple Camera Bar",
                subsystem = "Optical Array",
                normalizedX = 0.0f,
                normalizedY = -0.58f,
                isRearSide = true
            ) {
                "50MP Main + 48MP Ultrawide + 48MP 5x Telephoto • Camera Features Declared: ${features.count { it.contains("camera") }}"
            },
            DeviceHotspot(
                id = "TITAN_M2",
                title = "Titan M2 Security Subsystem",
                subsystem = "Hardware Root of Trust",
                normalizedX = -0.32f,
                normalizedY = -0.22f,
                isRearSide = true
            ) {
                "Security Patch: ${identity.securityPatch} • StrongBox/Keystore features verified via PackageManager"
            },
            DeviceHotspot(
                id = "BATTERY_VAPOR_CHAMBER",
                title = "Dual-Cell Pack & Vapor Chamber",
                subsystem = "Power & Thermal",
                normalizedX = 0.0f,
                normalizedY = 0.35f,
                isRearSide = true
            ) {
                "Level: ${batteryThermal.levelPercent}% (${batteryThermal.statusLabel}) • Temp: ${batteryThermal.temperatureCelsius}°C • Voltage: ${batteryThermal.voltageMillivolts} mV • Thermal: ${batteryThermal.thermalStatusLabel}"
            }
        )
    }

    val currentHotspot = hotspots[selectedHotspotIndex.coerceIn(0, hotspots.lastIndex)]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "MODULE 01 — PIXEL TWIN",
                subtitle = "Interactive 3D Device Twin & Authentic Hardware Telemetry",
                badge = identity.deviceCodename.uppercase()
            )
        }

        // Interactive 3D Device Canvas
        item {
            GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showRearView) "3D Rear Chassis & Camera Bar" else "3D Front Super Actua Display",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            showRearView = !showRearView
                            yawDeg = if (showRearView) 162f else 18f
                        }) {
                            Text(if (showRearView) "Show Front" else "Flip to Rear")
                        }
                        OutlinedButton(onClick = {
                            yawDeg = if (showRearView) 180f else 0f
                            pitchDeg = 0f
                            zoomScale = 1.0f
                        }) {
                            Text("Reset")
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                yawDeg = (yawDeg + dragAmount.x * 0.45f) % 360f
                                pitchDeg = (pitchDeg - dragAmount.y * 0.35f).coerceIn(-45f, 45f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radYaw = Math.toRadians(yawDeg.toDouble())
                        val radPitch = Math.toRadians(pitchDeg.toDouble())

                        val baseW = size.width * 0.36f * zoomScale
                        val baseH = size.height * 0.78f * zoomScale

                        // 3D foreshortening projection for chassis corners
                        val cosY = cos(radYaw).toFloat()
                        val sinY = sin(radYaw).toFloat()
                        val cosP = cos(radPitch).toFloat()
                        val depthThickness = 18f * zoomScale

                        val halfW = (baseW / 2f) * kotlin.math.abs(cosY).coerceAtLeast(0.28f)
                        val halfH = (baseH / 2f) * kotlin.math.abs(cosP).coerceAtLeast(0.45f)
                        val skewX = sinY * depthThickness
                        val skewY = sin(radPitch).toFloat() * depthThickness

                        // Back depth layer
                        drawRoundRect(
                            color = Color(0xFF232F4D),
                            topLeft = Offset(cx - halfW + skewX, cy - halfH + skewY),
                            size = Size(halfW * 2f, halfH * 2f),
                            cornerRadius = CornerRadius(32f * zoomScale, 32f * zoomScale)
                        )

                        // Main chassis body
                        drawRoundRect(
                            color = Color(0xFF0B1020),
                            topLeft = Offset(cx - halfW, cy - halfH),
                            size = Size(halfW * 2f, halfH * 2f),
                            cornerRadius = CornerRadius(32f * zoomScale, 32f * zoomScale)
                        )
                        drawRoundRect(
                            color = PixelLabColors.CyanPulse,
                            topLeft = Offset(cx - halfW, cy - halfH),
                            size = Size(halfW * 2f, halfH * 2f),
                            cornerRadius = CornerRadius(32f * zoomScale, 32f * zoomScale),
                            style = Stroke(width = 3f)
                        )

                        if (showRearView) {
                            // Iconic Pixel 9 Pro XL Pill-Shaped Camera Bar
                            val barW = halfW * 1.72f
                            val barH = halfH * 0.32f
                            val barTop = cy - halfH * 0.74f
                            drawRoundRect(
                                color = Color(0xFF1E2942),
                                topLeft = Offset(cx - barW / 2f, barTop),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(barH / 2f, barH / 2f)
                            )
                            drawRoundRect(
                                color = PixelLabColors.ElectricViolet,
                                topLeft = Offset(cx - barW / 2f, barTop),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(barH / 2f, barH / 2f),
                                style = Stroke(width = 2.5f)
                            )
                            // Three camera lenses inside bar
                            val lensRadius = barH * 0.26f
                            for (i in -1..1) {
                                drawCircle(
                                    color = Color(0xFF05070B),
                                    radius = lensRadius,
                                    center = Offset(cx + i * (barW * 0.25f), barTop + barH / 2f)
                                )
                                drawCircle(
                                    color = PixelLabColors.CyanPulse,
                                    radius = lensRadius * 0.45f,
                                    center = Offset(cx + i * (barW * 0.25f), barTop + barH / 2f)
                                )
                            }
                        } else {
                            // Front punch-hole camera & internal Tensor G4 wireframe die
                            drawCircle(
                                color = Color.Black,
                                radius = 6f * zoomScale,
                                center = Offset(cx, cy - halfH * 0.86f)
                            )
                            drawRect(
                                color = PixelLabColors.ElectricViolet.copy(alpha = 0.28f),
                                topLeft = Offset(cx - halfW * 0.42f, cy - halfH * 0.28f),
                                size = Size(halfW * 0.84f, halfH * 0.36f)
                            )
                            drawRect(
                                color = PixelLabColors.ElectricViolet,
                                topLeft = Offset(cx - halfW * 0.42f, cy - halfH * 0.28f),
                                size = Size(halfW * 0.84f, halfH * 0.36f),
                                style = Stroke(width = 2f)
                            )
                        }

                        // Draw interactive hotspot markers
                        hotspots.forEachIndexed { idx, spot ->
                            val hx = cx + spot.normalizedX * halfW
                            val hy = cy + spot.normalizedY * halfH
                            val isSelected = idx == selectedHotspotIndex
                            val spotColor = if (isSelected) PixelLabColors.EmeraldNominal else PixelLabColors.CyanPulse
                            drawCircle(
                                color = spotColor.copy(alpha = 0.30f),
                                radius = if (isSelected) 18f else 12f,
                                center = Offset(hx, hy)
                            )
                            drawCircle(
                                color = spotColor,
                                radius = if (isSelected) 7f else 5f,
                                center = Offset(hx, hy)
                            )
                        }
                    }
                }

                Text(
                    text = "Drag to rotate (Yaw: ${yawDeg.toInt()}°, Pitch: ${pitchDeg.toInt()}°) • Zoom: ${String.format("%.1fx", zoomScale)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = zoomScale,
                    onValueChange = { zoomScale = it },
                    valueRange = 0.7f..1.4f
                )

                // Hotspot Selector Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    hotspots.forEachIndexed { index, spot ->
                        FilterChip(
                            selected = selectedHotspotIndex == index,
                            onClick = {
                                selectedHotspotIndex = index
                                showRearView = spot.isRearSide
                            },
                            label = { Text(spot.title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(accentColor = PixelLabColors.EmeraldNominal) {
                    Text(
                        text = "${currentHotspot.title} • ${currentHotspot.subsystem}",
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelLabColors.EmeraldNominal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentHotspot.liveDetailBuilder(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Sub-navigation for Telemetry vs Features vs Installed Packages
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    label = { Text("Hardware & OS") }
                )
                FilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    label = { Text("System Features (${features.size})") }
                )
                FilterChip(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    label = { Text("Installed Apps (${installedApps.size})") }
                )
            }
        }

        if (activeTab == 0) {
            item {
                GlassCard(accentColor = PixelLabColors.CyanPulse) {
                    SectionHeader(title = "Device Identity & Build", badge = "ANDROID ${identity.androidRelease}")
                    TelemetryKeyValueRow("Manufacturer / Brand", "${identity.manufacturer} / ${identity.brand}")
                    TelemetryKeyValueRow("Model / Device Codename", "${identity.model} (${identity.deviceCodename})")
                    TelemetryKeyValueRow("Hardware / Bootloader", "${identity.hardwareName} / ${identity.bootloader}")
                    TelemetryKeyValueRow("SoC Manufacturer / Model", "${identity.socManufacturer} ${identity.socModel}")
                    TelemetryKeyValueRow("API Level / Security Patch", "API ${identity.sdkInt} • ${identity.securityPatch}")
                    TelemetryKeyValueRow("Kernel Version", identity.kernelVersion)
                    TelemetryKeyValueRow("Build Fingerprint", identity.buildFingerprint)
                }
            }

            item {
                GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                    SectionHeader(title = "Display & Refresh Rate Capabilities", badge = "${display.currentRefreshRateHz} Hz")
                    TelemetryKeyValueRow("Resolution", "${display.widthPx} × ${display.heightPx} px")
                    TelemetryKeyValueRow("Pixel Density", "${display.densityDpi} dpi (${display.densityScale}x scale)")
                    TelemetryKeyValueRow("Physical Diagonal", "${display.physicalDiagonalInches} inches")
                    TelemetryKeyValueRow("Supported Refresh Rates", "${display.supportedRefreshRatesHz.joinToString(", ")} Hz")
                    TelemetryKeyValueRow("HDR / Wide Color Gamut", "HDR: ${display.isHdrCapable} • Wide Gamut: ${display.isWideColorGamut}")
                }
            }

            item {
                GlassCard(accentColor = PixelLabColors.EmeraldNominal) {
                    SectionHeader(title = "Processor, RAM & Internal Storage", badge = "${cpuMem.availableProcessors} CORES")
                    TelemetryKeyValueRow("Supported ABIs", cpuMem.supportedAbis.joinToString(", "))
                    TelemetryKeyValueRow("CPU Header", cpuMem.cpuHardwareHeader)
                    cpuMem.coreFrequenciesKhz.take(8).forEach { freqLine ->
                        TelemetryKeyValueRow(freqLine.substringBefore(":"), freqLine.substringAfter(":").trim())
                    }
                    TelemetryKeyValueRow("Total System RAM", FormatUtils.formatBytes(cpuMem.totalRamBytes))
                    TelemetryKeyValueRow("Available System RAM", "${FormatUtils.formatBytes(cpuMem.availableRamBytes)} (${100 - cpuMem.ramUsagePercent}% free)")
                    TelemetryKeyValueRow("Total Internal Storage", FormatUtils.formatBytes(cpuMem.totalInternalStorageBytes))
                    TelemetryKeyValueRow("Available Storage", FormatUtils.formatBytes(cpuMem.availableInternalStorageBytes))
                }
            }
        } else if (activeTab == 1) {
            item {
                GlassCard(accentColor = PixelLabColors.CyanPulse) {
                    SectionHeader(
                        title = "Declared Hardware & Software Features",
                        subtitle = "Queried directly from PackageManager.getSystemAvailableFeatures()",
                        badge = "${features.size} FEATURES"
                    )
                    features.take(60).forEach { feat ->
                        TelemetryKeyValueRow(feat, "SUPPORTED", PixelLabColors.EmeraldNominal)
                    }
                }
            }
        } else {
            item {
                GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                    SectionHeader(
                        title = "Installed Application Inventory",
                        subtitle = "Subject to Android 11+ package visibility rules",
                        badge = "${installedApps.size} PACKAGES"
                    )
                    installedApps.take(50).forEach { pkg ->
                        TelemetryKeyValueRow(
                            label = "${pkg.appName} (${pkg.packageName})",
                            value = "v${pkg.versionName} • SDK ${pkg.targetSdk}"
                        )
                    }
                }
            }
        }

        // Export Hardware Capability Report
        item {
            GlassCard(accentColor = PixelLabColors.CyanPulse) {
                SectionHeader(
                    title = "Hardware Capability Report Export",
                    subtitle = "Generate and share a complete Markdown/JSON diagnostic snapshot"
                )
                if (exportStatusBanner != null) {
                    Text(
                        text = exportStatusBanner!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelLabColors.EmeraldNominal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        val reportMd = buildString {
                            appendLine("# PIXEL LAB — Hardware Capability Report")
                            appendLine("- **Model**: ${identity.manufacturer} ${identity.model} (${identity.deviceCodename})")
                            appendLine("- **Android**: ${identity.androidRelease} (API ${identity.sdkInt}, Patch ${identity.securityPatch})")
                            appendLine("- **SoC**: ${identity.socManufacturer} ${identity.socModel}")
                            appendLine("- **Display**: ${display.widthPx}x${display.heightPx} @ ${display.currentRefreshRateHz} Hz")
                            appendLine("- **RAM**: ${FormatUtils.formatBytes(cpuMem.availableRamBytes)} free / ${FormatUtils.formatBytes(cpuMem.totalRamBytes)} total")
                            appendLine("- **Storage**: ${FormatUtils.formatBytes(cpuMem.availableInternalStorageBytes)} free / ${FormatUtils.formatBytes(cpuMem.totalInternalStorageBytes)} total")
                            appendLine("- **Battery**: ${batteryThermal.levelPercent}% (${batteryThermal.statusLabel}, ${batteryThermal.temperatureCelsius}°C, ${batteryThermal.voltageMillivolts} mV)")
                            appendLine("- **Thermal Status**: ${batteryThermal.thermalStatusLabel}")
                            appendLine("- **Declared Features Count**: ${features.size}")
                        }
                        val file = ExportManager.writeExportFile(context, "pixel_twin_report_${System.currentTimeMillis()}.md", reportMd)
                        exportStatusBanner = "Saved hardware report: ${file.name}"
                        scope.launch {
                            repository.recordActivity("device", "Exported Hardware Report", file.name)
                        }
                        ExportManager.shareExportedFile(context, file, "text/markdown")
                    }) {
                        Text("Export & Share Report (.MD)")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
