package com.netlogger.lib.presentation.manager

import com.netlogger.lib.domain.model.LogLevel
import okhttp3.Interceptor

interface INetloggerManager {
    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO)
    fun getInterceptor(): Interceptor
}
