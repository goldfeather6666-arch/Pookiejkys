package com.pixellab.suite.core.device

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.Display
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class DeviceIdentitySnapshot(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val deviceCodename: String,
    val hardwareName: String,
    val socManufacturer: String,
    val socModel: String,
    val androidRelease: String,
    val sdkInt: Int,
    val securityPatch: String,
    val buildFingerprint: String,
    val bootloader: String,
    val kernelVersion: String,
    val isPixel9ProXl: Boolean
)

data class DisplayTelemetrySnapshot(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val densityScale: Float,
    val physicalDiagonalInches: Double,
    val currentRefreshRateHz: Float,
    val supportedRefreshRatesHz: List<Float>,
    val isHdrCapable: Boolean,
    val isWideColorGamut: Boolean
)

data class CpuMemoryStorageSnapshot(
    val supportedAbis: List<String>,
    val availableProcessors: Int,
    val cpuHardwareHeader: String,
    val coreFrequenciesKhz: List<String>,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val usedRamBytes: Long,
    val ramUsagePercent: Int,
    val isLowMemory: Boolean,
    val totalInternalStorageBytes: Long,
    val availableInternalStorageBytes: Long,
    val usedInternalStorageBytes: Long,
    val storageUsagePercent: Int
)

data class BatteryThermalSnapshot(
    val levelPercent: Int,
    val isCharging: Boolean,
    val statusLabel: String,
    val plugSourceLabel: String,
    val temperatureCelsius: Float,
    val voltageMillivolts: Int,
    val currentNowMicroamps: Long?,
    val currentAvgMicroamps: Long?,
    val chargeCounterMicroampHours: Long?,
    val healthLabel: String,
    val technology: String,
    val thermalStatusCode: Int,
    val thermalStatusLabel: String,
    val cycleCountExposed: Int?
)

data class InstalledAppSummary(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val targetSdk: Int
)

class DeviceHardwareProvider(private val context: Context) {

    fun getDeviceIdentity(): DeviceIdentitySnapshot {
        val socMfg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER.ifBlank { "Google" }
        } else {
            "Unavailable (Requires API 31+)"
        }
        val socMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { "Tensor" }
        } else {
            "Unavailable (Requires API 31+)"
        }
        val patch = Build.VERSION.SECURITY_PATCH.ifBlank { "Unavailable" }
        val kernel = System.getProperty("os.version") ?: "Unavailable"
        val isKomodo = Build.MODEL.contains("Pixel 9 Pro XL", ignoreCase = true) ||
            Build.DEVICE.equals("komodo", ignoreCase = true)

        return DeviceIdentitySnapshot(
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            model = Build.MODEL,
            deviceCodename = Build.DEVICE,
            hardwareName = Build.HARDWARE,
            socManufacturer = socMfg,
            socModel = socMod,
            androidRelease = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = patch,
            buildFingerprint = Build.FINGERPRINT,
            bootloader = Build.BOOTLOADER,
            kernelVersion = kernel,
            isPixel9ProXl = isKomodo
        )
    }

    fun getDisplayTelemetry(): DisplayTelemetrySnapshot {
        val dm: DisplayMetrics = context.resources.displayMetrics
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val defaultDisplay: Display? = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)

        val width = dm.widthPixels
        val height = dm.heightPixels
        val xInches = if (dm.xdpi > 0f) width / dm.xdpi.toDouble() else 0.0
        val yInches = if (dm.ydpi > 0f) height / dm.ydpi.toDouble() else 0.0
        val diag = ((sqrt(xInches * xInches + yInches * yInches) * 100.0).roundToInt()) / 100.0

        val currentHz = defaultDisplay?.refreshRate ?: 60f
        val supportedRates = defaultDisplay?.supportedModes
            ?.map { (it.refreshRate * 10f).roundToInt() / 10f }
            ?.distinct()
            ?.sorted()
            ?: listOf(currentHz)

        val isHdr = defaultDisplay?.isHdr ?: false
        val isWideGamut = defaultDisplay?.isWideColorGamut ?: false

        return DisplayTelemetrySnapshot(
            widthPx = width,
            heightPx = height,
            densityDpi = dm.densityDpi,
            densityScale = dm.density,
            physicalDiagonalInches = diag,
            currentRefreshRateHz = (currentHz * 10f).roundToInt() / 10f,
            supportedRefreshRatesHz = supportedRates,
            isHdrCapable = isHdr,
            isWideColorGamut = isWideGamut
        )
    }

    fun getCpuMemoryStorageTelemetry(): CpuMemoryStorageSnapshot {
        val abis = Build.SUPPORTED_ABIS.toList()
        val cores = Runtime.getRuntime().availableProcessors()

        val cpuHeader = runCatching {
            File("/proc/cpuinfo").takeIf { it.canRead() }
                ?.useLines { lines ->
                    lines.firstOrNull {
                        it.startsWith("Hardware", ignoreCase = true) ||
                            it.startsWith("model name", ignoreCase = true)
                    }?.substringAfter(":")?.trim()
                }
        }.getOrNull() ?: "Restricted by Android SELinux policy"

        val freqs = (0 until cores).map { coreIdx ->
            val curPath = File("/sys/devices/system/cpu/cpu$coreIdx/cpufreq/scaling_cur_freq")
            val maxPath = File("/sys/devices/system/cpu/cpu$coreIdx/cpufreq/cpuinfo_max_freq")
            val curKhz = runCatching { if (curPath.canRead()) curPath.readText().trim().toLongOrNull() else null }.getOrNull()
            val maxKhz = runCatching { if (maxPath.canRead()) maxPath.readText().trim().toLongOrNull() else null }.getOrNull()
            when {
                curKhz != null && maxKhz != null -> "Core $coreIdx: ${curKhz / 1000} / ${maxKhz / 1000} MHz"
                maxKhz != null -> "Core $coreIdx: Max ${maxKhz / 1000} MHz (Live freq OS-restricted)"
                else -> "Core $coreIdx: Frequency telemetry OS-restricted"
            }
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem.coerceAtLeast(1L)
        val availRam = memInfo.availMem.coerceAtLeast(0L)
        val usedRam = (totalRam - availRam).coerceAtLeast(0L)
        val ramPct = ((usedRam.toDouble() / totalRam.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)

        val dataStat = StatFs(Environment.getDataDirectory().path)
        val totalStorage = dataStat.totalBytes.coerceAtLeast(1L)
        val availStorage = dataStat.availableBytes.coerceAtLeast(0L)
        val usedStorage = (totalStorage - availStorage).coerceAtLeast(0L)
        val storagePct = ((usedStorage.toDouble() / totalStorage.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)

        return CpuMemoryStorageSnapshot(
            supportedAbis = abis,
            availableProcessors = cores,
            cpuHardwareHeader = cpuHeader,
            coreFrequenciesKhz = freqs,
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            usedRamBytes = usedRam,
            ramUsagePercent = ramPct,
            isLowMemory = memInfo.lowMemory,
            totalInternalStorageBytes = totalStorage,
            availableInternalStorageBytes = availStorage,
            usedInternalStorageBytes = usedStorage,
            storageUsagePercent = storagePct
        )
    }

    fun getBatteryThermalSnapshot(): BatteryThermalSnapshot {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
        val pct = if (level >= 0) ((level * 100f) / scale).roundToInt().coerceIn(0, 100) else 0

        val statusInt = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
            statusInt == BatteryManager.BATTERY_STATUS_FULL

        val statusLabel = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val pluggedInt = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val plugLabel = when (pluggedInt) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Qi / Pixel Stand Wireless"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
            else -> "On Battery Power"
        }

        val rawTempDeciC = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = rawTempDeciC / 10f
        val rawVoltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        // Normalize if OEM reports microvolts vs millivolts
        val voltageMv = if (rawVoltage > 100_000) rawVoltage / 1000 else rawVoltage

        val healthInt = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val healthLabel = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead / Replace"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown / Unexposed"
        }

        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.ifBlank { "Li-Ion" } ?: "Li-Ion"

        val currentNow = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?.takeIf { it != Long.MIN_VALUE && it != 0L }
        val currentAvg = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            ?.takeIf { it != Long.MIN_VALUE && it != 0L }
        val chargeCounter = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.takeIf { it != Long.MIN_VALUE && it > 0L }

        val cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent?.getIntExtra("android.os.extra.CYCLE_COUNT", -1)?.takeIf { it >= 0 }
        } else {
            null
        }

        val thermalCode = powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
        val thermalLabel = formatThermalStatus(thermalCode)

        return BatteryThermalSnapshot(
            levelPercent = pct,
            isCharging = isCharging,
            statusLabel = statusLabel,
            plugSourceLabel = plugLabel,
            temperatureCelsius = tempC,
            voltageMillivolts = voltageMv,
            currentNowMicroamps = currentNow,
            currentAvgMicroamps = currentAvg,
            chargeCounterMicroampHours = chargeCounter,
            healthLabel = healthLabel,
            technology = tech,
            thermalStatusCode = thermalCode,
            thermalStatusLabel = thermalLabel,
            cycleCountExposed = cycleCount
        )
    }

    fun observeBatteryThermal(): Flow<BatteryThermalSnapshot> = callbackFlow {
        trySend(getBatteryThermalSnapshot())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(getBatteryThermalSnapshot())
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val thermalListener = PowerManager.OnThermalStatusChangedListener {
            trySend(getBatteryThermalSnapshot())
        }
        powerManager?.addThermalStatusListener(context.mainExecutor, thermalListener)

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { powerManager?.removeThermalStatusListener(thermalListener) }
        }
    }

    fun getDeclaredHardwareFeatures(): List<String> {
        return context.packageManager.systemAvailableFeatures
            .mapNotNull { it.name }
            .sorted()
    }

    fun getInstalledAppsInventory(): List<InstalledAppSummary> {
        val pm = context.packageManager
        val packages = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }
        }.getOrDefault(emptyList())

        return packages.mapNotNull { pkg ->
            val appInfo = pkg.applicationInfo ?: return@mapNotNull null
            val label = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(pkg.packageName)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            InstalledAppSummary(
                appName = label,
                packageName = pkg.packageName,
                versionName = pkg.versionName ?: "N/A",
                isSystemApp = isSystem,
                targetSdk = appInfo.targetSdkVersion
            )
        }.sortedBy { it.appName.lowercase() }
    }

    companion object {
        fun formatThermalStatus(code: Int): String = when (code) {
            PowerManager.THERMAL_STATUS_NONE -> "Nominal (None)"
            PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
            PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Load"
            PowerManager.THERMAL_STATUS_SEVERE -> "Severe (Throttling Recommended)"
            PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Thermal Limit"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Thermal State"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown Imminent"
            else -> "Unknown ($code)"
        }
    }
}
