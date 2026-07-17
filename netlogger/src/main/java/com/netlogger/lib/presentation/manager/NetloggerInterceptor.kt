package com.netlogger.lib.presentation.manager

import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.domain.usecase.GetSettingsUseCase
import com.netlogger.lib.domain.usecase.SaveApiLogUseCase
import com.netlogger.lib.presentation.util.NetloggerConsoleLogger
import com.netlogger.lib.presentation.util.NetloggerRedactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.ForwardingSink
import okio.buffer
import java.io.IOException
import java.nio.charset.StandardCharsets

class NetloggerInterceptor internal constructor(
    private val saveApiLogUseCase: SaveApiLogUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val config: NetloggerConfig,
    private val redactor: NetloggerRedactor,
    initialLogLevel: LogLevel
) : Interceptor {

    @Deprecated(
        message = "Use Netlogger.init(application, config) and Netlogger.getInterceptor()",
        level = DeprecationLevel.WARNING
    )
    constructor(
        saveApiLogUseCase: SaveApiLogUseCase,
        getSettingsUseCase: GetSettingsUseCase
    ) : this(
        saveApiLogUseCase = saveApiLogUseCase,
        getSettingsUseCase = getSettingsUseCase,
        config = NetloggerConfig(),
        redactor = NetloggerRedactor(NetloggerConfig()),
        initialLogLevel = LogLevel.NONE
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val consoleLogger = NetloggerConsoleLogger(config, redactor)
    private val logQueue = Channel<LogEntry.Api>(
        capacity = MAX_PENDING_LOGS,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Volatile
    private var currentLogLevel: LogLevel = initialLogLevel

    init {
        scope.launch {
            for (log in logQueue) {
                runCatching { consoleLogger.logApi(log) }
                runCatching { saveApiLogUseCase(log) }
            }
        }
        scope.launch {
            getSettingsUseCase().collect { settings ->
                currentLogLevel = settings.logLevel
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val level = currentLogLevel
        if (!level.info) return chain.proceed(chain.request())

        val request = chain.request()
        val requestTime = System.currentTimeMillis()
        val requestBodyString = captureRequestBody(request.body, level)

        val response = try {
            chain.proceed(request)
        } catch (exception: Exception) {
            val endTime = System.currentTimeMillis()
            val requestHeaders = if (level.headers) buildAllHeadersJson(request) else null
            val errorBody = if (level.body && config.captureBodies) {
                redactor.redactText(exception.message.orEmpty())
            } else {
                null
            }

            logQueue.trySend(
                LogEntry.Api(
                    tag = "API_ERROR",
                    method = request.method,
                    url = redactor.redactUrl(request.url),
                    requestHeaders = requestHeaders,
                    requestBody = requestBodyString,
                    responseHeaders = null,
                    responseBody = errorBody,
                    statusCode = 0,
                    requestTime = requestTime,
                    responseTime = endTime,
                    totalDuration = endTime - requestTime
                )
            )
            throw exception
        }

        val responseTime = System.currentTimeMillis()
        val sentRequest = response.request
        val sentHeadersJson = if (level.headers) buildAllHeadersJson(sentRequest) else null
        val responseHeadersJson = if (level.headers) buildResponseHeadersJson(response) else null
        val responseBodyString = captureResponseBody(response, level)

        logQueue.trySend(
            LogEntry.Api(
                tag = "API_SUCCESS",
                method = sentRequest.method,
                url = redactor.redactUrl(sentRequest.url),
                requestHeaders = sentHeadersJson,
                requestBody = requestBodyString,
                responseHeaders = responseHeadersJson,
                responseBody = responseBodyString,
                statusCode = response.code,
                requestTime = requestTime,
                responseTime = responseTime,
                totalDuration = responseTime - requestTime
            )
        )

        return response
    }

    internal fun close() {
        logQueue.close()
        scope.cancel()
    }

    private fun captureRequestBody(body: RequestBody?, level: LogLevel): String? {
        if (!level.body || !config.captureBodies || body == null) return null
        if (body.isOneShot()) return OMITTED_ONE_SHOT
        if (body.isDuplex()) return OMITTED_DUPLEX

        val contentType = body.contentType()
        if (!isTextual(contentType)) return OMITTED_NON_TEXT

        val contentLength = runCatching { body.contentLength() }.getOrDefault(-1L)
        if (contentLength > config.maxBodyBytes) return omittedTooLarge(contentLength)
        if (contentLength == 0L) return null

        val buffer = Buffer()
        val limitedSink = object : ForwardingSink(buffer) {
            private var bytesWritten = 0L

            override fun write(source: Buffer, byteCount: Long) {
                if (byteCount > config.maxBodyBytes - bytesWritten) {
                    throw BodyLimitExceededException()
                }
                super.write(source, byteCount)
                bytesWritten += byteCount
            }
        }.buffer()

        return try {
            body.writeTo(limitedSink)
            limitedSink.flush()
            if (buffer.size == 0L) return null
            val charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            redactor.redactBody(buffer.readString(charset), contentType?.toString())
        } catch (_: BodyLimitExceededException) {
            omittedUnknownLengthTooLarge()
        } catch (_: Exception) {
            OMITTED_READ_ERROR
        } finally {
            runCatching { limitedSink.close() }
        }
    }

    private fun captureResponseBody(response: Response, level: LogLevel): String? {
        if (!level.body || !config.captureBodies) return null

        val body = response.body
        val contentType = body.contentType()
        if (!isTextual(contentType)) return OMITTED_NON_TEXT

        val contentLength = body.contentLength()
        if (contentLength > config.maxBodyBytes) return omittedTooLarge(contentLength)
        if (contentLength == 0L) return null

        return runCatching {
            val peekByteCount = if (config.maxBodyBytes == Long.MAX_VALUE) {
                Long.MAX_VALUE
            } else {
                config.maxBodyBytes + 1L
            }
            val peekedBody = response.peekBody(peekByteCount)
            if (peekedBody.contentLength() > config.maxBodyBytes) {
                return@runCatching omittedUnknownLengthTooLarge()
            }
            val raw = peekedBody.string()
            if (raw.isEmpty()) return@runCatching null
            redactor.redactBody(raw, contentType?.toString())
        }.getOrDefault(OMITTED_READ_ERROR)
    }

    private fun buildAllHeadersJson(request: Request): String {
        val headers = mutableListOf<Pair<String, String>>()
        for (index in 0 until request.headers.size) {
            headers += request.headers.name(index) to request.headers.value(index)
        }

        val body = request.body
        if (body != null) {
            if (headers.none { it.first.equals("Content-Type", ignoreCase = true) }) {
                body.contentType()?.let { headers += "Content-Type" to it.toString() }
            }
            if (headers.none { it.first.equals("Content-Length", ignoreCase = true) }) {
                runCatching { body.contentLength() }
                    .getOrNull()
                    ?.takeIf { it >= 0L }
                    ?.let { headers += "Content-Length" to it.toString() }
            }
        }
        if (headers.none { it.first.equals("Host", ignoreCase = true) }) {
            headers += "Host" to request.url.host
        }

        return redactor.headersToJson(headers)
    }

    private fun buildResponseHeadersJson(response: Response): String {
        val headers = buildList {
            for (index in 0 until response.headers.size) {
                add(response.headers.name(index) to response.headers.value(index))
            }
        }
        return redactor.headersToJson(headers)
    }

    private fun isTextual(contentType: MediaType?): Boolean {
        if (contentType == null) return false
        val subtype = contentType.subtype.lowercase()
        return contentType.type.equals("text", ignoreCase = true) ||
            subtype.contains("json") ||
            subtype.contains("xml") ||
            subtype.contains("x-www-form-urlencoded") ||
            subtype.contains("graphql") ||
            subtype.contains("javascript")
    }

    private fun omittedTooLarge(contentLength: Long): String =
        "[OMITTED: body size $contentLength bytes exceeds ${config.maxBodyBytes}-byte limit]"

    private fun omittedUnknownLengthTooLarge(): String =
        "[OMITTED: body exceeds ${config.maxBodyBytes}-byte limit]"

    private class BodyLimitExceededException : IOException()

    private companion object {
        const val OMITTED_ONE_SHOT = "[OMITTED: one-shot request body]"
        const val OMITTED_DUPLEX = "[OMITTED: duplex request body]"
        const val OMITTED_NON_TEXT = "[OMITTED: non-text body]"
        const val OMITTED_READ_ERROR = "[OMITTED: body could not be read safely]"
        const val MAX_PENDING_LOGS = 64
    }
}
