package com.netlogger.lib.domain.usecase

import com.netlogger.lib.domain.model.LogSettings
import com.netlogger.lib.domain.repository.SettingsRepository

class SaveSettingsUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(settings: LogSettings) = repository.saveSettings(settings)
}
