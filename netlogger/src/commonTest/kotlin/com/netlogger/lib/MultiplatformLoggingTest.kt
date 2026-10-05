package com.netlogger.lib

import com.netlogger.lib.domain.model.*
import com.netlogger.lib.domain.repository.*
import com.netlogger.lib.network.netlogger
import com.netlogger.lib.presentation.util.NetloggerRedactor
import com.netlogger.lib.presentation.util.CurlGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.*
import kotlin.test.*

class MultiplatformLoggingTest {
    @Test fun nestedRedactionAndCurlWorkOnBothPlatforms() {
        val redactor = NetloggerRedactor(NetloggerConfig(additionalRedactedBodyFields = setOf("employee_id")))
        val body = redactor.redactBody("""{"user":{"password":"secret","employee_id":"42","name":"Ada"}}""", "application/json")
        assertFalse(body.contains("secret"))
        assertFalse(body.contains("42"))
        assertTrue(body.contains("Ada"))
        val url = redactor.redactUrl("https://example.test/items?token=secret&q=visible")
        assertFalse(url.contains("secret"))
        assertTrue(url.contains("visible"))
        val curl = CurlGenerator.generate("POST", url, """{"X-Test":"it's safe"}""", body)
        assertTrue(curl.contains("'\\''"))
        assertFalse(curl.contains("secret"))
    }

    @Test fun logSerializationPreservesConcreteSubtypes() {
        val log = LogEntry.General(timestamp = 123, tag = "Demo", message = "Unicode: Việt Nam", level = LogSeverity.WARNING)
        assertEquals(log, Json.decodeFromString<LogEntry.General>(Json.encodeToString(log)))
    }

    @Test fun ktorCaptureRedactsAndPreservesResponse() = runBlocking {
        val body = """{"password":"secret","value":"ok"}"""
        val (client, logs) = client(body, body.length.toLong())
        try {
            val response = client.get("https://example.test/?token=secret&q=visible") {
                header(HttpHeaders.Authorization, "Bearer credential")
            }
            assertEquals(body, response.bodyAsText())
            val log = withTimeout(5_000) { logs.receive() }
            assertFalse(log.url.contains("secret"))
            assertFalse(log.requestHeaders.orEmpty().contains("credential"))
            assertFalse(log.responseBody.orEmpty().contains("secret"))
            assertTrue(log.responseBody.orEmpty().contains("ok"))
            assertEquals(200, log.statusCode)
        } finally { client.close() }
    }

    @Test fun oversizedAndUnknownLengthResponsesAreNotCaptured() = runBlocking {
        for (length in listOf<Long?>(1_024, null)) {
            val body = "x".repeat(1_024)
            val (client, logs) = client(body, length, maxBytes = 64)
            try {
                assertEquals(body, client.get("https://example.test/").bodyAsText())
                assertNull(withTimeout(5_000) { logs.receive() }.responseBody)
            } finally { client.close() }
        }
    }

    @Test fun incorrectContentLengthCannotBypassBodyLimit() = runBlocking {
        val body = "x".repeat(1_024)
        val (client, logs) = client(body, length = 1, maxBytes = 64)
        try {
            // Streaming avoids Ktor's own content-length consistency check for saved responses.
            client.prepareGet("https://example.test/").execute { response ->
                assertEquals(body, response.bodyAsText())
            }
            assertNull(withTimeout(5_000) { logs.receive() }.responseBody)
        } finally { client.close() }
    }

    @Test fun requestBodiesAreRedactedWithinApprovedCaps() = runBlocking {
        val (client, logs) = client("ok", 2)
        try {
            client.post("https://example.test/") {
                setBody(io.ktor.http.content.TextContent("""{"password":"secret","value":"visible"}""", ContentType.Application.Json))
            }.bodyAsText()
            val log = withTimeout(5_000) { logs.receive() }
            assertFalse(log.requestBody.orEmpty().contains("secret"))
            assertTrue(log.requestBody.orEmpty().contains("visible"))
        } finally { client.close() }
    }

    @Test fun configuredCapsOverridePermissiveSavedSettings() = runBlocking {
        val (client, logs) = client("ok", 2, config = NetloggerConfig(maximumLogLevel = LogLevel.INFO))
        try {
            client.post("https://example.test/") {
                header(HttpHeaders.Authorization, "secret")
                setBody(io.ktor.http.content.TextContent("secret", ContentType.Text.Plain))
            }.bodyAsText()
            val log = withTimeout(5_000) { logs.receive() }
            assertNull(log.requestHeaders)
            assertNull(log.responseHeaders)
            assertNull(log.requestBody)
            assertNull(log.responseBody)
        } finally { client.close() }
    }

    @Test fun disabledLoggingDoesNotSaveRequests() = runBlocking {
        val (client, logs) = client("ok", 2, config = NetloggerConfig())
        try {
            assertEquals("ok", client.get("https://example.test/").bodyAsText())
            assertTrue(logs.tryReceive().isFailure)
        } finally { client.close() }
    }

    private fun client(body: String, length: Long?, maxBytes: Long = 256 * 1024,
        config: NetloggerConfig = NetloggerConfig(maximumLogLevel = LogLevel.ALL, captureBodies = true, maxBodyBytes = maxBytes)
    ): Pair<HttpClient, Channel<LogEntry.Api>> {
        val logs = Channel<LogEntry.Api>(Channel.UNLIMITED)
        val repository = object : INetloggerRepository {
            override suspend fun saveLog(log: LogEntry) { logs.send(log as LogEntry.Api) }
            override fun getAllLogs() = flowOf(emptyList<LogEntry>())
            override fun getLogsByType(type: String) = getAllLogs()
            override suspend fun clearLogs() = Unit
        }
        val settings = object : SettingsRepository {
            override fun getCurrentSettings() = LogSettings(logLevel = LogLevel.ALL)
            override fun getSettings() = flowOf(getCurrentSettings())
            override suspend fun saveSettings(settings: LogSettings) = Unit
        }
        return HttpClient(MockEngine { respond(body, HttpStatusCode.OK, headersOf(
            *buildList {
                add(HttpHeaders.ContentType to listOf("application/json"))
                length?.let { add(HttpHeaders.ContentLength to listOf(it.toString())) }
            }.toTypedArray()
        )) }) { netlogger(config, settings, repository) } to logs
    }
}
