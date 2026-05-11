package com.netlogger.lib.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netlogger.lib.domain.model.LogSettings
import com.netlogger.lib.domain.usecase.GetSettingsUseCase
import com.netlogger.lib.domain.usecase.SaveSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetloggerSettingsViewModel(
    getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {

    val settings: StateFlow<LogSettings> = getSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogSettings())

    fun updateAutoReset(enabled: Boolean) {
        viewModelScope.launch {
            saveSettingsUseCase(settings.value.copy(autoResetOnStart = enabled))
        }
    }

    fun updateShakeDetector(enabled: Boolean) {
        viewModelScope.launch {
            saveSettingsUseCase(settings.value.copy(enableShakeDetector = enabled))
        }
    }

    fun updateShakeSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            saveSettingsUseCase(settings.value.copy(shakeSensitivity = sensitivity))
        }
    }
}
