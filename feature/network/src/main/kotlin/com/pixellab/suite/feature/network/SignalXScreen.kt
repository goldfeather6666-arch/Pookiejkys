package com.pixellab.suite.feature.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixellab.suite.core.common.ExportManager
import com.pixellab.suite.core.common.FormatUtils
import com.pixellab.suite.core.datastore.UserSettingsState
import com.pixellab.suite.core.designsystem.GlassCard
import com.pixellab.suite.core.designsystem.PixelLabColors
import com.pixellab.suite.core.designsystem.SectionHeader
import com.pixellab.suite.core.designsystem.TelemetryKeyValueRow
import com.pixellab.suite.core.diagnostics.NetworkProbeResult
import com.pixellab.suite.data.PixelLabRepository
import kotlinx.coroutines.launch

@Composable
fun SignalXScreen(repository: PixelLabRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by repository.preferences.settingsFlow.collectAsState(initial = UserSettingsState())
    var refreshTick by remember { mutableIntStateOf(0) }
    val snapshot = remember(refreshTick) { repository.networkProvider.inspectLocalConnectivity() }

    var targetHost by remember { mutableStateOf("dns.google") }
    val probeHistory = remember { mutableStateListOf<NetworkProbeResult>() }
    var exportBanner by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "MODULE 05 — SIGNALX",
                subtitle = "Local Network Interface Inspector & Strict Offline Diagnostics",
                badge = snapshot.transportLabel.uppercase()
            )
        }

        // Strict Offline Profile Control
        item {
            GlassCard(accentColor = PixelLabColors.EmeraldNominal) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strict Offline Profile (Disable External Probes)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "When enabled, SignalX inspects only local kernel network interfaces and LinkProperties and blocks all DNS/TCP socket probes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.strictOfflineProfile,
                        onCheckedChange = { enabled ->
                            scope.launch { repository.preferences.setStrictOfflineProfile(enabled) }
                        }
                    )
                }
            }
        }

        // Device-Local Active Link & Transport Telemetry
        item {
            GlassCard(accentColor = PixelLabColors.CyanPulse) {
                SectionHeader(
                    title = "1. Device-Local Connectivity & LinkProperties",
                    subtitle = "Inspected locally via ConnectivityManager without sending packets",
                    badge = if (snapshot.isConnected) "LINK UP" else "OFFLINE"
                )
                TelemetryKeyValueRow("Active Transport", snapshot.transportLabel)
                TelemetryKeyValueRow("Primary Interface", snapshot.interfaceName)
                TelemetryKeyValueRow("Validated / Metered / VPN", "Validated: ${snapshot.isValidatedInternet} • Metered: ${snapshot.isMetered} • VPN: ${snapshot.isVpnActive}")
                TelemetryKeyValueRow("Configured DNS Servers", snapshot.dnsServers.ifEmpty { listOf("None Exposed") }.joinToString(", "))
                TelemetryKeyValueRow("Default Gateways", snapshot.gatewayAddresses.ifEmpty { listOf("None Exposed") }.joinToString(", "))
                TelemetryKeyValueRow("Search Domains", snapshot.domains)
                TelemetryKeyValueRow(
                    "Estimated Link Bandwidth (Down / Up)",
                    "${snapshot.linkDownstreamKbps?.let { "$it kbps" } ?: "N/A"} / ${snapshot.linkUpstreamKbps?.let { "$it kbps" } ?: "N/A"}"
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { refreshTick++ }) {
                    Text("Refresh Local Link State")
                }
            }
        }

        // Local Kernel Network Interfaces Inventory
        item {
            GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                SectionHeader(
                    title = "2. Local NetworkInterface Enumeration",
                    subtitle = "Enumerated from java.net.NetworkInterface.getNetworkInterfaces()",
                    badge = "${snapshot.localInterfaces.size} IFACES"
                )
                snapshot.localInterfaces.forEach { iface ->
                    TelemetryKeyValueRow(
                        label = "${iface.name} (MTU ${iface.mtu})",
                        value = if (iface.isUp) "UP" else "DOWN",
                        valueColor = if (iface.isUp) PixelLabColors.EmeraldNominal else PixelLabColors.TextMuted
                    )
                    if (iface.ipv4Addresses.isNotEmpty()) {
                        TelemetryKeyValueRow("  IPv4", iface.ipv4Addresses.joinToString(", "))
                    }
                    if (iface.ipv6Addresses.isNotEmpty()) {
                        TelemetryKeyValueRow("  IPv6", iface.ipv6Addresses.take(2).joinToString(", "))
                    }
                }
            }
        }

        // Optional Explicit DNS & TCP Reachability Probe
        item {
            GlassCard(accentColor = PixelLabColors.AmberCaution) {
                SectionHeader(
                    title = "3. Explicit DNS Lookup & TCP/443 Reachability Probe",
                    subtitle = "Requires active network connection and Strict Offline Profile OFF",
                    badge = if (settings.strictOfflineProfile) "LOCKED OFFLINE" else "ON-DEMAND"
                )
                OutlinedTextField(
                    value = targetHost,
                    onValueChange = { targetHost = it },
                    label = { Text("Hostname or IP (e.g. dns.google, 1.1.1.1)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !settings.strictOfflineProfile
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val res = repository.networkProvider.runExplicitDiagnosticProbe(
                                    host = targetHost,
                                    port = 443,
                                    strictOfflineProfile = settings.strictOfflineProfile
                                )
                                probeHistory.add(0, res)
                                repository.recordActivity("network", "Diagnostic Probe (${res.targetHost})", res.diagnosticDetail)
                            }
                        }
                    ) {
                        Text(if (settings.strictOfflineProfile) "Probe Blocked (Strict Offline)" else "Run DNS & Latency Probe")
                    }

                    OutlinedButton(onClick = {
                        val csv = buildString {
                            appendLine("interface,is_up,mtu,ipv4,ipv6")
                            snapshot.localInterfaces.forEach { i ->
                                appendLine("${FormatUtils.escapeCsvField(i.name)},${i.isUp},${i.mtu},${FormatUtils.escapeCsvField(i.ipv4Addresses.joinToString(";"))},${FormatUtils.escapeCsvField(i.ipv6Addresses.joinToString(";"))}")
                            }
                        }
                        val file = ExportManager.writeExportFile(context, "signalx_interfaces_${System.currentTimeMillis()}.csv", csv)
                        exportBanner = "Saved network report: ${file.name}"
                        ExportManager.shareExportedFile(context, file, "text/csv")
                    }) {
                        Text("Export CSV")
                    }
                }

                if (exportBanner != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(exportBanner!!, style = MaterialTheme.typography.bodyMedium, color = PixelLabColors.EmeraldNominal)
                }
            }
        }

        items(probeHistory) { probe ->
            GlassCard(
                accentColor = if (probe.isSuccess) PixelLabColors.EmeraldNominal else PixelLabColors.CrimsonAlert
            ) {
                TelemetryKeyValueRow(
                    label = "${probe.targetHost} (${probe.operationType})",
                    value = probe.roundTripMillis?.let { "$it ms" } ?: "FAILED/BLOCKED",
                    valueColor = if (probe.isSuccess) PixelLabColors.EmeraldNominal else PixelLabColors.CrimsonAlert
                )
                Text(
                    text = probe.diagnosticDetail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
