package com.pixellab.suite.core.common

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val unit = 1024.0
        val exp = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceIn(0, 4)
        val pre = arrayOf("B", "KB", "MB", "GB", "TB")[exp]
        return String.format(Locale.US, "%.2f %s", bytes / unit.pow(exp.toDouble()), pre)
    }

    fun formatTimestamp(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return sdf.format(Date(epochMillis))
    }

    fun formatShortTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        return sdf.format(Date(epochMillis))
    }

    fun escapeCsvField(raw: String): String {
        val needsQuotes = raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r")
        return if (needsQuotes) {
            "\"" + raw.replace("\"", "\"\"") + "\""
        } else {
            raw
        }
    }
}

object ExportManager {
    fun writeExportFile(context: Context, fileName: String, content: String): File {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val target = File(exportDir, fileName)
        target.writeText(content, Charsets.UTF_8)
        return target
    }

    fun listExportedFiles(context: Context): List<File> {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) return emptyList()
        return exportDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun shareExportedFile(context: Context, file: File, mimeType: String = "text/plain") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export ${file.name}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
