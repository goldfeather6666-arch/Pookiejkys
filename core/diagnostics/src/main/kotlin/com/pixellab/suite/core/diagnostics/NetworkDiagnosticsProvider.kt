package com.pixellab.suite.core.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections

data class LocalNetworkInterfaceSpec(
    val name: String,
    val displayName: String,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val mtu: Int,
    val ipv4Addresses: List<String>,
    val ipv6Addresses: List<String>
)

data class ConnectivityDiagnosticsSnapshot(
    val isConnected: Boolean,
    val transportLabel: String,
    val isMetered: Boolean,
    val isVpnActive: Boolean,
    val isValidatedInternet: Boolean,
    val interfaceName: String,
    val dnsServers: List<String>,
    val gatewayAddresses: List<String>,
    val domains: String,
    val linkDownstreamKbps: Int?,
    val linkUpstreamKbps: Int?,
    val localInterfaces: List<LocalNetworkInterfaceSpec>
)

data class NetworkProbeResult(
    val targetHost: String,
    val operationType: String,   // "DNS_LOOKUP" or "TCP_ECHO_LATENCY"
    val isSuccess: Boolean,
    val resolvedAddrs: List<String>,
    val roundTripMillis: Long?,
    val diagnosticDetail: String,
    val timestamp: Long = System.currentTimeMillis()
)

class NetworkDiagnosticsProvider(private val context: Context) {

    fun inspectLocalConnectivity(): ConnectivityDiagnosticsSnapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps: NetworkCapabilities? = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val linkProps: LinkProperties? = activeNetwork?.let { cm.getLinkProperties(it) }

        val isConnected = caps != null
        val transport = when {
            caps == null -> "Disconnected / Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi (802.11)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "USB / Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth PAN"
            else -> "Other Active Transport"
        }

        val isMetered = cm?.isActiveNetworkMetered ?: false
        val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
        val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false

        val ifaceName = linkProps?.interfaceName ?: "None"
        val dnsList = linkProps?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()
        val gateways = linkProps?.routes
            ?.mapNotNull { it.gateway?.hostAddress }
            ?.filter { it != "0.0.0.0" && it != "::" }
            ?.distinct()
            ?: emptyList()
        val searchDomains = linkProps?.domains ?: "None"

        val downKbps = caps?.linkDownstreamBandwidthKbps?.takeIf { it > 0 }
        val upKbps = caps?.linkUpstreamBandwidthKbps?.takeIf { it > 0 }

        val interfaces = runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces()).map { nif ->
                val addrs = Collections.list(nif.inetAddresses)
                val v4 = addrs.filterIsInstance<Inet4Address>().mapNotNull { it.hostAddress }
                val v6 = addrs.filter { it !is Inet4Address }
                    .mapNotNull { it.hostAddress?.substringBefore('%') }
                LocalNetworkInterfaceSpec(
                    name = nif.name,
                    displayName = nif.displayName ?: nif.name,
                    isUp = nif.isUp,
                    isLoopback = nif.isLoopback,
                    mtu = nif.mtu,
                    ipv4Addresses = v4,
                    ipv6Addresses = v6
                )
            }.sortedByDescending { it.isUp && !it.isLoopback }
        }.getOrDefault(emptyList())

        return ConnectivityDiagnosticsSnapshot(
            isConnected = isConnected,
            transportLabel = transport,
            isMetered = isMetered,
            isVpnActive = isVpn,
            isValidatedInternet = isValidated,
            interfaceName = ifaceName,
            dnsServers = dnsList,
            gatewayAddresses = gateways,
            domains = searchDomains,
            linkDownstreamKbps = downKbps,
            linkUpstreamKbps = upKbps,
            localInterfaces = interfaces
        )
    }

    /**
     * Executes an explicit user-triggered DNS & TCP handshake latency check.
     * Immediately aborts if strictOfflineProfile is enabled.
     */
    suspend fun runExplicitDiagnosticProbe(
        host: String,
        port: Int = 443,
        strictOfflineProfile: Boolean
    ): NetworkProbeResult = withContext(Dispatchers.IO) {
        if (strictOfflineProfile) {
            return@withContext NetworkProbeResult(
                targetHost = host,
                operationType = "BLOCKED_BY_STRICT_OFFLINE_PROFILE",
                isSuccess = false,
                resolvedAddrs = emptyList(),
                roundTripMillis = null,
                diagnosticDetail = "Strict Offline Profile is active. All external network probes are disabled; only device-local interface inspection is permitted."
            )
        }

        val cleanedHost = host.trim().removePrefix("https://").removePrefix("http://").substringBefore("/")
        if (cleanedHost.isBlank()) {
            return@withContext NetworkProbeResult(
                targetHost = host,
                operationType = "VALIDATION_ERROR",
                isSuccess = false,
                resolvedAddrs = emptyList(),
                roundTripMillis = null,
                diagnosticDetail = "Target hostname cannot be empty."
            )
        }

        return@withContext try {
            val t0 = System.nanoTime()
            val resolved = InetAddress.getAllByName(cleanedHost).mapNotNull { it.hostAddress }
            val primaryIp = resolved.firstOrNull() ?: throw IllegalStateException("No IP records returned")

            // Measure Reachability / TCP connect latency without transmitting any application payload
            val socketStart = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(primaryIp, port), 2500)
            }
            val rttMs = (System.nanoTime() - socketStart) / 1_000_000L
            val totalMs = (System.nanoTime() - t0) / 1_000_000L

            NetworkProbeResult(
                targetHost = cleanedHost,
                operationType = "DNS + TCP/$port REACHABILITY",
                isSuccess = true,
                resolvedAddrs = resolved,
                roundTripMillis = rttMs,
                diagnosticDetail = "Resolved ${resolved.size} address(es) and verified TCP/$port handshake in $rttMs ms (total $totalMs ms)."
            )
        } catch (e: Exception) {
            NetworkProbeResult(
                targetHost = cleanedHost,
                operationType = "DNS + TCP/$port REACHABILITY",
                isSuccess = false,
                resolvedAddrs = emptyList(),
                roundTripMillis = null,
                diagnosticDetail = "Probe failed: ${e.javaClass.simpleName} — ${e.message ?: "Unreachable"}"
            )
        }
    }
}
