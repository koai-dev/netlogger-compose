package com.netlogger.lib.presentation.util

import com.google.gson.JsonParser
import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.NetloggerStorage
import com.netlogger.lib.domain.model.LogLevel
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NetloggerRedactorTest {

    private val redactor = NetloggerRedactor(NetloggerConfig())

    @Test
    fun `redacts sensitive headers case insensitively`() {
        val json = JsonParser.parseString(
            redactor.headersToJson(
                listOf(
                    "authorization" to "Bearer top-secret",
                    "Cookie" to "session=abc",
                    "Content-Type" to "application/json"
                )
            )
        ).asJsonObject

        assertEquals(NetloggerRedactor.REDACTED, json.get("authorization").asString)
        assertEquals(NetloggerRedactor.REDACTED, json.get("Cookie").asString)
        assertEquals("application/json", json.get("Content-Type").asString)
    }

    @Test
    fun `redacts sensitive query parameters without changing safe values`() {
        val output = redactor.redactUrl(
            "https://example.test/items?token=secret&q=visible&API_KEY=private".toHttpUrl()
        ).toHttpUrl()

        assertEquals(NetloggerRedactor.REDACTED, output.queryParameter("token"))
        assertEquals(NetloggerRedactor.REDACTED, output.queryParameter("API_KEY"))
        assertEquals("visible", output.queryParameter("q"))
    }

    @Test
    fun `omits abusive header and query collections`() {
        val headers = (0..100).map { "X-Test-$it" to "safe" }
        val headerJson = JsonParser.parseString(redactor.headersToJson(headers)).asJsonObject
        val query = (0..100).joinToString("&") { "value$it=secret-$it" }

        val outputUrl = redactor.redactUrl("https://example.test/items?$query".toHttpUrl())

        assertTrue(headerJson.has("_netlogger"))
        assertFalse(outputUrl.contains("secret-100"))
        assertTrue(outputUrl.contains("netlogger_query"))
    }

    @Test
    fun `redacts nested json fields and bearer tokens`() {
        val output = JsonParser.parseString(
            redactor.redactBody(
                """{"user":{"password":"secret","name":"Ada"},"items":[{"access_token":"abc"}],"note":"Bearer token-value"}""",
                "application/json"
            )
        ).asJsonObject

        assertEquals(NetloggerRedactor.REDACTED, output.getAsJsonObject("user").get("password").asString)
        assertEquals("Ada", output.getAsJsonObject("user").get("name").asString)
        assertEquals(
            NetloggerRedactor.REDACTED,
            output.getAsJsonArray("items")[0].asJsonObject.get("access_token").asString
        )
        assertFalse(output.get("note").asString.contains("token-value"))
    }

    @Test
    fun `redacts form and configured custom fields`() {
        val customRedactor = NetloggerRedactor(
            NetloggerConfig(additionalRedactedBodyFields = setOf("employee_id"))
        )

        val output = customRedactor.redactBody(
            "employee_id=123&name=Ada&password=secret",
            "application/x-www-form-urlencoded"
        )

        assertTrue(output.contains("employee_id=${NetloggerRedactor.REDACTED}"))
        assertTrue(output.contains("password=${NetloggerRedactor.REDACTED}"))
        assertTrue(output.contains("name=Ada"))
    }

    @Test
    fun `redacts sensitive general json logs`() {
        val output = redactor.redactText("""{"refresh_token":"secret","result":"ok"}""")

        assertFalse(output.contains("secret"))
        assertTrue(output.contains(NetloggerRedactor.REDACTED))
    }

    @Test
    fun `rejects unsafe resource limits`() {
        assertThrows(IllegalArgumentException::class.java) {
            NetloggerConfig(maxBodyBytes = NetloggerConfig.MAX_ALLOWED_BODY_BYTES + 1L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NetloggerConfig(maxLogEntries = 0)
        }
    }

    @Test
    fun `security sensitive features are opt in`() {
        val config = NetloggerConfig()

        assertEquals(LogLevel.NONE, config.maximumLogLevel)
        assertFalse(config.captureBodies)
        assertFalse(config.captureGeneralLogs)
        assertFalse(config.enableLogcatOutput)
        assertFalse(config.allowShakeDetector)
        assertFalse(config.allowFloatingButton)
        assertTrue(config.secureWindow)
        assertEquals(NetloggerStorage.MEMORY_ONLY, config.storage)
    }
}
