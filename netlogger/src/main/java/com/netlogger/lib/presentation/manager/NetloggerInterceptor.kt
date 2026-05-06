package com.netlogger.lib.presentation.manager

import com.google.gson.JsonObject
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.usecase.SaveApiLogUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset

class NetloggerInterceptor(
    private val saveApiLogUseCase: SaveApiLogUseCase
) : Interceptor {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val utf8 = Charset.forName("UTF-8")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestTime = System.currentTimeMillis()

        // Read request body
        var requestBodyString: String? = null
        val requestBody = request.body
        if (requestBody != null) {
            try {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                requestBodyString = buffer.readString(utf8)
            } catch (e: Exception) {
                requestBodyString = "Error reading request body: ${e.message}"
            }
        }

        var response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            // On error, we only have the original request headers (pre-chain).
            // Build them as JSON for consistent parsing downstream.
            val headersJson = buildAllHeadersJson(request)
            scope.launch {
                saveApiLogUseCase(
                    LogEntry.Api(
                        tag = "API_ERROR",
                        method = request.method,
                        url = request.url.toString(),
                        requestHeaders = headersJson,
                        requestBody = requestBodyString,
                        responseHeaders = null,
                        responseBody = e.message ?: e.toString(),
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

        if (responseBody.contentLength() != 0L) {
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
        val sentHeadersJson = buildAllHeadersJson(sentRequest)

        scope.launch {
            saveApiLogUseCase(
                LogEntry.Api(
                    tag = "API_SUCCESS",
                    method = sentRequest.method,
                    url = sentRequest.url.toString(),
                    requestHeaders = sentHeadersJson,
                    requestBody = requestBodyString,
                    responseHeaders = buildResponseHeadersJson(response),
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
