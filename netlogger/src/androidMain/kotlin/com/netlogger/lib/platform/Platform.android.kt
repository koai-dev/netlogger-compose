package com.netlogger.lib.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
internal actual fun formatTime(timestamp: Long): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
internal actual fun dateLabel(timestamp: Long): String {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val log = Calendar.getInstance().apply { timeInMillis = timestamp }
    fun sameDay(other: Calendar) = log.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        log.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    return when {
        sameDay(today) -> "Today"
        sameDay(yesterday) -> "Yesterday"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
internal actual class ClipboardContext(val context: Context)
@Composable internal actual fun rememberClipboardContext(): ClipboardContext {
    val context = LocalContext.current
    return remember(context) { ClipboardContext(context) }
}
internal actual fun copyToClipboard(context: ClipboardContext, label: String, text: String) {
    if (text.isBlank()) return
    val clipboard = context.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text).apply {
        description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    })
    Toast.makeText(context.context, "$label copied!", Toast.LENGTH_SHORT).show()
}
internal actual fun writeConsole(priority: Int, tag: String, message: String) {
    android.util.Log.println(priority, tag, message)
}

internal actual val supportsAndroidShortcuts: Boolean = true
