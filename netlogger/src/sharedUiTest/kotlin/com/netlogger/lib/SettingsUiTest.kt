@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.netlogger.lib

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import com.netlogger.lib.domain.model.LogSettings
import com.netlogger.lib.domain.repository.SettingsRepository
import com.netlogger.lib.domain.usecase.GetSettingsUseCase
import com.netlogger.lib.domain.usecase.SaveSettingsUseCase
import com.netlogger.lib.platform.supportsAndroidShortcuts
import com.netlogger.lib.presentation.ui.settings.NetloggerSettingsScreen
import com.netlogger.lib.presentation.ui.settings.NetloggerSettingsViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The same UI interactions run on an Android device and the native iOS Simulator. */
class SettingsUiTest {
    @Test
    fun shakeTogglePersistsAndBackClosesSettings() = runComposeUiTest {
        val repository = TestSettingsRepository()
        val store = ViewModelStore()
        val viewModel = NetloggerSettingsViewModel(GetSettingsUseCase(repository), SaveSettingsUseCase(repository))
        store.put("settings", viewModel)
        var closed = false
        try {
            setContent { MaterialTheme { NetloggerSettingsScreen(viewModel) { closed = true } } }
            onNodeWithTag("netlogger.shake.toggle").performScrollTo().assertIsOff().performClick()
            waitUntil { repository.getCurrentSettings().enableShakeDetector }
            onNodeWithTag("netlogger.shake.toggle").assertIsOn()
            if (supportsAndroidShortcuts) {
                onNodeWithTag("netlogger.shake.sensitivity").assertExists()
                onNodeWithTag("netlogger.floating.toggle").assertExists()
            } else {
                onNodeWithTag("netlogger.shake.sensitivity").assertDoesNotExist()
                onNodeWithTag("netlogger.floating.toggle").assertDoesNotExist()
            }
            onNodeWithTag("netlogger.shake.toggle").performScrollTo().performClick()
            waitUntil { !repository.getCurrentSettings().enableShakeDetector }
            onNodeWithTag("netlogger.shake.toggle").assertIsOff()
            onNodeWithTag("netlogger.shake.sensitivity").assertDoesNotExist()
            onNodeWithContentDescription("Back").performClick()
            runOnIdle { assertTrue(closed) }
        } finally { runOnIdle { store.clear() } }
    }

    @Test
    fun shakeToggleCannotOverrideHostOptOut() = runComposeUiTest {
        val repository = TestSettingsRepository(allowShake = false)
        val store = ViewModelStore()
        val viewModel = NetloggerSettingsViewModel(GetSettingsUseCase(repository), SaveSettingsUseCase(repository))
        store.put("settings", viewModel)
        try {
            setContent { MaterialTheme { NetloggerSettingsScreen(viewModel) {} } }
            onNodeWithTag("netlogger.shake.toggle").performScrollTo().performClick()
            waitForIdle()
            onNodeWithTag("netlogger.shake.toggle").assertIsOff()
            runOnIdle { assertFalse(repository.getCurrentSettings().enableShakeDetector) }
        } finally { runOnIdle { store.clear() } }
    }

    private class TestSettingsRepository(private val allowShake: Boolean = true) : SettingsRepository {
        private val state = MutableStateFlow(LogSettings())
        override fun getSettings() = state
        override fun getCurrentSettings() = state.value
        override suspend fun saveSettings(settings: LogSettings) {
            state.value = settings.copy(enableShakeDetector = settings.enableShakeDetector && allowShake)
        }
    }
}
