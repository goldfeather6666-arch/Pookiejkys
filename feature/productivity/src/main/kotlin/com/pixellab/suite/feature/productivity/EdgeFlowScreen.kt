package com.pixellab.suite.feature.productivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixellab.suite.core.database.NoteEntity
import com.pixellab.suite.core.database.SnippetEntity
import com.pixellab.suite.core.designsystem.GlassCard
import com.pixellab.suite.core.designsystem.PixelLabColors
import com.pixellab.suite.core.designsystem.SectionHeader
import com.pixellab.suite.core.designsystem.TelemetryKeyValueRow
import com.pixellab.suite.data.PixelLabRepository
import com.pixellab.suite.domain.QrCodeGeneratorEngine
import com.pixellab.suite.domain.ScientificExpressionEvaluator
import com.pixellab.suite.domain.TextCryptoEngine
import com.pixellab.suite.domain.UnitCategory
import com.pixellab.suite.domain.UnitConversionEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EdgeFlowScreen(repository: PixelLabRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val notes by repository.dao.observeNotes().collectAsState(initial = emptyList())
    val snippets by repository.dao.observeSnippets().collectAsState(initial = emptyList())

    var activeToolTab by remember { mutableIntStateOf(0) }
    // 0: Scientific Calculator, 1: Notes, 2: Clipboard Snippets, 3: Stopwatch/Timer, 4: Unit Converter, 5: QR & Text Utils

    // Calculator state
    var calcExpression by remember { mutableStateOf("sqrt(144) + 2^5") }
    var calcResult by remember { mutableStateOf("44.0") }

    // Notes state
    var noteSearch by remember { mutableStateOf("") }
    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteBody by remember { mutableStateOf("") }

    // Clipboard snippet state
    var newSnippetLabel by remember { mutableStateOf("") }
    var newSnippetText by remember { mutableStateOf("") }

    // Stopwatch state
    var stopwatchRunning by remember { mutableStateOf(false) }
    var stopwatchElapsedMs by remember { mutableLongStateOf(0L) }
    val stopwatchLaps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(stopwatchRunning) {
        var lastTick = System.currentTimeMillis()
        while (isActive && stopwatchRunning) {
            delay(30)
            val now = System.currentTimeMillis()
            stopwatchElapsedMs += (now - lastTick)
            lastTick = now
        }
    }

    // Unit Converter state
    var selectedUnitCategory by remember { mutableStateOf(UnitCategory.LENGTH) }
    var unitInputVal by remember { mutableStateOf("100") }
    var fromUnit by remember { mutableStateOf("cm") }
    var toUnit by remember { mutableStateOf("m") }

    // QR & Text Utils state
    var qrInputText by remember { mutableStateOf("https://github.com/goldfeather6666-arch/Pookiejkys") }
    val qrMatrix = remember(qrInputText) { QrCodeGeneratorEngine.encodeToMatrix(qrInputText) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "MODULE 08 — EDGEFLOW",
                subtitle = "Native Productivity Suite: Scientific Calc, Notes, Snippets, Converter & QR",
                badge = "7 TOOLS"
            )
        }

        // Tool Switcher
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    0 to "Sci Calculator",
                    1 to "Notes (${notes.size})",
                    2 to "Snippets (${snippets.size})",
                    3 to "Stopwatch & Laps",
                    4 to "Unit Converter",
                    5 to "QR & Crypto Utils"
                ).forEach { (idx, label) ->
                    FilterChip(
                        selected = activeToolTab == idx,
                        onClick = { activeToolTab = idx },
                        label = { Text(label) }
                    )
                }
            }
        }

        when (activeToolTab) {
            0 -> {
                item {
                    GlassCard(accentColor = PixelLabColors.CyanPulse) {
                        SectionHeader(title = "Scientific Expression Calculator", badge = "= $calcResult")
                        OutlinedTextField(
                            value = calcExpression,
                            onValueChange = { expr ->
                                calcExpression = expr
                                ScientificExpressionEvaluator.evaluate(expr).onSuccess {
                                    calcResult = String.format(Locale.US, "%.6g", it)
                                }
                            },
                            label = { Text("Expression (supports sin, cos, tan, sqrt, ln, log, ^, !, pi)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("7", "8", "9", "+", "-", "4", "5", "6", "*", "/", "1", "2", "3", "^", "!", "0", ".", "(", ")", "sqrt(", "sin(", "cos(", "ln(", "pi").forEach { token ->
                                OutlinedButton(onClick = {
                                    calcExpression += token
                                    ScientificExpressionEvaluator.evaluate(calcExpression).onSuccess {
                                        calcResult = String.format(Locale.US, "%.6g", it)
                                    }
                                }) {
                                    Text(token)
                                }
                            }
                            Button(onClick = {
                                calcResult = ScientificExpressionEvaluator.evaluate(calcExpression).fold(
                                    onSuccess = { String.format(Locale.US, "%.8g", it) },
                                    onFailure = { "Error: ${it.message}" }
                                )
                            }) {
                                Text("Evaluate =")
                            }
                            OutlinedButton(onClick = {
                                calcExpression = ""
                                calcResult = "0"
                            }) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                        SectionHeader(title = "Local Notes Vault & Instant Search", badge = "${notes.size} NOTES")
                        OutlinedTextField(
                            value = noteSearch,
                            onValueChange = { noteSearch = it },
                            label = { Text("Search local notes...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newNoteTitle,
                            onValueChange = { newNoteTitle = it },
                            label = { Text("New Note Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newNoteBody,
                            onValueChange = { newNoteBody = it },
                            label = { Text("Note Content") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newNoteTitle.isNotBlank() || newNoteBody.isNotBlank()) {
                                    scope.launch {
                                        repository.dao.upsertNote(
                                            NoteEntity(
                                                title = newNoteTitle.ifBlank { "Untitled Note" },
                                                content = newNoteBody
                                            )
                                        )
                                        newNoteTitle = ""
                                        newNoteBody = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Note to Room DB")
                        }
                    }
                }

                val filteredNotes = notes.filter {
                    noteSearch.isBlank() ||
                        it.title.contains(noteSearch, ignoreCase = true) ||
                        it.content.contains(noteSearch, ignoreCase = true)
                }
                filteredNotes.forEach { note ->
                    item(key = "note_${note.id}") {
                        GlassCard(accentColor = PixelLabColors.CyanPulse) {
                            TelemetryKeyValueRow(note.title, if (note.isPinned) "★ PINNED" else note.category)
                            Text(note.content, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    scope.launch { repository.dao.upsertNote(note.copy(isPinned = !note.isPinned)) }
                                }) {
                                    Text(if (note.isPinned) "Unpin" else "Pin")
                                }
                                OutlinedButton(onClick = {
                                    scope.launch { repository.dao.deleteNote(note) }
                                }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                item {
                    GlassCard(accentColor = PixelLabColors.EmeraldNominal) {
                        SectionHeader(title = "Clipboard Snippets Manager", badge = "${snippets.size} SAVED")
                        OutlinedTextField(
                            value = newSnippetLabel,
                            onValueChange = { newSnippetLabel = it },
                            label = { Text("Snippet Label") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newSnippetText,
                            onValueChange = { newSnippetText = it },
                            label = { Text("Snippet Content") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newSnippetText.isNotBlank()) {
                                    scope.launch {
                                        repository.dao.upsertSnippet(
                                            SnippetEntity(
                                                label = newSnippetLabel.ifBlank { "Quick Snippet" },
                                                textContent = newSnippetText
                                            )
                                        )
                                        newSnippetLabel = ""
                                        newSnippetText = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Snippet")
                        }
                    }
                }

                snippets.forEach { snip ->
                    item(key = "snip_${snip.id}") {
                        GlassCard(accentColor = PixelLabColors.CyanPulse) {
                            TelemetryKeyValueRow(snip.label, snip.tag)
                            Text(snip.textContent, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText(snip.label, snip.textContent))
                                }) {
                                    Text("Copy to Clipboard")
                                }
                                OutlinedButton(onClick = {
                                    scope.launch { repository.dao.deleteSnippet(snip) }
                                }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                item {
                    GlassCard(accentColor = PixelLabColors.CyanPulse) {
                        val minutes = (stopwatchElapsedMs / 60000)
                        val seconds = (stopwatchElapsedMs % 60000) / 1000
                        val millis = (stopwatchElapsedMs % 1000)
                        SectionHeader(
                            title = "Precision Stopwatch & Lap Recorder",
                            badge = String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { stopwatchRunning = !stopwatchRunning }) {
                                Text(if (stopwatchRunning) "Pause" else "Start")
                            }
                            OutlinedButton(onClick = {
                                if (stopwatchElapsedMs > 0L) stopwatchLaps.add(0, stopwatchElapsedMs)
                            }) {
                                Text("Lap")
                            }
                            OutlinedButton(onClick = {
                                stopwatchRunning = false
                                stopwatchElapsedMs = 0L
                                stopwatchLaps.clear()
                            }) {
                                Text("Reset")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        stopwatchLaps.forEachIndexed { idx, lapMs ->
                            TelemetryKeyValueRow("Lap ${stopwatchLaps.size - idx}", "${lapMs} ms")
                        }
                    }
                }
            }

            4 -> {
                item {
                    GlassCard(accentColor = PixelLabColors.ElectricViolet) {
                        SectionHeader(title = "Multi-Domain Unit Converter", badge = selectedUnitCategory.displayName)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            UnitCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = selectedUnitCategory == cat,
                                    onClick = {
                                        selectedUnitCategory = cat
                                        fromUnit = cat.units.first()
                                        toUnit = cat.units.last()
                                    },
                                    label = { Text(cat.displayName) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = unitInputVal,
                            onValueChange = { unitInputVal = it },
                            label = { Text("Input Value ($fromUnit)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("From Unit:", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            selectedUnitCategory.units.forEach { u ->
                                FilterChip(selected = fromUnit == u, onClick = { fromUnit = u }, label = { Text(u) })
                            }
                        }
                        Text("To Unit:", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            selectedUnitCategory.units.forEach { u ->
                                FilterChip(selected = toUnit == u, onClick = { toUnit = u }, label = { Text(u) })
                            }
                        }
                        val numericVal = unitInputVal.toDoubleOrNull() ?: 0.0
                        val converted = runCatching {
                            UnitConversionEngine.convert(selectedUnitCategory, numericVal, fromUnit, toUnit)
                        }.getOrDefault(0.0)
                        Spacer(modifier = Modifier.height(8.dp))
                        TelemetryKeyValueRow(
                            label = "$numericVal $fromUnit =",
                            value = String.format(Locale.US, "%.6g %s", converted, toUnit),
                            valueColor = PixelLabColors.EmeraldNominal
                        )
                    }
                }
            }

            5 -> {
                item {
                    GlassCard(accentColor = PixelLabColors.EmeraldNominal) {
                        SectionHeader(
                            title = "Offline QR Code Generator & Crypto Hash Studio",
                            subtitle = "Pure-Kotlin ISO/IEC 18004 QR matrix & SHA-256 / Base64 tools"
                        )
                        OutlinedTextField(
                            value = qrInputText,
                            onValueChange = { qrInputText = it },
                            label = { Text("Payload for QR Code & Hash Utilities") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Canvas(modifier = Modifier.size(190.dp)) {
                                val n = qrMatrix.size
                                val cell = size.width / (n + 4)
                                drawRect(color = Color.White, size = size)
                                for (r in 0 until n) {
                                    for (c in 0 until n) {
                                        if (qrMatrix[r][c]) {
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = Offset((c + 2) * cell, (r + 2) * cell),
                                                size = Size(cell, cell)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        TelemetryKeyValueRow("Character / Byte Count", "${qrInputText.length} chars • ${qrInputText.toByteArray().size} B")
                        TelemetryKeyValueRow("SHA-256 (Prefix)", TextCryptoEngine.sha256Hex(qrInputText).take(28) + "…")
                        TelemetryKeyValueRow("MD5 Digest", TextCryptoEngine.md5Hex(qrInputText))
                        TelemetryKeyValueRow("Base64 Encoded", TextCryptoEngine.base64Encode(qrInputText).take(32) + "…")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
