package com.netlogger.lib.presentation.util

import com.netlogger.lib.platform.PlatformLog
import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogSeverity

/**
 * Optional Logcat output for approved non-production environments.
 *
 * Every value is redacted again here as a defense-in-depth measure. Callers must never pass
 * original OkHttp request or response objects to this class.
 */
internal class NetloggerConsoleLogger(
    private val config: NetloggerConfig,
    private val redactor: NetloggerRedactor,
    private val writer: (priority: Int, tag: String, message: String) -> Unit =
        { priority, tag, message -> PlatformLog.println(priority, tag, message) }
) {

    fun logApi(log: LogEntry.Api) {
        if (!config.enableLogcatOutput) return

        val safeLog = log.copy(
            tag = redactor.redactText(log.tag),
            method = redactor.redactText(log.method),
            url = runCatching { redactor.redactUrl(log.url) }.getOrElse { redactor.redactText(log.url) },
            requestHeaders = log.requestHeaders?.let(redactor::redactText),
            requestBody = log.requestBody?.let(redactor::redactText),
            responseHeaders = log.responseHeaders?.let(redactor::redactText),
            responseBody = log.responseBody?.let(redactor::redactText)
        )
        val priority = when {
            safeLog.statusCode == 0 || safeLog.statusCode >= 500 -> PlatformLog.ERROR
            safeLog.statusCode >= 400 -> PlatformLog.WARN
            else -> PlatformLog.DEBUG
        }
        writeChunked(priority, HTTP_TAG, formatApi(safeLog))
    }

    fun logGeneral(tag: String, message: String, level: LogSeverity) {
        if (!config.enableLogcatOutput) return

        val safeTag = redactor.redactText(tag.take(MAX_GENERAL_TAG_CHARS))
            .take(MAX_GENERAL_TAG_CHARS)
        val safeMessage = redactor.redactText(message.take(config.maxGeneralMessageChars))
            .take(config.maxGeneralMessageChars)
        val priority = when (level) {
            LogSeverity.DEBUG -> PlatformLog.DEBUG
            LogSeverity.INFO -> PlatformLog.INFO
            LogSeverity.WARNING -> PlatformLog.WARN
            LogSeverity.ERROR -> PlatformLog.ERROR
        }
        writeChunked(priority, GENERAL_TAG, "[$safeTag] $safeMessage")
    }

    private fun formatApi(log: LogEntry.Api): String = buildString {
        append("--> ").append(log.method).append(' ').append(log.url)
        appendSection("Request headers", log.requestHeaders)
        appendSection("Request body", log.requestBody)
        append("\n<-- ").append(log.statusCode).append(" (")
            .append(log.totalDuration).append(" ms)")
        appendSection("Response headers", log.responseHeaders)
        appendSection("Response body", log.responseBody)
    }

    private fun StringBuilder.appendSection(label: String, value: String?) {
        if (value != null) append('\n').append(label).append(": ").append(value)
    }

    private fun writeChunked(priority: Int, tag: String, message: String) {
        message.chunked(MAX_LOGCAT_CHUNK_CHARS).forEach { chunk ->
            writer(priority, tag, chunk)
        }
    }

    private companion object {
        const val HTTP_TAG = "Netlogger-HTTP"
        const val GENERAL_TAG = "Netlogger-General"
        const val MAX_LOGCAT_CHUNK_CHARS = 3_500
        const val MAX_GENERAL_TAG_CHARS = 128
    }
}
