package com.netlogger.lib.presentation.ui.list

import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogSeverity
import org.junit.Assert.assertEquals
import org.junit.Test

class NetloggerListViewModelTest {

    @Test
    fun `tag filter returns only general logs with the exact tag`() {
        val authLog = generalLog(tag = "Auth")
        val otherGeneralLog = generalLog(tag = "Database")
        val apiLogWithSameTag = apiLog(tag = "Auth")

        val result = filterByQuickFilter(
            logs = listOf(authLog, otherGeneralLog, apiLogWithSameTag),
            type = null,
            tag = "Auth"
        )

        assertEquals(listOf(authLog), result)
    }

    @Test
    fun `tag filter does not conflict with a built in filter name`() {
        val generalApiTag = generalLog(tag = "API")
        val apiLog = apiLog(tag = "API")

        val result = filterByQuickFilter(
            logs = listOf(generalApiTag, apiLog),
            type = null,
            tag = "API"
        )

        assertEquals(listOf(generalApiTag), result)
    }

    private fun generalLog(tag: String) = LogEntry.General(
        tag = tag,
        message = "message",
        level = LogSeverity.INFO
    )

    private fun apiLog(tag: String) = LogEntry.Api(
        tag = tag,
        method = "GET",
        url = "https://example.test",
        requestHeaders = null,
        requestBody = null,
        responseHeaders = null,
        responseBody = null,
        statusCode = 200,
        requestTime = 0,
        responseTime = 1,
        totalDuration = 1
    )
}
