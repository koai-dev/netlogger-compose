package com.netlogger.lib.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.netlogger.lib.domain.model.LogSettings
import com.netlogger.lib.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("netlogger_settings", Context.MODE_PRIVATE)

    override fun getSettings(): Flow<LogSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(readSettings())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(readSettings())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(readSettings()) }

    override suspend fun saveSettings(settings: LogSettings) {
        prefs.edit().apply {
            putBoolean("auto_reset", settings.autoResetOnStart)
            putBoolean("enable_shake", settings.enableShakeDetector)
            putFloat("shake_sensitivity", settings.shakeSensitivity)
            apply()
        }
    }

    private fun readSettings(): LogSettings {
        return LogSettings(
            autoResetOnStart = prefs.getBoolean("auto_reset", false),
            enableShakeDetector = prefs.getBoolean("enable_shake", true),
            shakeSensitivity = prefs.getFloat("shake_sensitivity", 2.0f)
        )
    }
}
