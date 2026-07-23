package com.netlogger.lib.domain.repository

import com.netlogger.lib.domain.model.LogSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<LogSettings>
    fun getCurrentSettings(): LogSettings = LogSettings()
    suspend fun saveSettings(settings: LogSettings)
    fun getFilterTabOrder(): List<String> = emptyList()
    fun saveFilterTabOrder(tabIds: List<String>) = Unit
}
