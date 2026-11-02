package com.pixellab.suite.feature.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.pixellab.suite.core.common.FormatUtils
import com.pixellab.suite.core.database.AutomationLogEntity
import com.pixellab.suite.core.database.AutomationRoutineEntity
import com.pixellab.suite.core.designsystem.GlassCard
import com.pixellab.suite.core.designsystem.PixelLabColors
import com.pixellab.suite.core.designsystem.SectionHeader
import com.pixellab.suite.core.designsystem.TelemetryKeyValueRow
import com.pixellab.suite.data.PixelLabRepository
import com.pixellab.suite.domain.AutomationConditionEvaluator
import com.pixellab.suite.domain.DeviceConditionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object AutopilotActionExecutor {
    private const val CHANNEL_ID = "pixellab_autopilot_alerts"

    suspend fun evaluateAndExecuteRoutine(
        context: Context,
        repository: PixelLabRepository,
        routine: AutomationRoutineEntity,
        triggerSource: String,
        bypassConditions: Boolean = false
    ): String {
        val battery = repository.hardwareProvider.getBatteryThermalSnapshot()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val conditionCtx = DeviceConditionContext(
            batteryLevel = battery.levelPercent,
            isCharging = battery.isCharging,
            hourOfDay = hour,
            batteryTempC = battery.temperatureCelsius,
            thermalStatusCode = battery.thermalStatusCode
        )

        val clauses = AutomationConditionEvaluator.parseClauses(routine.conditionsJson)
        val conditionsMet = bypassConditions || AutomationConditionEvaluator.evaluateAll(
            logicMode = routine.conditionLogic,
            clauses = clauses,
            ctx = conditionCtx
        )

        if (!conditionsMet) {
            val skippedMsg = "Skipped: Conditions (${routine.conditionLogic}) not satisfied at Battery=${battery.levelPercent}%, Temp=${battery.temperatureCelsius}°C."
            repository.dao.insertAutomationLog(
                AutomationLogEntity(
                    routineId = routine.id,
                    routineName = routine.name,
                    timestamp = System.currentTimeMillis(),
                    triggerSource = triggerSource,
                    outcome = "CONDITION_FALSE",
                    diagnosticMessage = skippedMsg
                )
            )
            return skippedMsg
        }

        val actionSummary = when (routine.actionType) {
            "NOTIFICATION", "REMINDER" -> {
                postLocalNotification(context, routine.name, routine.actionPayload)
                "Dispatched local notification: '${routine.actionPayload}'"
            }
            "OPEN_SETTINGS" -> {
                runCatching {
                    val settingsIntent = Intent(routine.actionPayload.ifBlank { Settings.ACTION_SETTINGS }).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    "Opened Android Settings intent (${routine.actionPayload})"
                }.getOrElse { "Settings intent unavailable: ${it.message}" }
            }
            "SAVE_LOG" -> {
                "Saved telemetry checkpoint: Battery=${battery.levelPercent}%, Thermal=${battery.thermalStatusLabel}"
            }
            else -> {
                "Executed internal PIXEL LAB routine action (${routine.actionType})"
            }
        }

        val now = System.currentTimeMillis()
        repository.dao.upsertRoutine(
            routine.copy(
                lastExecutedAt = now,
                lastResultStatus = "SUCCESS ($triggerSource)",
                executionCount = routine.executionCount + 1
            )
        )
        repository.dao.insertAutomationLog(
            AutomationLogEntity(
                routineId = routine.id,
                routineName = routine.name,
                timestamp = now,
                triggerSource = triggerSource,
                outcome = "EXECUTED",
                diagnosticMessage = actionSummary
            )
        )
        repository.recordActivity("automation", "Executed ${routine.name}", actionSummary)
        return actionSummary
    }

    private fun postLocalNotification(context: Context, title: String, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PIXEL LAB Autopilot Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Autopilot: $title")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % 100000).toInt(), notif)
    }
}

class AutopilotSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = PixelLabRepository(context.applicationContext)
                val enabled = repo.dao.getEnabledRoutines()
                enabled.forEach { routine ->
                    val shouldEvaluate = when (action) {
                        Intent.ACTION_BOOT_COMPLETED -> routine.triggerType == "BOOT"
                        Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED -> routine.triggerType == "CHARGING_STATE"
                        Intent.ACTION_BATTERY_LOW -> routine.triggerType == "BATTERY_BELOW"
                        else -> false
                    }
                    if (shouldEvaluate) {
                        AutopilotActionExecutor.evaluateAndExecuteRoutine(
                            context = context,
                            repository = repo,
                            routine = routine,
                            triggerSource = action.substringAfterLast('.')
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutopilotScreen(repository: PixelLabRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val routines by repository.dao.observeRoutines().collectAsState(initial = emptyList())
    val logs by repository.dao.observeAutomationLogs().collectAsState(initial = emptyList())

    val canExactAlarm = remember { repository.permissionCoordinator.canScheduleExactAlarms() }
    val ignoringBatteryOpt = remember { repository.permissionCoordinator.isIgnoringBatteryOptimizations() }

    var showCreator by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var selectedTrigger by remember { mutableStateOf("BATTERY_BELOW") }
    var selectedLogic by remember { mutableStateOf("AND") }
    var selectedCondition by remember { mutableStateOf("BATTERY_LEVEL|<=|25") }
    var selectedAction by remember { mutableStateOf("NOTIFICATION") }
    var actionPayload by remember { mutableStateOf("Pixel 9 Pro XL automation triggered.") }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "MODULE 02 — AUTOPILOT",
                subtitle = "Offline Automation Engine with AND/OR Rule Evaluation",
                badge = "${routines.count { it.isEnabled }} ENABLED"
            )
        }

        // Android Background Execution & Permission Diagnostics
        item {
            GlassCard(accentColor = PixelLabColors.CyanPulse) {
                SectionHeader(
                    title = "Android 15 Scheduling & Permission Diagnostics",
                    subtitle = "Respects OS background limits, exact alarm policies & Doze mode"
                )
                TelemetryKeyValueRow(
                    "Exact Alarm Capability",
                    if (canExactAlarm) "GRANTED" else "RESTRICTED (Deferrable WorkManager Fallback)",
                    valueColor = if (canExactAlarm) PixelLabColors.EmeraldNominal else PixelLabColors.AmberCaution
                )
                TelemetryKeyValueRow(
                    "Battery Optimization Exemption",
                    if (ignoringBatteryOpt) "UNRESTRICTED" else "OPTIMIZED (Standard Doze Rules Apply)",
                    valueColor = if (ignoringBatteryOpt) PixelLabColors.EmeraldNominal else PixelLabColors.CyanPulse
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
                    }) {
                        Text("Battery Optimization Settings")
                    }
                    Button(onClick = { showCreator = !showCreator }) {
                        Text(if (showCreator) "Close Builder" else "+ New Routine")
                    }
                }
            }
        }

        // Visual Routine Builder
        if (showCreator) {
            item {
                GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                    SectionHeader(title = "Visual Automation Builder", badge = "NEW RULE")
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Routine Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Trigger Type", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("BATTERY_BELOW", "CHARGING_STATE", "TIME_SCHEDULE", "BOOT", "MANUAL").forEach { trig ->
                            FilterChip(
                                selected = selectedTrigger == trig,
                                onClick = { selectedTrigger = trig },
                                label = { Text(trig) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("2. Condition Logic & Threshold", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("AND", "OR").forEach { logic ->
                            FilterChip(
                                selected = selectedLogic == logic,
                                onClick = { selectedLogic = logic },
                                label = { Text("$logic Logic") }
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "BATTERY_LEVEL|<=|25",
                            "BATTERY_LEVEL|>=|80",
                            "BATTERY_TEMP_C|>=|40.0",
                            "IS_CHARGING|==|true",
                            "IS_CHARGING|==|false"
                        ).forEach { cond ->
                            FilterChip(
                                selected = selectedCondition == cond,
                                onClick = { selectedCondition = cond },
                                label = { Text(cond.replace("|", " ")) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("3. Action Type & Payload", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NOTIFICATION", "SAVE_LOG", "OPEN_SETTINGS").forEach { act ->
                            FilterChip(
                                selected = selectedAction == act,
                                onClick = { selectedAction = act },
                                label = { Text(act) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = actionPayload,
                        onValueChange = { actionPayload = it },
                        label = { Text("Notification Message or Settings Action") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                scope.launch {
                                    repository.dao.upsertRoutine(
                                        AutomationRoutineEntity(
                                            name = newName.trim(),
                                            description = newDesc.ifBlank { "Custom user automation rule" },
                                            triggerType = selectedTrigger,
                                            triggerParameter = selectedCondition,
                                            conditionLogic = selectedLogic,
                                            conditionsJson = selectedCondition,
                                            actionType = selectedAction,
                                            actionPayload = actionPayload,
                                            isEnabled = true
                                        )
                                    )
                                    repository.recordActivity("automation", "Created Routine", newName.trim())
                                    newName = ""
                                    newDesc = ""
                                    showCreator = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Automation Routine")
                    }
                }
            }
        }

        if (statusFeedback != null) {
            item {
                GlassCard(accentColor = PixelLabColors.EmeraldNominal) {
                    Text(
                        text = statusFeedback!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelLabColors.EmeraldNominal
                    )
                }
            }
        }

        // Saved Routines List
        items(routines, key = { it.id }) { routine ->
            GlassCard(
                accentColor = if (routine.isEnabled) PixelLabColors.CyanPulse else PixelLabColors.TextMuted
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = routine.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = routine.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = routine.isEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                repository.dao.upsertRoutine(routine.copy(isEnabled = enabled))
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                TelemetryKeyValueRow("Trigger / Logic", "${routine.triggerType} (${routine.conditionLogic})")
                TelemetryKeyValueRow("Condition Clause", routine.conditionsJson.replace("|", " "))
                TelemetryKeyValueRow("Action", "${routine.actionType}: ${routine.actionPayload.take(36)}")
                TelemetryKeyValueRow("Executions / Status", "${routine.executionCount} runs • ${routine.lastResultStatus}")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            statusFeedback = AutopilotActionExecutor.evaluateAndExecuteRoutine(
                                context = context,
                                repository = repository,
                                routine = routine,
                                triggerSource = "MANUAL_TEST",
                                bypassConditions = false
                            )
                        }
                    }) {
                        Text("Evaluate & Run")
                    }
                    OutlinedButton(onClick = {
                        scope.launch {
                            statusFeedback = AutopilotActionExecutor.evaluateAndExecuteRoutine(
                                context = context,
                                repository = repository,
                                routine = routine,
                                triggerSource = "FORCE_RUN",
                                bypassConditions = true
                            )
                        }
                    }) {
                        Text("Force Action")
                    }
                    OutlinedButton(onClick = {
                        scope.launch {
                            repository.dao.upsertRoutine(
                                routine.copy(id = 0, name = "${routine.name} (Copy)", executionCount = 0)
                            )
                        }
                    }) {
                        Text("Duplicate")
                    }
                    OutlinedButton(onClick = {
                        scope.launch { repository.dao.deleteRoutine(routine) }
                    }) {
                        Text("Delete")
                    }
                }
            }
        }

        // Execution History Log
        item {
            GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                SectionHeader(
                    title = "Execution & Condition Evaluation History",
                    badge = "${logs.size} LOGS"
                )
                if (logs.isEmpty()) {
                    Text(
                        text = "No automation runs logged yet. Tap 'Evaluate & Run' or 'Force Action' on any routine above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    logs.take(15).forEach { log ->
                        TelemetryKeyValueRow(
                            label = "${log.routineName} [${log.outcome}]",
                            value = FormatUtils.formatShortTime(log.timestamp)
                        )
                        Text(
                            text = log.diagnosticMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
