package com.netlogger.lib

import com.netlogger.lib.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosShakeSettingsTest {
    @Test
    fun shakePreferencePersistsAndConfigurationCapsIt() = runBlocking {
        val suite = "com.netlogger.lib.test.shake-settings"
        val prefs = NSUserDefaults(suiteName = suite)
        prefs.removePersistentDomainForName(suite)
        try {
            val config = NetloggerConfig(allowShakeDetector = true)
            val settings = SettingsRepositoryImpl(config, prefs)
            assertTrue(settings.getCurrentSettings().enableShakeDetector)
            settings.saveSettings(settings.getCurrentSettings().copy(enableShakeDetector = false))
            assertFalse(SettingsRepositoryImpl(config, prefs).getCurrentSettings().enableShakeDetector)
            settings.saveSettings(settings.getCurrentSettings().copy(enableShakeDetector = true))
            assertTrue(SettingsRepositoryImpl(config, prefs).getCurrentSettings().enableShakeDetector)
            val optedOut = SettingsRepositoryImpl(NetloggerConfig(allowShakeDetector = false), prefs)
            assertFalse(optedOut.getCurrentSettings().enableShakeDetector)
            optedOut.saveSettings(settings.getCurrentSettings().copy(enableShakeDetector = true))
            assertFalse(optedOut.getCurrentSettings().enableShakeDetector)
        } finally { prefs.removePersistentDomainForName(suite) }
    }
}
