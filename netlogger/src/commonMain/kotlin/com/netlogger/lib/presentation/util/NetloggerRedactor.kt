package com.netlogger.lib.presentation.util

import kotlinx.serialization.json.*
import io.ktor.http.*

import com.netlogger.lib.NetloggerConfig

internal class NetloggerRedactor(config: NetloggerConfig) {

    private val redactedHeaders = normalize(DEFAULT_REDACTED_HEADERS + config.additionalRedactedHeaders)
    private val redactedQueryParameters =
        normalize(DEFAULT_REDACTED_QUERY_PARAMETERS + config.additionalRedactedQueryParameters)
    private val redactedBodyFields =
        normalize(DEFAULT_REDACTED_BODY_FIELDS + config.additionalRedactedBodyFields)

    fun redactUrl(raw: String): String {
        val url = URLBuilder(raw)
        val entries = url.parameters.entries().flatMap { (name, values) -> values.map { name to it } }
        url.parameters.clear()
        if (entries.size > MAX_QUERY_PARAMETERS) {
            url.parameters.append(QUERY_OMITTED_KEY, OMITTED_TOO_MANY_QUERY_PARAMETERS)
        } else {
            entries.forEach { (name, value) ->
                url.parameters.append(name, if (name.lowercase() in redactedQueryParameters) REDACTED else value)
            }
        }
        return url.buildString().take(MAX_URL_CHARS)
    }

    fun headersToJson(headers: List<Pair<String, String>>): String {
        val values = linkedMapOf<String, JsonElement>()
        headers.take(MAX_HEADER_COUNT).forEach { (name, rawValue) ->
            val value = if (name.lowercase() in redactedHeaders) REDACTED
                else redactText(rawValue.take(MAX_HEADER_VALUE_CHARS))
            values[name] = JsonPrimitive(values[name]?.jsonPrimitive?.content?.let { "$it, $value" } ?: value)
        }
        if (headers.size > MAX_HEADER_COUNT) values[HEADERS_OMITTED_KEY] = JsonPrimitive(OMITTED_TOO_MANY_HEADERS)
        val serialized = JsonObject(values).toString()
        return if (serialized.length <= MAX_HEADERS_JSON_CHARS) serialized
            else JsonObject(mapOf(HEADERS_OMITTED_KEY to JsonPrimitive(OMITTED_OVERSIZED_HEADERS))).toString()
    }

    fun redactBody(raw: String, contentType: String?): String {
        val trimmed = raw.trimStart()
        val looksLikeJson = trimmed.startsWith("{") || trimmed.startsWith("[")
        val normalizedContentType = contentType.orEmpty().lowercase()

        if (looksLikeJson || "json" in normalizedContentType) {
            runCatching {
                return redactJson(Json.parseToJsonElement(raw), 0).toString()
            }
        }
        if ("application/x-www-form-urlencoded" in normalizedContentType) {
            return redactForm(raw)
        }
        if ("xml" in normalizedContentType) {
            return redactXml(raw)
        }
        return redactText(raw)
    }

    fun redactText(raw: String): String {
        val trimmed = raw.trimStart()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            runCatching {
                return redactJson(Json.parseToJsonElement(raw), 0).toString()
            }
        }

        var result = raw

        HEADER_VALUE_PATTERN.findAll(result).toList().asReversed().forEach { match ->
            val name = match.groupValues[1]
            if (name.lowercase() in redactedHeaders || name.lowercase() in redactedBodyFields) {
                result = result.replaceRange(
                    match.range,
                    "${match.groupValues[1]}${match.groupValues[2]}$REDACTED"
                )
            }
        }
        result = BEARER_PATTERN.replace(result, "Bearer $REDACTED")
        result = JWT_PATTERN.replace(result, REDACTED)
        return result
    }

    private fun redactJson(element: JsonElement, depth: Int): JsonElement {
        if (depth >= MAX_JSON_DEPTH) return JsonPrimitive(TRUNCATED)

        return when (element) {
            is JsonObject -> JsonObject(element.mapValues { (name, value) ->
                if (name.lowercase() in redactedBodyFields) JsonPrimitive(REDACTED)
                else redactJson(value, depth + 1)
            })
            is JsonArray -> JsonArray(element.map { redactJson(it, depth + 1) })
            is JsonPrimitive -> if (element.isString) JsonPrimitive(redactText(element.content)) else element
        }
    }

    private fun redactForm(raw: String): String = raw.split('&').joinToString("&") { pair ->
        val separator = pair.indexOf('=')
        if (separator < 0) return@joinToString pair

        val encodedName = pair.substring(0, separator)
        val decodedName = runCatching {
            encodedName.decodeURLQueryComponent()
        }.getOrDefault(encodedName)
        if (decodedName.lowercase() in redactedBodyFields) {
            "$encodedName=$REDACTED"
        } else {
            "${pair.substring(0, separator + 1)}${redactText(pair.substring(separator + 1))}"
        }
    }

    private fun redactXml(raw: String): String {
        var result = raw
        redactedBodyFields.forEach { field ->
            val escaped = Regex.escape(field)
            val pattern = Regex(
                "(<(?:[A-Za-z0-9_.-]+:)?$escaped\\b[^>]*>)[\\s\\S]*?(</(?:[A-Za-z0-9_.-]+:)?$escaped\\s*>)",
                RegexOption.IGNORE_CASE
            )
            result = pattern.replace(result) { "${it.groupValues[1]}$REDACTED${it.groupValues[2]}" }
        }
        return redactText(result)
    }

    private fun normalize(values: Set<String>): Set<String> = values
        .asSequence()
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()

    companion object {
        const val REDACTED = "[REDACTED]"
        const val TRUNCATED = "[TRUNCATED]"
        private const val MAX_JSON_DEPTH = 64
        private const val MAX_URL_CHARS = 16_384
        private const val MAX_QUERY_PARAMETERS = 100
        private const val MAX_HEADER_COUNT = 100
        private const val MAX_HEADER_VALUE_CHARS = 4_096
        private const val MAX_HEADERS_JSON_CHARS = 64 * 1_024
        private const val QUERY_OMITTED_KEY = "netlogger_query"
        private const val HEADERS_OMITTED_KEY = "_netlogger"
        private const val OMITTED_TOO_MANY_QUERY_PARAMETERS = "[OMITTED: too many query parameters]"
        private const val OMITTED_TOO_MANY_HEADERS = "[OMITTED: additional headers]"
        private const val OMITTED_OVERSIZED_HEADERS = "[OMITTED: headers exceed safe size limit]"

        private val DEFAULT_REDACTED_HEADERS = setOf(
            "Authorization",
            "Proxy-Authorization",
            "Cookie",
            "Set-Cookie",
            "X-Api-Key",
            "Api-Key",
            "X-Auth-Token",
            "X-Csrf-Token",
            "X-Amz-Security-Token",
            "X-Goog-Api-Key",
            "X-Signature",
            "X-Forwarded-For",
            "X-Real-Ip",
            "X-Device-Id"
        )
        private val DEFAULT_REDACTED_QUERY_PARAMETERS = setOf(
            "access_token",
            "refresh_token",
            "id_token",
            "token",
            "api_key",
            "apikey",
            "key",
            "code",
            "password",
            "passwd",
            "secret",
            "client_secret",
            "secret_key",
            "private_key",
            "credential",
            "x-amz-credential",
            "x-amz-signature",
            "x-amz-security-token",
            "signature",
            "sig",
            "session",
            "email",
            "phone",
            "phone_number",
            "address",
            "full_name",
            "first_name",
            "last_name",
            "date_of_birth",
            "dob",
            "national_id",
            "identity_number",
            "account_number",
            "iban",
            "latitude",
            "longitude",
            "location"
        )
        private val DEFAULT_REDACTED_BODY_FIELDS = DEFAULT_REDACTED_QUERY_PARAMETERS + setOf(
            "authorization",
            "cookie",
            "set-cookie",
            "pin",
            "otp",
            "cvv",
            "cvc",
            "card_number",
            "credit_card",
            "ssn"
        )

        private val HEADER_VALUE_PATTERN = Regex(
            "(?i)\\b([a-z][a-z0-9_-]{1,63})(\\s*[:=]\\s*)(?:Bearer\\s+)?(?:\\\"[^\\\"]*\\\"|'[^']*'|[^,\\r\\n&}]+)"
        )
        private val BEARER_PATTERN = Regex("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]+")
        private val JWT_PATTERN = Regex("\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b")
    }
}
