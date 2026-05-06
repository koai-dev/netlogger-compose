package com.netlogger.lib.data.mapper

import com.netlogger.lib.data.source.local.entity.LogEntity
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.domain.model.LogType

fun LogEntry.toEntity(): LogEntity {
    return when (this) {
        is LogEntry.General -> LogEntity(
            id = id,
            type = type.name,
            timestamp = timestamp,
            tag = tag,
            message = message,
            level = level.name,
            method = null, url = null, requestHeaders = null, requestBody = null,
            responseHeaders = null, responseBody = null, statusCode = null,
            requestTime = null, responseTime = null, totalDuration = null
        )

        is LogEntry.Api -> LogEntity(
            id = id,
            type = type.name,
            timestamp = timestamp,
            tag = tag,
            message = null,
            level = null,
            method = method,
            url = url,
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            responseHeaders = responseHeaders,
            responseBody = responseBody,
            statusCode = statusCode,
            requestTime = requestTime,
            responseTime = responseTime,
            totalDuration = totalDuration
        )
    }
}

fun LogEntity.toDomain(): LogEntry {
    return if (type == LogType.GENERAL.name) {
        LogEntry.General(
            id = id,
            timestamp = timestamp,
            tag = tag,
            message = message ?: "",
            level = LogLevel.valueOf(level ?: LogLevel.INFO.name)
        )
    } else {
        LogEntry.Api(
            id = id,
            timestamp = timestamp,
            tag = tag,
            method = method ?: "",
            url = url ?: "",
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            responseHeaders = responseHeaders,
            responseBody = responseBody,
            statusCode = statusCode ?: 0,
            requestTime = requestTime ?: 0L,
            responseTime = responseTime ?: 0L,
            totalDuration = totalDuration ?: 0L
        )
    }
}
