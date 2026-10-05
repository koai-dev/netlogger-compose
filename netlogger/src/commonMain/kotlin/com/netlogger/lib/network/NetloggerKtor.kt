@file:OptIn(io.ktor.utils.io.InternalAPI::class)

package com.netlogger.lib.network

import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.repository.INetloggerRepository
import com.netlogger.lib.domain.repository.SettingsRepository
import com.netlogger.lib.platform.currentTimeMillis
import com.netlogger.lib.presentation.util.NetloggerConsoleLogger
import com.netlogger.lib.presentation.util.NetloggerRedactor
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.replaceResponse
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.io.readByteArray

/** Installs bounded, redacted capture for any Ktor engine, including Darwin on iOS. */
fun HttpClientConfig<*>.netlogger(
    config: NetloggerConfig,
    settings: SettingsRepository,
    repository: INetloggerRepository
) {
    val redactor = NetloggerRedactor(config)
    val console = NetloggerConsoleLogger(config, redactor)
    val plugin = createClientPlugin("Netlogger") {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val queue = Channel<LogEntry.Api>(64, BufferOverflow.DROP_OLDEST)
        scope.launch {
            for (log in queue) {
                runCatching { console.logApi(log) }
                runCatching { repository.saveLog(log) }
            }
        }
        onClose { queue.close(); scope.cancel() }
        on(Send) { request ->
            val requested = settings.getCurrentSettings().logLevel
            val info = requested.info && config.maximumLogLevel.info
            val headers = requested.headers && config.maximumLogLevel.headers
            val bodies = requested.body && config.maximumLogLevel.body && config.captureBodies
            if (!info) return@on proceed(request)
            val start = currentTimeMillis()
            val requestBody = (request.body as? TextContent)?.takeIf {
                bodies && it.text.length.toLong() <= config.maxBodyBytes && it.text.encodeToByteArray().size <= config.maxBodyBytes && isText(it.contentType.toString())
            }?.let { redactor.redactBody(it.text, it.contentType.toString()) }
            val call = try {
                proceed(request)
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                val end = currentTimeMillis()
                queue.trySend(LogEntry.Api(
                    tag = request.url.host, method = request.method.value,
                    url = redactor.redactUrl(request.url.buildString()),
                    requestHeaders = if (headers) redactor.headersToJson(request.headers.entries().flatMap { (k, v) -> v.map { k to it } }) else null,
                    requestBody = requestBody, responseHeaders = null,
                    responseBody = if (bodies) redactor.redactText(error.message.orEmpty().take(config.maxGeneralMessageChars)).take(config.maxGeneralMessageChars) else null,
                    statusCode = 0, requestTime = start, responseTime = end, totalDuration = (end - start).coerceAtLeast(0)
                ))
                throw error
            }
            val response = call.response
            val end = currentTimeMillis()
            var result = call
            var responseBody: String? = null
            val length = response.contentLength()
            val type = response.contentType()?.toString()
            if (bodies && length != null && length in 1..config.maxBodyBytes && isText(type) &&
                response.headers[HttpHeaders.ContentEncoding].let { it == null || it == "identity" }) {
                val original = response.rawContent
                val prefix = original.readBuffer(config.maxBodyBytes + 1).readByteArray()
                val restored = if (original.isClosedForRead) ByteReadChannel(prefix) else
                    writer(response.coroutineContext) { channel.writeFully(prefix); original.copyTo(channel) }.channel
                result = call.replaceResponse { restored }
                if (prefix.size <= config.maxBodyBytes) responseBody = redactor.redactBody(prefix.decodeToString(), type)
            }
            queue.trySend(LogEntry.Api(
                tag = response.request.url.host, method = response.request.method.value,
                url = redactor.redactUrl(response.request.url.toString()),
                requestHeaders = if (headers) redactor.headersToJson(response.request.headers.entries().flatMap { (k, v) -> v.map { k to it } }) else null,
                requestBody = requestBody,
                responseHeaders = if (headers) redactor.headersToJson(response.headers.entries().flatMap { (k, v) -> v.map { k to it } }) else null,
                responseBody = responseBody, statusCode = response.status.value,
                requestTime = start, responseTime = end, totalDuration = (end - start).coerceAtLeast(0)
            ))
            result
        }
    }
    install(plugin)
}

private fun isText(type: String?): Boolean = type != null &&
    (type.startsWith("text/") || listOf("json", "xml", "x-www-form-urlencoded").any { it in type.lowercase() })
