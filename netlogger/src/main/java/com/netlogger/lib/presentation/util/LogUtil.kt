package com.netlogger.lib.presentation.util

import com.netlogger.lib.Netlogger
import com.netlogger.lib.domain.model.LogSeverity
import com.netlogger.lib.presentation.manager.INetloggerManager

object LogUtil {
    private val netloggerManager: INetloggerManager?
        get() = try {
            Netlogger.netloggerManager
        } catch (e: Exception) {
            null
        }

    fun log(tag: String, message: String, level: LogSeverity = LogSeverity.DEBUG) {
        try {
            val manager = netloggerManager
            if (manager != null) {
                manager.log(tag, message, level)
            } else {
                println("Netlogger fallback: [$tag] $message")
            }
        } catch (e: Exception) {
            println("Netlogger fallback: [$tag] $message")
        }
    }

    fun info(tag: String, message: String) = log(tag, message, LogSeverity.INFO)
    fun debug(tag: String, message: String) = log(tag, message, LogSeverity.DEBUG)
    fun warn(tag: String, message: String) = log(tag, message, LogSeverity.WARNING)
    fun error(tag: String, message: String) = log(tag, message, LogSeverity.ERROR)
}
