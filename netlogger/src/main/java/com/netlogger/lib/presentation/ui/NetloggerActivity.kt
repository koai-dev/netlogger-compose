package com.netlogger.lib.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.netlogger.lib.Netlogger
import com.netlogger.lib.presentation.ui.detail.NetloggerDetailScreen
import com.netlogger.lib.presentation.ui.list.NetloggerListScreen
import com.netlogger.lib.presentation.ui.list.NetloggerListViewModel
import com.netlogger.lib.presentation.ui.settings.NetloggerSettingsScreen
import com.netlogger.lib.presentation.ui.settings.NetloggerSettingsViewModel

sealed class NetloggerScreen {
    object List : NetloggerScreen()
    data class Detail(val logType: String, val jsonString: String) : NetloggerScreen()
    object Settings : NetloggerScreen()
}

class NetloggerActivity : ComponentActivity() {
    
    private val listViewModel: NetloggerListViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NetloggerListViewModel(
                    Netlogger.getLogsUseCase,
                    Netlogger.clearLogsUseCase
                ) as T
            }
        }
    }
    
    private val settingsViewModel: NetloggerSettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NetloggerSettingsViewModel(
                    Netlogger.getSettingsUseCase,
                    Netlogger.saveSettingsUseCase
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<NetloggerScreen>(NetloggerScreen.List) }

                    BackHandler(enabled = currentScreen !is NetloggerScreen.List) {
                        currentScreen = NetloggerScreen.List
                    }

                    when (val screen = currentScreen) {
                        is NetloggerScreen.List -> {
                            NetloggerListScreen(
                                viewModel = listViewModel,
                                onLogClicked = { log, json ->
                                    currentScreen = NetloggerScreen.Detail(log.type.name, json)
                                },
                                onOpenSettings = { currentScreen = NetloggerScreen.Settings },
                                onClose = { finish() }
                            )
                        }

                        is NetloggerScreen.Detail -> {
                            NetloggerDetailScreen(
                                logType = screen.logType,
                                jsonString = screen.jsonString,
                                onBack = { currentScreen = NetloggerScreen.List }
                            )
                        }

                        is NetloggerScreen.Settings -> {
                            NetloggerSettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { currentScreen = NetloggerScreen.List }
                            )
                        }
                    }
                }
            }
        }
    }
}
