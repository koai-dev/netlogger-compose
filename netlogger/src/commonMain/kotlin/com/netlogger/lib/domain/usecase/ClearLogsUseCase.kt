package com.netlogger.lib.domain.usecase

import com.netlogger.lib.domain.repository.INetloggerRepository

class ClearLogsUseCase(private val repository: INetloggerRepository) {
    suspend operator fun invoke() {
        repository.clearLogs()
    }
}
