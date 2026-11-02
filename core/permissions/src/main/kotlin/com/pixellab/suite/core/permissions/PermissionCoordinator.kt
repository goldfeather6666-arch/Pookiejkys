package com.pixellab.suite.core.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

data class PermissionAuditItem(
    val permissionId: String,
    val displayName: String,
    val moduleOwner: String,
    val purposeExplanation: String,
    val isGranted: Boolean,
    val isOptional: Boolean = true
)

class PermissionCoordinator(private val context: Context) {

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun canScheduleExactAlarms(): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun auditPermissions(): List<PermissionAuditItem> {
        val items = mutableListOf<PermissionAuditItem>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            items += PermissionAuditItem(
                permissionId = Manifest.permission.POST_NOTIFICATIONS,
                displayName = "Notifications (POST_NOTIFICATIONS)",
                moduleOwner = "Autopilot & Energix",
                purposeExplanation = "Displays local automation alerts, timer completions, and battery threshold warnings.",
                isGranted = isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
            )
        }

        items += PermissionAuditItem(
            permissionId = Manifest.permission.ACTIVITY_RECOGNITION,
            displayName = "Activity Recognition",
            moduleOwner = "Sensorium",
            purposeExplanation = "Required by Android 10+ only if reading Step Counter or Step Detector hardware sensors.",
            isGranted = isPermissionGranted(Manifest.permission.ACTIVITY_RECOGNITION)
        )

        items += PermissionAuditItem(
            permissionId = Manifest.permission.RECORD_AUDIO,
            displayName = "Microphone (Audio-Reactive Studio)",
            moduleOwner = "MotionX",
            purposeExplanation = "Optional opt-in FFT amplitude analysis for live sound-reactive 3D shaders. Never active in background.",
            isGranted = isPermissionGranted(Manifest.permission.RECORD_AUDIO)
        )

        items += PermissionAuditItem(
            permissionId = Manifest.permission.CAMERA,
            displayName = "Camera (Local QR Scanner)",
            moduleOwner = "EdgeFlow",
            purposeExplanation = "Optional on-device QR code scanning without cloud barcode services.",
            isGranted = isPermissionGranted(Manifest.permission.CAMERA)
        )

        items += PermissionAuditItem(
            permissionId = "SCHEDULE_EXACT_ALARM",
            displayName = "Exact Alarms Capability",
            moduleOwner = "Autopilot",
            purposeExplanation = "Allows time-triggered automations to fire at a precise minute window.",
            isGranted = canScheduleExactAlarms()
        )

        return items
    }
}
