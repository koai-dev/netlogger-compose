package com.netlogger.lib.domain.model

import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.domain.model.LogType

sealed class LogEntry {
    abstract val id: Long
    abstract val type: LogType
    abstract val timestamp: Long
    abstract val tag: String

    data class General(
        override val id: Long = 0,
        override val timestamp: Long = System.currentTimeMillis(),
        override val tag: String,
        val message: String,
        val level: LogLevel = LogLevel.INFO
    ) : LogEntry() {
        override val type: LogType = LogType.GENERAL
    }

    data class Api(
        override val id: Long = 0,
        override val timestamp: Long = System.currentTimeMillis(),
        override val tag: String,
        val method: String,
        val url: String,
        val requestHeaders: String?,
        val requestBody: String?,
        val responseHeaders: String?,
        val responseBody: String?,
        val statusCode: Int,
        val requestTime: Long,
        val responseTime: Long,
        val totalDuration: Long
    ) : LogEntry() {
        override val type: LogType = LogType.API
    }
}
