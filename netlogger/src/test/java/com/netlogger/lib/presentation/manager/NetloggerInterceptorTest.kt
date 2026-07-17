package com.netlogger.lib.presentation.manager

import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.domain.model.LogSettings
import com.netlogger.lib.domain.repository.INetloggerRepository
import com.netlogger.lib.domain.repository.SettingsRepository
import com.netlogger.lib.domain.usecase.GetSettingsUseCase
import com.netlogger.lib.domain.usecase.SaveApiLogUseCase
import com.netlogger.lib.presentation.util.NetloggerRedactor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.BufferedSink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NetloggerInterceptorTest {

    @Test
    fun `redacts secrets from every captured HTTP section`() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("Set-Cookie", "session=response-secret")
                .setBody("""{"access_token":"response-body-secret","result":"ok"}""")
        )
        server.start()

        try {
            val repository = CapturingRepository()
            val client = client(repository, maxBodyBytes = 4_096L)
            val request = Request.Builder()
                .url(server.url("/redact?token=query-secret&q=visible"))
                .header("Authorization", "Bearer header-secret")
                .post(
                    """{"password":"request-body-secret","value":"ok"}"""
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            client.newCall(request).execute().use { it.body.string() }

            assertTrue(repository.awaitLog())
            val captured = repository.lastApiLog.toString()
            listOf(
                "query-secret",
                "header-secret",
                "request-body-secret",
                "response-secret",
                "response-body-secret"
            ).forEach { assertFalse(captured.contains(it)) }
            assertTrue(captured.contains(NetloggerRedactor.REDACTED))
            assertTrue(captured.contains("q=visible"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `oversized response is omitted without consuming application body`() {
        val responseBody = "x".repeat(1_024)
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/plain")
                .setBody(responseBody)
        )
        server.start()

        try {
            val repository = CapturingRepository()
            val client = client(repository, maxBodyBytes = 64L)
            val request = Request.Builder().url(server.url("/large")).build()

            val bodySeenByApplication = client.newCall(request).execute().use { it.body.string() }

            assertEquals(responseBody, bodySeenByApplication)
            assertTrue(repository.awaitLog())
            assertTrue(repository.lastApiLog?.responseBody.orEmpty().contains("exceeds 64-byte limit"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `chunked response with unknown length is captured without consuming application body`() {
        val responseBody = """{"result":"chunked"}"""
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setChunkedBody(responseBody, 3)
        )
        server.start()

        try {
            val repository = CapturingRepository()
            val client = client(repository, maxBodyBytes = 64L)
            val request = Request.Builder().url(server.url("/chunked")).build()

            val bodySeenByApplication = client.newCall(request).execute().use { it.body.string() }

            assertEquals(responseBody, bodySeenByApplication)
            assertTrue(repository.awaitLog())
            assertEquals(responseBody, repository.lastApiLog?.responseBody)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `repeatable request body with unknown length is captured`() {
        val requestJson = """{"value":"unknown-length"}"""
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{}")
        )
        server.start()

        try {
            val unknownLengthBody = object : RequestBody() {
                override fun contentType() = "application/json".toMediaType()
                override fun contentLength() = -1L
                override fun writeTo(sink: BufferedSink) {
                    sink.writeUtf8(requestJson)
                }
            }
            val repository = CapturingRepository()
            val client = client(repository, maxBodyBytes = 64L)
            val request = Request.Builder().url(server.url("/unknown-length")).post(unknownLengthBody).build()

            client.newCall(request).execute().use { it.body.string() }

            assertTrue(repository.awaitLog())
            assertEquals(requestJson, repository.lastApiLog?.requestBody)
            assertEquals(requestJson, server.takeRequest().body.readUtf8())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `oversized request body with unknown length is omitted but still sent`() {
        val requestText = "x".repeat(1_024)
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{}")
        )
        server.start()

        try {
            val unknownLengthBody = object : RequestBody() {
                override fun contentType() = "text/plain".toMediaType()
                override fun contentLength() = -1L
                override fun writeTo(sink: BufferedSink) {
                    sink.writeUtf8(requestText)
                }
            }
            val repository = CapturingRepository()
            val client = client(repository, maxBodyBytes = 64L)
            val request = Request.Builder().url(server.url("/large-unknown-length")).post(unknownLengthBody).build()

            client.newCall(request).execute().use { it.body.string() }

            assertTrue(repository.awaitLog())
            assertEquals(
                "[OMITTED: body exceeds 64-byte limit]",
                repository.lastApiLog?.requestBody
            )
            assertEquals(requestText, server.takeRequest().body.readUtf8())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `oversized chunked response is omitted without consuming application body`() {
        val responseBody = "x".repeat(1_024)
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/plain")
                .setChunkedBody(responseBody, 16)
        )
        server.start()

        try {
            val repository = CapturingRepository()
            val client = client(repository, maxBodyBytes = 64L)
            val request = Request.Builder().url(server.url("/large-chunked")).build()

            val bodySeenByApplication = client.newCall(request).execute().use { it.body.string() }

            assertEquals(responseBody, bodySeenByApplication)
            assertTrue(repository.awaitLog())
            assertEquals(
                "[OMITTED: body exceeds 64-byte limit]",
                repository.lastApiLog?.responseBody
            )
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `one shot request body is never read twice`() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{}")
        )
        server.start()

        try {
            val writes = AtomicInteger(0)
            val oneShotBody = object : RequestBody() {
                override fun contentType() = "application/json".toMediaType()
                override fun contentLength() = 2L
                override fun isOneShot() = true
                override fun writeTo(sink: BufferedSink) {
                    writes.incrementAndGet()
                    sink.writeUtf8("{}")
                }
            }
            val repository = CapturingRepository()
            val client = client(repository, maxBodyBytes = 64L)
            val request = Request.Builder().url(server.url("/one-shot")).post(oneShotBody).build()

            client.newCall(request).execute().use { it.body.string() }

            assertTrue(repository.awaitLog())
            assertEquals(1, writes.get())
            assertEquals("[OMITTED: one-shot request body]", repository.lastApiLog?.requestBody)
        } finally {
            server.shutdown()
        }
    }

    private fun client(repository: CapturingRepository, maxBodyBytes: Long): OkHttpClient {
        val config = NetloggerConfig(
            maximumLogLevel = LogLevel.ALL,
            captureBodies = true,
            maxBodyBytes = maxBodyBytes
        )
        val settingsRepository = FixedSettingsRepository(LogSettings(logLevel = LogLevel.ALL))
        val interceptor = NetloggerInterceptor(
            saveApiLogUseCase = SaveApiLogUseCase(repository),
            getSettingsUseCase = GetSettingsUseCase(settingsRepository),
            config = config,
            redactor = NetloggerRedactor(config),
            initialLogLevel = LogLevel.ALL
        )
        return OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    private class CapturingRepository : INetloggerRepository {
        private val latch = CountDownLatch(1)

        @Volatile
        var lastApiLog: LogEntry.Api? = null
            private set

        override suspend fun saveLog(log: LogEntry) {
            if (log is LogEntry.Api) {
                lastApiLog = log
                latch.countDown()
            }
        }

        fun awaitLog(): Boolean = latch.await(2, TimeUnit.SECONDS)

        override fun getAllLogs(): Flow<List<LogEntry>> = flowOf(emptyList())
        override fun getLogsByType(type: String): Flow<List<LogEntry>> = flowOf(emptyList())
        override suspend fun clearLogs() = Unit
    }

    private class FixedSettingsRepository(
        private val settings: LogSettings
    ) : SettingsRepository {
        override fun getSettings(): Flow<LogSettings> = flowOf(settings)
        override fun getCurrentSettings(): LogSettings = settings
        override suspend fun saveSettings(settings: LogSettings) = Unit
    }
}
