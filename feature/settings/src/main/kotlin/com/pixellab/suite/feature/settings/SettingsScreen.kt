package com.pixellab.suite.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixellab.suite.core.datastore.UserSettingsState
import com.pixellab.suite.core.designsystem.GlassCard
import com.pixellab.suite.core.designsystem.PixelLabColors
import com.pixellab.suite.core.designsystem.SectionHeader
import com.pixellab.suite.core.designsystem.ThemeMode
import com.pixellab.suite.data.PixelLabRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(repository: PixelLabRepository) {
    val scope = rememberCoroutineScope()
    val settings by repository.preferences.settingsFlow.collectAsState(initial = UserSettingsState())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "SYSTEM SETTINGS & DESIGN TOKENS",
                subtitle = "Configure Obsidian Theme, High Contrast, Motion & Thermal Policies",
                badge = settings.themeMode.name
            )
        }

        item {
            GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                SectionHeader(title = "Visual Theme Mode")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { scope.launch { repository.preferences.setThemeMode(mode) } },
                            label = { Text(mode.name.replace('_', ' ')) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("High-Contrast Accessibility Mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Replaces translucent glass with pure black surfaces and 2dp high-luminance borders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.highContrast,
                        onCheckedChange = { scope.launch { repository.preferences.setHighContrast(it) } }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reduced Motion Mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Minimizes spring animations and ambient studio motion for accessibility and battery savings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.reducedMotion,
                        onCheckedChange = { scope.launch { repository.preferences.setReducedMotion(it) } }
                    )
                }
            }
        }

        item {
            GlassCard(accentColor = PixelLabColors.CyanPulse) {
                SectionHeader(title = "Pixel 9 Pro XL Hardware & Privacy Policies")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thermal-Aware Performance Scaling", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Automatically throttles 3D Canvas & benchmark threads when PowerManager reports moderate/severe thermal load.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.thermalAwareMode,
                        onCheckedChange = { scope.launch { repository.preferences.setThermalAwareMode(it) } }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Offline Network Lock", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Disables all external DNS/TCP reachability probes in SignalX while retaining local interface inspection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.strictOfflineProfile,
                        onCheckedChange = { scope.launch { repository.preferences.setStrictOfflineProfile(it) } }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
