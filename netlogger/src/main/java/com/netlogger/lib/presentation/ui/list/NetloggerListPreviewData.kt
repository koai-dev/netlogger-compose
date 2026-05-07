package com.netlogger.lib.presentation.ui.list

import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogLevel

internal enum class NetloggerFilter(val title: String, val queryValue: String?) {
    ALL("All", "ALL"),
    API("API", "API"),
    GENERAL("General", "GENERAL"),
    ERROR("Error", "ERROR")
}

internal fun sampleLogListItems(): List<LogListItem> = listOf(
    LogListItem.DateHeader("Today"),
    LogListItem.LogItem(sampleApiLog(statusCode = 200, method = "GET", url = "https://api.github.com", duration = 120, offset = 0)),
    LogListItem.LogItem(sampleApiLog(statusCode = 404, method = "POST", url = "https://api.example.com", duration = 85, offset = 53_000)),
    LogListItem.LogItem(
        LogEntry.General(
            tag = "AuthModule",
            message = "User session validated successfully",
            level = LogLevel.DEBUG,
            timestamp = System.currentTimeMillis() - 70_000
        )
    ),
    LogListItem.LogItem(
        LogEntry.General(
            tag = "Database",
            message = "Failed to fetch user profile",
            level = LogLevel.ERROR,
            timestamp = System.currentTimeMillis() - 130_000
        )
    ),
    LogListItem.DateHeader("Yesterday"),
    LogListItem.LogItem(sampleApiLog(statusCode = 200, method = "GET", url = "https://api.weather.com", duration = 210, offset = 86_400_000))
)

private fun sampleApiLog(statusCode: Int, method: String, url: String, duration: Long, offset: Long) = LogEntry.Api(
    tag = "API",
    method = method,
    url = url,
    requestHeaders = null,
    requestBody = null,
    responseHeaders = null,
    responseBody = "{}",
    statusCode = statusCode,
    requestTime = 0L,
    responseTime = duration,
    totalDuration = duration,
    timestamp = System.currentTimeMillis() - offset
)
