package com.netlogger.lib.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.domain.model.LogSettings
import com.netlogger.lib.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray

class SettingsRepositoryImpl(
    private val context: Context,
    private val maximumLogLevel: LogLevel = LogLevel.NONE,
    private val allowShakeDetector: Boolean = false,
    private val allowFloatingButton: Boolean = false
) : SettingsRepository {

    constructor(context: Context) : this(
        context = context,
        maximumLogLevel = LogLevel.NONE,
        allowShakeDetector = false,
        allowFloatingButton = false
    )

    private val prefs: SharedPreferences = context.getSharedPreferences("netlogger_settings", Context.MODE_PRIVATE)

    override fun getSettings(): Flow<LogSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(readSettings())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(readSettings())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun getCurrentSettings(): LogSettings = readSettings()

    override suspend fun saveSettings(settings: LogSettings) {
        prefs.edit().apply {
            putBoolean("auto_reset", settings.autoResetOnStart)
            putBoolean("enable_shake", settings.enableShakeDetector && allowShakeDetector)
            putFloat("shake_sensitivity", settings.shakeSensitivity.toSafeSensitivity())
            putString("log_level", restrictLogLevel(settings.logLevel).name)
            putBoolean("enable_floating_button", settings.enableFloatingButton && allowFloatingButton)
            apply()
        }
    }

    override fun getFilterTabOrder(): List<String> {
        val storedOrder = prefs.getString(FILTER_TAB_ORDER_KEY, null) ?: return emptyList()
        return runCatching {
            val jsonArray = JSONArray(storedOrder)
            buildList {
                repeat(jsonArray.length()) { index ->
                    jsonArray.optString(index)
                        .takeIf(String::isNotBlank)
                        ?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    override fun saveFilterTabOrder(tabIds: List<String>) {
        prefs.edit()
            .putString(FILTER_TAB_ORDER_KEY, JSONArray(tabIds).toString())
            .apply()
    }

    private fun readSettings(): LogSettings {
        val logLevelName = prefs.getString("log_level", maximumLogLevel.name) ?: maximumLogLevel.name
        val requestedLogLevel = try {
            LogLevel.valueOf(logLevelName)
        } catch (e: Exception) {
            maximumLogLevel
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val isShakeSupported = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        val defaultShowFab = allowFloatingButton

        return LogSettings(
            autoResetOnStart = prefs.getBoolean("auto_reset", false),
            enableShakeDetector = allowShakeDetector && isShakeSupported &&
                prefs.getBoolean("enable_shake", true),
            shakeSensitivity = prefs.getFloat("shake_sensitivity", DEFAULT_SENSITIVITY)
                .toSafeSensitivity(),
            logLevel = restrictLogLevel(requestedLogLevel),
            enableFloatingButton = allowFloatingButton &&
                prefs.getBoolean("enable_floating_button", defaultShowFab)
        )
    }

    private fun restrictLogLevel(requested: LogLevel): LogLevel {
        val info = requested.info && maximumLogLevel.info
        val headers = requested.headers && maximumLogLevel.headers
        val body = requested.body && maximumLogLevel.body
        return when {
            !info -> LogLevel.NONE
            headers && body -> LogLevel.ALL
            headers -> LogLevel.HEADERS
            body -> LogLevel.BODY
            else -> LogLevel.INFO
        }
    }

    private fun Float.toSafeSensitivity(): Float =
        if (isFinite()) coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY) else DEFAULT_SENSITIVITY

    private companion object {
        const val MIN_SENSITIVITY = 1.0f
        const val MAX_SENSITIVITY = 5.0f
        const val DEFAULT_SENSITIVITY = 2.0f
        const val FILTER_TAB_ORDER_KEY = "filter_tab_order"
    }
}
