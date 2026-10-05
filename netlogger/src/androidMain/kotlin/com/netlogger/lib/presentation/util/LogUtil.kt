package com.netlogger.lib.presentation.util

import com.netlogger.lib.Netlogger
import com.netlogger.lib.domain.model.LogSeverity

internal actual fun emitGeneralLog(tag: String, message: String, level: LogSeverity) {
    Netlogger.managerOrNull()?.log(tag, message, level)
}
