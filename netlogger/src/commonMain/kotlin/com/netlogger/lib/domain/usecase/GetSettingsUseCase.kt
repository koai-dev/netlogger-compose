package com.netlogger.lib.domain.usecase

import com.netlogger.lib.domain.model.LogSettings
import com.netlogger.lib.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<LogSettings> = repository.getSettings()
}
