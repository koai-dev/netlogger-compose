package com.netlogger.lib.presentation.manager

import com.netlogger.lib.domain.model.LogSeverity
import okhttp3.Interceptor

interface INetloggerManager {
    fun log(tag: String, message: String, level: LogSeverity = LogSeverity.INFO)
    fun getInterceptor(): Interceptor
}
