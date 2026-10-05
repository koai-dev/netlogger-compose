package com.netlogger.lib.presentation.util

import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogSeverity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetloggerConsoleLoggerTest {

    @Test
    fun `console output is disabled by default`() {
        val writes = mutableListOf<String>()
        val config = NetloggerConfig()
        val logger = NetloggerConsoleLogger(config, NetloggerRedactor(config)) { _, _, message ->
            writes += message
        }

        logger.logGeneral("test", "message", LogSeverity.INFO)

        assertTrue(writes.isEmpty())
    }

    @Test
    fun `console redacts every HTTP section again before writing`() {
        val writes = mutableListOf<String>()
        val config = NetloggerConfig(enableLogcatOutput = true)
        val logger = NetloggerConsoleLogger(config, NetloggerRedactor(config)) { _, _, message ->
            writes += message
        }
        val rawLog = LogEntry.Api(
            tag = "API_SUCCESS",
            method = "POST",
            url = "https://example.test/items?token=query-secret&q=visible",
            requestHeaders = """{"Authorization":"Bearer header-secret"}""",
            requestBody = """{"password":"request-body-secret","value":"ok"}""",
            responseHeaders = """{"Set-Cookie":"session=response-secret"}""",
            responseBody = """{"access_token":"response-body-secret","result":"ok"}""",
            statusCode = 200,
            requestTime = 1L,
            responseTime = 2L,
            totalDuration = 1L
        )

        logger.logApi(rawLog)

        val output = writes.joinToString("")
        listOf(
            "query-secret",
            "header-secret",
            "request-body-secret",
            "response-secret",
            "response-body-secret"
        ).forEach { assertFalse(output.contains(it)) }
        assertTrue(output.contains(NetloggerRedactor.REDACTED))
        assertTrue(output.contains("q=visible"))
    }

    @Test
    fun `console chunks long redacted messages without truncating captured content`() {
        val writes = mutableListOf<String>()
        val config = NetloggerConfig(
            enableLogcatOutput = true,
            maxGeneralMessageChars = 8_000
        )
        val logger = NetloggerConsoleLogger(config, NetloggerRedactor(config)) { _, _, message ->
            writes += message
        }
        val message = "x".repeat(8_000)

        logger.logGeneral("test", message, LogSeverity.DEBUG)

        assertTrue(writes.size > 1)
        assertTrue(writes.all { it.length <= 3_500 })
        assertTrue(writes.joinToString("").endsWith(message))
    }
}
