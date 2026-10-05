package com.netlogger.lib.domain.model

import com.netlogger.lib.domain.model.LogSeverity
import com.netlogger.lib.domain.model.LogType

@kotlinx.serialization.Serializable
sealed class LogEntry {
    abstract val id: Long
    abstract val type: LogType
    abstract val timestamp: Long
    abstract val tag: String

    @kotlinx.serialization.Serializable
    data class General(
        override val id: Long = 0,
        override val timestamp: Long = com.netlogger.lib.platform.currentTimeMillis(),
        override val tag: String,
        val message: String,
        val level: LogSeverity = LogSeverity.INFO
    ) : LogEntry() {
        override val type: LogType = LogType.GENERAL
    }

    @kotlinx.serialization.Serializable
    data class Api(
        override val id: Long = 0,
        override val timestamp: Long = com.netlogger.lib.platform.currentTimeMillis(),
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
