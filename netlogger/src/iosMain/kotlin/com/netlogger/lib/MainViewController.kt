package com.netlogger.lib

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.presentation.ui.NetloggerContent
import com.netlogger.lib.presentation.util.LogUtil
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch

/** Entry point for the iOS sample. Hosts the same Compose UI as Android. */
fun MainViewController() = ComposeUIViewController {
    val client = remember {
        Netlogger.init(NetloggerConfig(maximumLogLevel = LogLevel.ALL, captureBodies = true, captureGeneralLogs = true))
        HttpClient(Darwin) { Netlogger.configureClient(this) }
    }
    DisposableEffect(client) { onDispose { client.close() } }
    val scope = rememberCoroutineScope()
    var showLogs by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Netlogger iOS sample") }
    MaterialTheme {
        if (showLogs) NetloggerContent { showLogs = false }
        else Column(Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(status)
            Button(onClick = {
                scope.launch {
                    status = runCatching {
                        val response = client.get("https://httpbin.org/get?token=example-secret&q=visible")
                        response.bodyAsText()
                        "HTTP ${response.status.value} — open logs to inspect"
                    }.getOrElse { "Request failed: ${it.message}" }
                    LogUtil.info("Sample", status)
                }
            }) { Text("Call API") }
            Button(onClick = { LogUtil.info("Sample", "Opened shared log viewer"); showLogs = true }) { Text("Open Netlogger") }
        }
    }
}
