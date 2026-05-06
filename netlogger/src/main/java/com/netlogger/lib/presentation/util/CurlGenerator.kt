package com.netlogger.lib.presentation.util

import com.google.gson.JsonParser

/**
 * Generates a valid cURL command from API log data.
 * The output can be directly pasted into a terminal or imported into Postman.
 */
object CurlGenerator {

    /**
     * @param method  HTTP method (GET, POST, PUT, DELETE, PATCH…)
     * @param url     Full URL including scheme and query params
     * @param requestHeaders  Raw headers string — either OkHttp's multi-line format ("Key: Value\n")
     *                        or a JSON object ({"Key":"Value"})
     * @param requestBody     Raw request body string (may be JSON or form-encoded)
     */
    fun generate(
        method: String,
        url: String,
        requestHeaders: String?,
        requestBody: String?
    ): String {
        val sb = StringBuilder()
        sb.append("curl -X $method")
        sb.append(" \\\n  '${escapeShell(url)}'")

        // Headers
        if (!requestHeaders.isNullOrBlank()) {
            val parsedHeaders = parseHeaders(requestHeaders)
            for ((key, value) in parsedHeaders) {
                sb.append(" \\\n  -H '${escapeShell(key)}: ${escapeShell(value)}'")
            }
        }

        // Body
        if (!requestBody.isNullOrBlank() && method.uppercase() !in listOf("GET", "HEAD", "OPTIONS")) {
            sb.append(" \\\n  -d '${escapeShell(requestBody)}'")
        }

        return sb.toString()
    }

    /**
     * Parses headers from either JSON format or OkHttp's "Key: Value\n" format.
     */
    private fun parseHeaders(raw: String): List<Pair<String, String>> {
        val headers = mutableListOf<Pair<String, String>>()

        // Try JSON first
        try {
            val trimmed = raw.trim()
            if (trimmed.startsWith("{")) {
                val obj = JsonParser.parseString(trimmed).asJsonObject
                for ((key, value) in obj.entrySet()) {
                    headers.add(key to value.asString)
                }
                return headers
            }
        } catch (_: Exception) { }

        // Fallback: OkHttp multi-line format "Key: Value"
        raw.lines()
            .filter { it.contains(":") }
            .forEach { line ->
                val colonIdx = line.indexOf(':')
                val key = line.substring(0, colonIdx).trim()
                val value = line.substring(colonIdx + 1).trim()
                if (key.isNotBlank()) {
                    headers.add(key to value)
                }
            }

        return headers
    }

    private fun escapeShell(input: String): String {
        return input.replace("'", "'\\''")
    }
}
