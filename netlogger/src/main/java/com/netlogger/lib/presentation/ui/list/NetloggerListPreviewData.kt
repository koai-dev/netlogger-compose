package com.netlogger.lib.presentation.ui.list

import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogSeverity

internal data class NetloggerFilter(
    val title: String,
    val queryValue: String? = null,
    val tag: String? = null
) {
    companion object {
        val ALL = NetloggerFilter("All", queryValue = "ALL")
        val API = NetloggerFilter("API", queryValue = "API")
        val GENERAL = NetloggerFilter("General", queryValue = "GENERAL")
        val ERROR = NetloggerFilter("Error", queryValue = "ERROR")

        val defaultFilters = listOf(ALL, API, GENERAL, ERROR)

        fun forTag(tag: String) = NetloggerFilter(title = tag, tag = tag)
    }
}

internal fun sampleLogListItems(): List<LogListItem> = listOf(
    LogListItem.DateHeader("Today"),
    LogListItem.LogItem(
        sampleApiLog(
            statusCode = 200,
            method = "GET",
            url = "https://api.github.com/mobile/v1/accounts/current/profile/settings?expand=permissions,teams&locale=vi_VN",
            duration = 120,
            offset = 0
        )
    ),
    LogListItem.LogItem(
        sampleApiLog(
            statusCode = 404,
            method = "POST",
            url = "https://api.example.com/auth/session/refresh/token",
            duration = 85,
            offset = 53_000
        )
    ),
    LogListItem.LogItem(
        LogEntry.General(
            tag = "AuthModule",
            message = "User session validated successfully",
            level = LogSeverity.DEBUG,
            timestamp = System.currentTimeMillis() - 70_000
        )
    ),
    LogListItem.LogItem(
        LogEntry.General(
            tag = "Database",
            message = "Failed to fetch user profile",
            level = LogSeverity.ERROR,
            timestamp = System.currentTimeMillis() - 130_000
        )
    ),
    LogListItem.DateHeader("Yesterday"),
    LogListItem.LogItem(
        sampleApiLog(
            statusCode = 200,
            method = "GET",
            url = "https://api.weather.com/v3/weather/forecast/daily/10day",
            duration = 210,
            offset = 86_400_000
        )
    )
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
