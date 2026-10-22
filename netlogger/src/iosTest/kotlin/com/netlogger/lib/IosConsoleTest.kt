package com.netlogger.lib

import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.domain.model.LogSettings
import com.netlogger.lib.domain.model.LogSeverity
import com.netlogger.lib.domain.repository.INetloggerRepository
import com.netlogger.lib.domain.repository.SettingsRepository
import com.netlogger.lib.network.netlogger
import com.netlogger.lib.platform.PlatformLog
import com.netlogger.lib.platform.writeConsole
import com.netlogger.lib.presentation.util.NetloggerConsoleLogger
import com.netlogger.lib.presentation.util.NetloggerRedactor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** These tests must use the real iOS writer: a fake writer cannot catch a native SIGSEGV. */
class IosConsoleTest {
    @Test
    fun nativeWriterHandlesLiteralFormatMarkersAndUnicode() = runBlocking {
        withContext(Dispatchers.Default) {
            for (message in listOf("", "100% %@ %s %d %n", "Tiếng Việt 🌏\nsecond line")) {
                writeConsole(PlatformLog.INFO, "Netlogger-HTTP", message)
            }
        }
    }

    @Test
    fun generalLogsUseTheRealWriterOnABackgroundThread() = runBlocking {
        val config = NetloggerConfig(enableLogcatOutput = true, captureGeneralLogs = true)
        val console = NetloggerConsoleLogger(config, NetloggerRedactor(config))
        withContext(Dispatchers.Default) {
            for (level in LogSeverity.entries) {
                // Includes chunking and format markers in both tag and message.
                console.logGeneral("Probe %@", "Tiếng Việt 🌏 100% %s\n" + "x".repeat(3_600), level)
            }
        }
    }

    @Test
    fun ktorConsoleOutputCompletesBeforeSavingTheCapturedRequest() = runBlocking {
        for (level in listOf(LogLevel.INFO, LogLevel.ALL)) {
            val config = NetloggerConfig(
                maximumLogLevel = level,
                captureBodies = true,
                enableLogcatOutput = true
            )
            val logs = Channel<LogEntry.Api>(Channel.UNLIMITED)
            val repository = object : INetloggerRepository {
                override suspend fun saveLog(log: LogEntry) { logs.send(log as LogEntry.Api) }
                override fun getAllLogs() = flowOf(emptyList<LogEntry>())
                override fun getLogsByType(type: String) = getAllLogs()
                override suspend fun clearLogs() = Unit
            }
            val settings = object : SettingsRepository {
                override fun getCurrentSettings() = LogSettings(logLevel = level)
                override fun getSettings() = flowOf(getCurrentSettings())
                override suspend fun saveSettings(settings: LogSettings) = Unit
            }
            val body = """{"value":"Tiếng Việt 🌏 100% %@ %s","password":"secret"}"""
            val client = HttpClient(MockEngine {
                respond(body, HttpStatusCode.OK, headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.ContentLength to listOf(body.encodeToByteArray().size.toString())
                ))
            }) { netlogger(config, settings, repository) }
            try {
                assertEquals(body, client.get("https://example.test/console?token=secret").bodyAsText())
                // The background queue writes to the native console before saving. Await the save
                // so client.close() cannot cancel logging before the regression is exercised.
                val log = withTimeout(5_000) { logs.receive() }
                assertEquals(200, log.statusCode)
                assertFalse(log.url.contains("secret"))
                if (level == LogLevel.INFO) {
                    assertNull(log.responseHeaders)
                    assertNull(log.responseBody)
                } else {
                    assertTrue(log.responseBody.orEmpty().contains("Tiếng Việt"))
                    assertFalse(log.responseBody.orEmpty().contains("secret"))
                }
            } finally {
                client.close()
                logs.close()
            }
        }
    }
}
