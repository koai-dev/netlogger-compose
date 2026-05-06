package com.netlogger.lib.domain.usecase

import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.repository.INetloggerRepository

class SaveApiLogUseCase(private val repository: INetloggerRepository) {
    suspend operator fun invoke(apiLog: LogEntry.Api) {
        repository.saveLog(apiLog)
    }
}
