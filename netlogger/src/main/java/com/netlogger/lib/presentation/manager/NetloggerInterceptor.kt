package com.netlogger.lib.presentation.manager

import android.util.Log
import com.google.gson.JsonObject
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.domain.usecase.GetSettingsUseCase
import com.netlogger.lib.domain.usecase.SaveApiLogUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset

class NetloggerInterceptor(
    private val saveApiLogUseCase: SaveApiLogUseCase,
    private val getSettingsUseCase: GetSettingsUseCase
) : Interceptor {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val utf8 = Charset.forName("UTF-8")

    @Volatile
    private var currentLogLevel: LogLevel = LogLevel.ALL

    init {
        scope.launch {
            getSettingsUseCase().collect { settings ->
                currentLogLevel = settings.logLevel
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val level = currentLogLevel
        if (!level.info) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val requestTime = System.currentTimeMillis()

        // Read request body
        var requestBodyString: String? = null
        val requestBody = request.body
        if (level.body && requestBody != null) {
            try {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                requestBodyString = buffer.readString(utf8)
            } catch (e: Exception) {
                requestBodyString = "Error reading request body: ${e.message}"
            }
        }

        // Print request to console
        Log.w("Netlogger", "--> ${request.method} ${request.url}")
        if (level.headers) {
            val headers = request.headers
            for (i in 0 until headers.size) {
                Log.w("Netlogger", "${headers.name(i)}: ${headers.value(i)}")
            }
            requestBody?.contentType()?.let {
                Log.w("Netlogger", "Content-Type: $it")
            }
            try {
                val len = requestBody?.contentLength() ?: -1
                if (len >= 0) {
                    Log.w("Netlogger", "Content-Length: $len")
                }
            } catch (_: Exception) {}
        }
        if (level.body && requestBodyString != null) {
            Log.w("Netlogger", "")
            Log.w("Netlogger", requestBodyString)
        }
        Log.w("Netlogger", "--> END ${request.method}")

        var response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            Log.w("Netlogger", "<-- HTTP FAILED: $e")
            // On error, we only have the original request headers (pre-chain).
            // Build them as JSON for consistent parsing downstream.
            val headersJson = if (level.headers) buildAllHeadersJson(request) else null
            val bodyJson = if (level.body) requestBodyString else null
            val errorBody = if (level.body) (e.message ?: e.toString()) else null
            scope.launch {
                saveApiLogUseCase(
                    LogEntry.Api(
                        tag = "API_ERROR",
                        method = request.method,
                        url = request.url.toString(),
                        requestHeaders = headersJson,
                        requestBody = bodyJson,
                        responseHeaders = null,
                        responseBody = errorBody,
                        statusCode = 0,
                        requestTime = requestTime,
                        responseTime = endTime,
                        totalDuration = endTime - requestTime
                    )
                )
            }
            throw e
        }

        val responseTime = System.currentTimeMillis()
        val responseBody = response.body
        var responseBodyString: String? = null

        if (level.body && responseBody.contentLength() != 0L) {
            try {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE) // Buffer the entire body.
                val buffer = source.buffer
                responseBodyString = buffer.clone().readString(utf8)
            } catch (e: Exception) {
                responseBodyString = "Error reading response body: ${e.message}"
            }
        }

        // Use response.request to capture the FINAL request that was actually sent.
        // This includes all headers added by other interceptors in the chain
        // (e.g. AuthenticationInterceptor, BridgeInterceptor, etc.).
        val sentRequest = response.request
        val sentHeadersJson = if (level.headers) buildAllHeadersJson(sentRequest) else null
        val responseHeadersJson = if (level.headers) buildResponseHeadersJson(response) else null

        // Print response to console
        val duration = responseTime - requestTime
        Log.w("Netlogger", "<-- ${response.code} ${response.message} ${sentRequest.url} (${duration}ms)")
        if (level.headers) {
            val headers = response.headers
            for (i in 0 until headers.size) {
                Log.w("Netlogger", "${headers.name(i)}: ${headers.value(i)}")
            }
        }
        if (level.body && responseBodyString != null) {
            Log.w("Netlogger", "")
            Log.w("Netlogger", responseBodyString)
        }
        Log.w("Netlogger", "<-- END HTTP")

        scope.launch {
            saveApiLogUseCase(
                LogEntry.Api(
                    tag = "API_SUCCESS",
                    method = sentRequest.method,
                    url = sentRequest.url.toString(),
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
        }

        return response
    }

    /**
     * Builds a JSON object containing ALL request headers.
     * Includes:
     *  - All explicit headers from request.headers
     *  - Content-Type from RequestBody (if present and not already in headers)
     *  - Content-Length from RequestBody (if present and not already in headers)
     *  - Host derived from the URL (if not already in headers)
     */
    private fun buildAllHeadersJson(request: Request): String {
        val json = JsonObject()

        // 1. All explicit headers (including ones added by other interceptors)
        val headers = request.headers
        for (i in 0 until headers.size) {
            val name = headers.name(i)
            val value = headers.value(i)
            // OkHttp allows duplicate header names; append with comma for JSON
            if (json.has(name)) {
                val existing = json.get(name).asString
                json.addProperty(name, "$existing, $value")
            } else {
                json.addProperty(name, value)
            }
        }

        // 2. Content-Type from RequestBody (often not in headers for app interceptors)
        val body = request.body
        if (body != null) {
            if (!json.has("Content-Type") && !json.has("content-type")) {
                body.contentType()?.let { mediaType ->
                    json.addProperty("Content-Type", mediaType.toString())
                }
            }
            if (!json.has("Content-Length") && !json.has("content-length")) {
                try {
                    val len = body.contentLength()
                    if (len >= 0) {
                        json.addProperty("Content-Length", len.toString())
                    }
                } catch (_: Exception) { }
            }
        }

        // 3. Host from URL
        if (!json.has("Host") && !json.has("host")) {
            json.addProperty("Host", request.url.host)
        }

        return json.toString()
    }

    /**
     * Builds a JSON object from all response headers.
     */
    private fun buildResponseHeadersJson(response: Response): String {
        val json = JsonObject()
        val headers = response.headers
        for (i in 0 until headers.size) {
            val name = headers.name(i)
            val value = headers.value(i)
            if (json.has(name)) {
                val existing = json.get(name).asString
                json.addProperty(name, "$existing, $value")
            } else {
                json.addProperty(name, value)
            }
        }
        return json.toString()
    }
}
