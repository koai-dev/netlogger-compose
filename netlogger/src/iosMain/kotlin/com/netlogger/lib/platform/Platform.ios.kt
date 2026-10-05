package com.netlogger.lib.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.*
import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardOptionLocalOnly
import platform.UIKit.UIPasteboardOptionExpirationDate

internal actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
private fun date(timestamp: Long) = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
internal actual fun formatTime(timestamp: Long): String = NSDateFormatter().apply {
    dateFormat = "HH:mm:ss"
}.stringFromDate(date(timestamp))
internal actual fun dateLabel(timestamp: Long): String {
    val value = date(timestamp)
    val calendar = NSCalendar.currentCalendar
    return when {
        calendar.isDateInToday(value) -> "Today"
        calendar.isDateInYesterday(value) -> "Yesterday"
        else -> NSDateFormatter().apply { dateFormat = "dd/MM/yyyy" }.stringFromDate(value)
    }
}
internal actual class ClipboardContext
@Composable internal actual fun rememberClipboardContext(): ClipboardContext = remember { ClipboardContext() }
internal actual fun copyToClipboard(context: ClipboardContext, label: String, text: String) {
    if (text.isBlank()) return
    UIPasteboard.generalPasteboard.setItems(
        listOf(mapOf("public.utf8-plain-text" to text)),
        mapOf(UIPasteboardOptionLocalOnly to true, UIPasteboardOptionExpirationDate to NSDate.dateWithTimeIntervalSinceNow(120.0))
    )
}
internal actual fun writeConsole(priority: Int, tag: String, message: String) {
    NSLog("%@", "$tag: $message")
}

internal actual val supportsAndroidShortcuts: Boolean = false
