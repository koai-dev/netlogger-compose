package com.netlogger.lib.domain.usecase

import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.domain.repository.INetloggerRepository

class SaveGeneralLogUseCase(private val repository: INetloggerRepository) {
    suspend operator fun invoke(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val entry = LogEntry.General(
            tag = tag,
            message = message,
            level = level
        )
        repository.saveLog(entry)
    }
}
