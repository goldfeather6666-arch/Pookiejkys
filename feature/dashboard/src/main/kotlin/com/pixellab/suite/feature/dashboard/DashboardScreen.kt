package com.pixellab.suite.feature.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pixellab.suite.core.common.FormatUtils
import com.pixellab.suite.core.designsystem.GlassCard
import com.pixellab.suite.core.designsystem.PixelLabColors
import com.pixellab.suite.core.designsystem.SectionHeader
import com.pixellab.suite.core.designsystem.StatusPill
import com.pixellab.suite.core.designsystem.TelemetryKeyValueRow
import com.pixellab.suite.data.PixelLabRepository
import kotlinx.coroutines.launch

data class PixelLabModuleDescriptor(
    val route: String,
    val numberBadge: String,
    val title: String,
    val subtitle: String,
    val keywords: String,
    val accent: Color,
    val icon: ImageVector
)

val ALL_PIXEL_LAB_MODULES = listOf(
    PixelLabModuleDescriptor(
        route = "device",
        numberBadge = "MOD 01",
        title = "PIXEL TWIN",
        subtitle = "3D Device Twin & Hardware Telemetry",
        keywords = "cpu ram storage display refresh rate soc tensor komodo apps export 3d",
        accent = PixelLabColors.ElectricViolet,
        icon = Icons.Default.PhoneAndroid
    ),
    PixelLabModuleDescriptor(
        route = "automation",
        numberBadge = "MOD 02",
        title = "AUTOPILOT",
        subtitle = "Offline Rule & Routine Engine",
        keywords = "automation triggers battery charging schedule alarm workmanager routines",
        accent = PixelLabColors.CyanPulse,
        icon = Icons.Default.PlayArrow
    ),
    PixelLabModuleDescriptor(
        route = "sensors",
        numberBadge = "MOD 03",
        title = "SENSORIUM",
        subtitle = "Real-Time Hardware Sensor Lab",
        keywords = "accelerometer gyroscope magnetometer barometer light proximity csv waveform",
        accent = PixelLabColors.EmeraldNominal,
        icon = Icons.Default.Sensors
    ),
    PixelLabModuleDescriptor(
        route = "localai",
        numberBadge = "MOD 04",
        title = "LOCALMIND",
        subtitle = "On-Device AI & Document Studio",
        keywords = "gguf tflite llm ai summarize qa benchmark tensor g4 memory",
        accent = PixelLabColors.ElectricVioletSoft,
        icon = Icons.Default.Memory
    ),
    PixelLabModuleDescriptor(
        route = "network",
        numberBadge = "MOD 05",
        title = "SIGNALX",
        subtitle = "Offline-Capable Network Diagnostics",
        keywords = "wifi ip dns gateway interface mtu ping latency offline",
        accent = PixelLabColors.CyanPulse,
        icon = Icons.Default.NetworkCheck
    ),
    PixelLabModuleDescriptor(
        route = "battery",
        numberBadge = "MOD 06",
        title = "ENERGIX",
        subtitle = "Battery Intelligence & Thermal Lab",
        keywords = "battery voltage temperature current charging health alerts",
        accent = PixelLabColors.EmeraldNominal,
        icon = Icons.Default.BatteryChargingFull
    ),
    PixelLabModuleDescriptor(
        route = "studio",
        numberBadge = "MOD 07",
        title = "MOTIONX",
        subtitle = "3D Procedural & Shader Studio",
        keywords = "3d particles shader gyroscope audio reactive fps canvas export",
        accent = PixelLabColors.AmberCaution,
        icon = Icons.Default.AutoAwesome
    ),
    PixelLabModuleDescriptor(
        route = "productivity",
        numberBadge = "MOD 08",
        title = "EDGEFLOW",
        subtitle = "Calculator, Notes, QR & Utilities",
        keywords = "calculator scientific notes clipboard timer stopwatch converter qr sha256",
        accent = PixelLabColors.CyanPulse,
        icon = Icons.Default.Build
    ),
    PixelLabModuleDescriptor(
        route = "privacy",
        numberBadge = "PRIVACY",
        title = "PRIVACY CENTER",
        subtitle = "Permissions, Storage & Data Wipe",
        keywords = "permissions audit backup wipe storage security offline",
        accent = PixelLabColors.EmeraldNominal,
        icon = Icons.Default.PrivacyTip
    ),
    PixelLabModuleDescriptor(
        route = "settings",
        numberBadge = "CONFIG",
        title = "SYSTEM SETTINGS",
        subtitle = "Theme, Contrast, Motion & Profiles",
        keywords = "theme dark light high contrast reduced motion thermal offline",
        accent = PixelLabColors.ElectricViolet,
        icon = Icons.Default.Settings
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    repository: PixelLabRepository,
    onNavigateToModule: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by repository.preferences.settingsFlow.collectAsState(initial = com.pixellab.suite.core.datastore.UserSettingsState())
    val batteryThermal by repository.hardwareProvider.observeBatteryThermal().collectAsState(
        initial = remember { repository.hardwareProvider.getBatteryThermalSnapshot() }
    )
    val routines by repository.dao.observeRoutines().collectAsState(initial = emptyList())
    val aiModels by repository.dao.observeAiModels().collectAsState(initial = emptyList())
    val activityLogs by repository.dao.observeActivityLogs().collectAsState(initial = emptyList())

    var refreshTick by remember { mutableStateOf(0) }
    val identity = remember(refreshTick) { repository.hardwareProvider.getDeviceIdentity() }
    val display = remember(refreshTick) { repository.hardwareProvider.getDisplayTelemetry() }
    val cpuMem = remember(refreshTick) { repository.hardwareProvider.getCpuMemoryStorageTelemetry() }
    val sensorCount = remember { repository.sensorProvider.discoverAllSensors().size }

    var searchQuery by remember { mutableStateOf("") }
    var customizeLayoutMode by remember { mutableStateOf(false) }

    val filteredModules = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            ALL_PIXEL_LAB_MODULES
        } else {
            val q = searchQuery.trim().lowercase()
            ALL_PIXEL_LAB_MODULES.filter {
                it.title.lowercase().contains(q) ||
                    it.subtitle.lowercase().contains(q) ||
                    it.keywords.contains(q)
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
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PIXEL LAB",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${identity.manufacturer.uppercase()} ${identity.model} • API ${identity.sdkInt}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PixelLabColors.CyanPulse
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Telemetry")
                    }
                    IconButton(onClick = { customizeLayoutMode = !customizeLayoutMode }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Customize Dashboard Cards",
                            tint = if (customizeLayoutMode) PixelLabColors.CyanPulse else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Search & Quick Filter Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search modules, sensors, tools, or telemetry...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )
        }

        // Dashboard customization panel when active
        if (customizeLayoutMode) {
            item {
                GlassCard(accentColor = PixelLabColors.CyanPulse) {
                    SectionHeader(
                        title = "Customize Command Center",
                        subtitle = "Reorder or toggle visibility of dashboard telemetry cards",
                        badge = "LAYOUT"
                    )
                    settings.dashboardCardOrder.forEach { cardId ->
                        val isHidden = cardId in settings.hiddenDashboardCards
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cardId.replace('_', ' '),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Row {
                                IconButton(onClick = {
                                    scope.launch { repository.preferences.moveDashboardCard(cardId, -1) }
                                }) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                }
                                IconButton(onClick = {
                                    scope.launch { repository.preferences.moveDashboardCard(cardId, 1) }
                                }) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                }
                                IconButton(onClick = {
                                    scope.launch { repository.preferences.toggleDashboardCardVisibility(cardId) }
                                }) {
                                    Icon(
                                        if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Card Visibility"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Render ordered visible cards
        val visibleOrder = settings.dashboardCardOrder.filter { it !in settings.hiddenDashboardCards }
        items(visibleOrder, key = { it }) { cardId ->
            when (cardId) {
                "IDENTITY_VITALS" -> {
                    GlassCard(
                        accentColor = PixelLabColors.ElectricViolet,
                        onClick = { onNavigateToModule("device") }
                    ) {
                        SectionHeader(
                            title = "${identity.model} (${identity.deviceCodename})",
                            subtitle = "SoC: ${identity.socManufacturer} ${identity.socModel} • ${cpuMem.availableProcessors} Cores",
                            badge = "${display.currentRefreshRateHz.toInt()} Hz",
                            accentColor = PixelLabColors.CyanPulse
                        )
                        TelemetryKeyValueRow("Display Resolution", "${display.widthPx} × ${display.heightPx} px (${display.densityDpi} dpi)")
                        TelemetryKeyValueRow("Active Hardware Sensors", "$sensorCount exposed via SensorManager")
                        TelemetryKeyValueRow(
                            "Offline Profile",
                            if (settings.strictOfflineProfile) "STRICT OFFLINE LOCKED" else "Local-First Standard",
                            valueColor = PixelLabColors.EmeraldNominal
                        )
                    }
                }

                "BATTERY_THERMAL" -> {
                    GlassCard(
                        accentColor = PixelLabColors.EmeraldNominal,
                        onClick = { onNavigateToModule("battery") }
                    ) {
                        SectionHeader(
                            title = "Energy & Thermal Telemetry",
                            subtitle = "${batteryThermal.statusLabel} • ${batteryThermal.plugSourceLabel}",
                            badge = "${batteryThermal.levelPercent}%",
                            accentColor = if (batteryThermal.levelPercent > 20) PixelLabColors.EmeraldNominal else PixelLabColors.AmberCaution
                        )
                        LinearProgressIndicator(
                            progress = { batteryThermal.levelPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            color = PixelLabColors.EmeraldNominal
                        )
                        TelemetryKeyValueRow("Pack Temperature / Voltage", "${batteryThermal.temperatureCelsius}°C • ${batteryThermal.voltageMillivolts} mV")
                        TelemetryKeyValueRow(
                            "System Thermal State",
                            batteryThermal.thermalStatusLabel,
                            valueColor = if (batteryThermal.thermalStatusCode <= 1) PixelLabColors.EmeraldNominal else PixelLabColors.AmberCaution
                        )
                    }
                }

                "MEMORY_STORAGE" -> {
                    GlassCard(
                        accentColor = PixelLabColors.CyanPulse,
                        onClick = { onNavigateToModule("device") }
                    ) {
                        SectionHeader(
                            title = "LPDDR5X Memory & UFS Storage",
                            subtitle = "Live OS allocation via ActivityManager & StatFs",
                            badge = "RAM ${cpuMem.ramUsagePercent}%",
                            accentColor = PixelLabColors.CyanPulse
                        )
                        TelemetryKeyValueRow(
                            "System RAM Used / Total",
                            "${FormatUtils.formatBytes(cpuMem.usedRamBytes)} / ${FormatUtils.formatBytes(cpuMem.totalRamBytes)}"
                        )
                        LinearProgressIndicator(
                            progress = { cpuMem.ramUsagePercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = PixelLabColors.CyanPulse
                        )
                        TelemetryKeyValueRow(
                            "Internal Storage Used / Total",
                            "${FormatUtils.formatBytes(cpuMem.usedInternalStorageBytes)} / ${FormatUtils.formatBytes(cpuMem.totalInternalStorageBytes)} (${cpuMem.storageUsagePercent}%)"
                        )
                    }
                }

                "LOCAL_AI_AUTOPILOT" -> {
                    GlassCard(accentColor = PixelLabColors.ElectricVioletSoft) {
                        SectionHeader(
                            title = "LocalMind AI & Autopilot Engine",
                            subtitle = "Zero-cloud on-device intelligence & automation status",
                            badge = "${routines.count { it.isEnabled }} ACTIVE RULES"
                        )
                        TelemetryKeyValueRow(
                            "Registered Local AI Models",
                            if (aiModels.isEmpty()) "0 Installed (Offline NLP Utilities Ready)" else "${aiModels.size} Verified Model(s)"
                        )
                        TelemetryKeyValueRow(
                            "Available Free RAM for AI",
                            FormatUtils.formatBytes(cpuMem.availableRamBytes),
                            valueColor = PixelLabColors.CyanPulse
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onNavigateToModule("localai") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open LocalMind")
                            }
                            Button(
                                onClick = { onNavigateToModule("automation") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Autopilot")
                            }
                        }
                    }
                }

                "MODULES_GRID" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            title = "Core Engineering Modules",
                            subtitle = "Tap any module to launch or star to pin to Favorites",
                            badge = "${filteredModules.size} MODULES"
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ALL_PIXEL_LAB_MODULES.filter { it.route in settings.favoriteModules }.forEach { fav ->
                                AssistChip(
                                    onClick = { onNavigateToModule(fav.route) },
                                    label = { Text("★ ${fav.title}") }
                                )
                            }
                        }

                        filteredModules.forEach { mod ->
                            val isFav = mod.route in settings.favoriteModules
                            GlassCard(
                                accentColor = mod.accent,
                                onClick = { onNavigateToModule(mod.route) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = mod.icon,
                                            contentDescription = mod.title,
                                            tint = mod.accent
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                StatusPill(label = mod.numberBadge, color = mod.accent)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = mod.title,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = mod.subtitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        scope.launch { repository.preferences.toggleFavoriteModule(mod.route) }
                                    }) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Favorite Module",
                                            tint = if (isFav) PixelLabColors.AmberCaution else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "RECENT_ACTIVITY" -> {
                    GlassCard(accentColor = PixelLabColors.CyanPulse) {
                        SectionHeader(
                            title = "Recent Local Activity Log",
                            subtitle = "Recorded diagnostic exports, automations & tool actions",
                            badge = "${activityLogs.size} EVENTS"
                        )
                        if (activityLogs.isEmpty()) {
                            Text(
                                text = "No activity recorded in this session yet. Run a diagnostic probe, export a report, or trigger a routine.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            activityLogs.take(5).forEach { log ->
                                TelemetryKeyValueRow(
                                    label = "[${log.moduleId.uppercase()}] ${log.actionTitle}",
                                    value = FormatUtils.formatTimestamp(log.timestamp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
}
