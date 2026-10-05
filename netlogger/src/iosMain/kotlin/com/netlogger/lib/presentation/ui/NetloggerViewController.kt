package com.netlogger.lib.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.ComposeUIViewController
import com.netlogger.lib.Netlogger
import com.netlogger.lib.presentation.ui.detail.NetloggerDetailScreen
import com.netlogger.lib.presentation.ui.list.NetloggerListScreen
import com.netlogger.lib.presentation.ui.settings.NetloggerSettingsScreen
import platform.UIKit.UIViewController

/** Presents the shared log list, JSON viewer, filtering, cURL export, and settings. */
fun NetloggerViewController(onClose: () -> Unit = {}): UIViewController {
    check(Netlogger.isInitialized()) { "Call Netlogger.init first" }
    return ComposeUIViewController {
        MaterialTheme { NetloggerContent(onClose) }
    }
}

@Composable
internal fun NetloggerContent(onClose: () -> Unit) {
    val listViewModel = remember { Netlogger.listViewModel() }
    val settingsViewModel = remember { Netlogger.settingsViewModel() }
    // ViewModelStore releases coroutines when the controller leaves composition.
    val store = remember { androidx.lifecycle.ViewModelStore() }
    DisposableEffect(Unit) {
        store.put("list", listViewModel)
        store.put("settings", settingsViewModel)
        onDispose { store.clear() }
    }
    var screen by remember { mutableStateOf("list") }
    var detail by remember { mutableStateOf<Pair<String, String>?>(null) }
    when (screen) {
        "detail" -> detail?.let { (type, json) ->
            NetloggerDetailScreen(logType = type, jsonString = json, onBack = { screen = "list" })
        }
        "settings" -> NetloggerSettingsScreen(settingsViewModel, onBack = { screen = "list" })
        else -> NetloggerListScreen(listViewModel,
            onLogClicked = { log, json -> detail = log.type.name to json; screen = "detail" },
            onOpenSettings = { screen = "settings" }, onClose = onClose)
    }
}
