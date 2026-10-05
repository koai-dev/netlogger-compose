package com.netlogger.lib.platform

import androidx.compose.runtime.Composable
import com.netlogger.lib.domain.model.LogEntry
import kotlinx.serialization.json.Json

internal expect fun currentTimeMillis(): Long
internal expect fun formatTime(timestamp: Long): String
internal expect fun dateLabel(timestamp: Long): String
internal expect class ClipboardContext
@Composable internal expect fun rememberClipboardContext(): ClipboardContext
internal expect fun copyToClipboard(context: ClipboardContext, label: String, text: String)

internal object PlatformLog {
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6
    fun println(priority: Int, tag: String, message: String) = writeConsole(priority, tag, message)
}
internal expect fun writeConsole(priority: Int, tag: String, message: String)

internal fun encodeLog(log: LogEntry): String = when (log) {
    is LogEntry.Api -> Json.encodeToString(log)
    is LogEntry.General -> Json.encodeToString(log)
}

internal expect val supportsAndroidShortcuts: Boolean
