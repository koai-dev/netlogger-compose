package com.netlogger.lib.presentation.manager

import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.domain.model.LogSeverity
import com.netlogger.lib.domain.usecase.SaveGeneralLogUseCase
import com.netlogger.lib.presentation.util.NetloggerConsoleLogger
import com.netlogger.lib.presentation.util.NetloggerRedactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Interceptor

class NetloggerManagerImpl internal constructor(
    private val saveGeneralLogUseCase: SaveGeneralLogUseCase,
    private val interceptor: NetloggerInterceptor,
    private val redactor: NetloggerRedactor,
    private val maxGeneralMessageChars: Int,
    private val captureGeneralLogs: Boolean,
    private val consoleLogger: NetloggerConsoleLogger
) : INetloggerManager {

    @Deprecated(
        message = "Use Netlogger.init(application, config)",
        level = DeprecationLevel.WARNING
    )
    constructor(
        saveGeneralLogUseCase: SaveGeneralLogUseCase,
        interceptor: NetloggerInterceptor
    ) : this(
        saveGeneralLogUseCase = saveGeneralLogUseCase,
        interceptor = interceptor,
        redactor = NetloggerRedactor(NetloggerConfig()),
        maxGeneralMessageChars = NetloggerConfig.DEFAULT_MAX_GENERAL_MESSAGE_CHARS,
        captureGeneralLogs = false,
        consoleLogger = NetloggerConsoleLogger(
            config = NetloggerConfig(),
            redactor = NetloggerRedactor(NetloggerConfig())
        )
    )
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logQueue = Channel<GeneralLog>(
        capacity = MAX_PENDING_LOGS,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launch {
            for (log in logQueue) {
                runCatching { consoleLogger.logGeneral(log.tag, log.message, log.level) }
                runCatching { saveGeneralLogUseCase(log.tag, log.message, log.level) }
            }
        }
    }

    override fun log(tag: String, message: String, level: LogSeverity) {
        if (!captureGeneralLogs) return
        val sanitizedTag = redactor.redactText(tag.take(MAX_TAG_CHARS)).take(MAX_TAG_CHARS)
        val sanitizedMessage = redactor.redactText(message.take(maxGeneralMessageChars))
            .take(maxGeneralMessageChars)
        logQueue.trySend(GeneralLog(sanitizedTag, sanitizedMessage, level))
    }

    override fun getInterceptor(): Interceptor {
        return interceptor
    }

    internal fun close() {
        logQueue.close()
        scope.cancel()
    }

    private companion object {
        const val MAX_TAG_CHARS = 128
        const val MAX_PENDING_LOGS = 64
    }

    private data class GeneralLog(
        val tag: String,
        val message: String,
        val level: LogSeverity
    )
}
