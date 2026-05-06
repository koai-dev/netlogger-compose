package com.netlogger.lib.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.netlogger.lib.presentation.ui.detail.NetloggerDetailScreen
import com.netlogger.lib.presentation.ui.list.NetloggerListScreen
import com.netlogger.lib.presentation.ui.list.NetloggerListViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

sealed class NetloggerScreen {
    object List : NetloggerScreen()
    data class Detail(val logType: String, val jsonString: String) : NetloggerScreen()
}

class NetloggerActivity : ComponentActivity() {
    private val viewModel: NetloggerListViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                                viewModel = viewModel,
                                onLogClicked = { log, json ->
                                    currentScreen = NetloggerScreen.Detail(log.type.name, json)
                                },
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
                    }
                }
            }
        }
    }
}
