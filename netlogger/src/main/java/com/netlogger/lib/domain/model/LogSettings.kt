package com.netlogger.lib.domain.model

data class LogSettings(
    val autoResetOnStart: Boolean = false,
    val enableShakeDetector: Boolean = true,
    val shakeSensitivity: Float = 2.0f,
    val logLevel: LogLevel = LogLevel.ALL
)
