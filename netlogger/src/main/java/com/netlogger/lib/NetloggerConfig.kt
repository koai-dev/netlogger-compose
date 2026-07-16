package com.netlogger.lib

import com.netlogger.lib.domain.model.LogLevel

/**
 * Security and resource limits for Netlogger.
 *
 * Defaults are intentionally conservative. An integrating app must explicitly opt in to
 * capturing bodies or persisting logs for a debug/internal environment.
 */
data class NetloggerConfig(
    val maximumLogLevel: LogLevel = LogLevel.NONE,
    val captureBodies: Boolean = false,
    val captureGeneralLogs: Boolean = false,
    val enableLogcatOutput: Boolean = false,
    val storage: NetloggerStorage = NetloggerStorage.MEMORY_ONLY,
    val secureWindow: Boolean = true,
    val maxBodyBytes: Long = DEFAULT_MAX_BODY_BYTES,
    val maxGeneralMessageChars: Int = DEFAULT_MAX_GENERAL_MESSAGE_CHARS,
    val maxLogEntries: Int = DEFAULT_MAX_LOG_ENTRIES,
    val retentionMillis: Long = DEFAULT_RETENTION_MILLIS,
    val allowShakeDetector: Boolean = false,
    val allowFloatingButton: Boolean = false,
    val additionalRedactedHeaders: Set<String> = emptySet(),
    val additionalRedactedQueryParameters: Set<String> = emptySet(),
    val additionalRedactedBodyFields: Set<String> = emptySet()
) {
    init {
        require(maxBodyBytes in 1..MAX_ALLOWED_BODY_BYTES) {
            "maxBodyBytes must be between 1 and $MAX_ALLOWED_BODY_BYTES"
        }
        require(maxGeneralMessageChars in 1..MAX_ALLOWED_GENERAL_MESSAGE_CHARS) {
            "maxGeneralMessageChars must be between 1 and $MAX_ALLOWED_GENERAL_MESSAGE_CHARS"
        }
        require(maxLogEntries in 1..MAX_ALLOWED_LOG_ENTRIES) {
            "maxLogEntries must be between 1 and $MAX_ALLOWED_LOG_ENTRIES"
        }
        require(retentionMillis in 1..MAX_ALLOWED_RETENTION_MILLIS) {
            "retentionMillis must be between 1 and $MAX_ALLOWED_RETENTION_MILLIS"
        }
    }

    companion object {
        const val DEFAULT_MAX_BODY_BYTES = 256L * 1024L
        const val MAX_ALLOWED_BODY_BYTES = 1024L * 1024L
        const val DEFAULT_MAX_GENERAL_MESSAGE_CHARS = 4_096
        const val MAX_ALLOWED_GENERAL_MESSAGE_CHARS = 16_384
        const val DEFAULT_MAX_LOG_ENTRIES = 500
        const val MAX_ALLOWED_LOG_ENTRIES = 5_000
        const val DEFAULT_RETENTION_MILLIS = 24L * 60L * 60L * 1_000L
        const val MAX_ALLOWED_RETENTION_MILLIS = 7L * 24L * 60L * 60L * 1_000L
    }
}

enum class NetloggerStorage {
    /** Logs live only for the lifetime of the application process. */
    MEMORY_ONLY,

    /** Logs are stored under the application's no-backup directory. */
    PERSISTENT_NO_BACKUP
}
