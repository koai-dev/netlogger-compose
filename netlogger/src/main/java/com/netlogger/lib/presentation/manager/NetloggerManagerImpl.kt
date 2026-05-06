package com.netlogger.lib.presentation.manager

import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.domain.usecase.SaveGeneralLogUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor

class NetloggerManagerImpl(
    private val saveGeneralLogUseCase: SaveGeneralLogUseCase,
    private val interceptor: NetloggerInterceptor
) : INetloggerManager {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun log(tag: String, message: String, level: LogLevel) {
        scope.launch {
            saveGeneralLogUseCase(tag, message, level)
        }
    }

    override fun getInterceptor(): Interceptor {
        return interceptor
    }
}
