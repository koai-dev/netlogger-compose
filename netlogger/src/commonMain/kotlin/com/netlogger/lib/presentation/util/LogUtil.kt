package com.netlogger.lib.presentation.util

import com.netlogger.lib.domain.model.LogSeverity

/** Manual logging API usable from commonMain on Android and iOS. */
object LogUtil {
    fun log(tag: String, message: String, level: LogSeverity = LogSeverity.DEBUG) = emitGeneralLog(tag, message, level)
    fun info(tag: String, message: String) = log(tag, message, LogSeverity.INFO)
    fun debug(tag: String, message: String) = log(tag, message, LogSeverity.DEBUG)
    fun warn(tag: String, message: String) = log(tag, message, LogSeverity.WARNING)
    fun error(tag: String, message: String) = log(tag, message, LogSeverity.ERROR)
}

internal expect fun emitGeneralLog(tag: String, message: String, level: LogSeverity)
