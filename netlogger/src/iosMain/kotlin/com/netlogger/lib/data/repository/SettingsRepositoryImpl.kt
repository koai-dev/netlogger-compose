package com.netlogger.lib.data.repository

import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.domain.model.LogSettings
import com.netlogger.lib.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

internal class SettingsRepositoryImpl(private val config: NetloggerConfig) : SettingsRepository {
    private val prefs = NSUserDefaults(suiteName = "com.netlogger.lib.settings")
    private val current = MutableStateFlow(readSettings())
    override fun getSettings(): Flow<LogSettings> = current
    override fun getCurrentSettings(): LogSettings = current.value
    override suspend fun saveSettings(settings: LogSettings) {
        prefs.setBool(settings.autoResetOnStart, "auto_reset")
        prefs.setObject(settings.logLevel.name, "log_level")
        current.value = readSettings()
    }
    private fun readSettings(): LogSettings {
        val requested = runCatching { LogLevel.valueOf(prefs.stringForKey("log_level").orEmpty()) }
            .getOrDefault(config.maximumLogLevel)
        val headers = requested.headers && config.maximumLogLevel.headers
        val body = requested.body && config.maximumLogLevel.body
        val level = when {
            !requested.info || !config.maximumLogLevel.info -> LogLevel.NONE
            headers && body -> LogLevel.ALL
            headers -> LogLevel.HEADERS
            body -> LogLevel.BODY
            else -> LogLevel.INFO
        }
        return LogSettings(autoResetOnStart = prefs.boolForKey("auto_reset"), logLevel = level,
            enableShakeDetector = false, enableFloatingButton = false)
    }
    override fun getFilterTabOrder(): List<String> = runCatching {
        Json.decodeFromString<List<String>>(prefs.stringForKey("filter_tab_order") ?: "[]")
    }.getOrDefault(emptyList())
    override fun saveFilterTabOrder(tabIds: List<String>) {
        prefs.setObject(Json.encodeToString(tabIds), "filter_tab_order")
    }
}
