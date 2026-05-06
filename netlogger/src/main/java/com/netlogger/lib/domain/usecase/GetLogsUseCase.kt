package com.netlogger.lib.domain.usecase

import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.repository.INetloggerRepository
import kotlinx.coroutines.flow.Flow

class GetLogsUseCase(private val repository: INetloggerRepository) {
    operator fun invoke(type: String? = null): Flow<List<LogEntry>> {
        return if (type == null) {
            repository.getAllLogs()
        } else {
            repository.getLogsByType(type)
        }
    }
}
