package com.netlogger.lib.presentation.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import com.netlogger.lib.presentation.ui.components.JsonViewer
import com.netlogger.lib.presentation.util.CurlGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetloggerDetailScreen(
    logType: String,
    jsonString: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val title = if (logType == "API") "API Detail" else "Log Detail"
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Request", "Response")

    val jsonObject = remember(jsonString) {
        try {
            JsonParser.parseString(jsonString).asJsonObject
        } catch (e: Exception) {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        bottomBar = {
            if (logType == "API") {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = {
                            val method = jsonObject?.get("method")?.asString ?: "GET"
                            val url = jsonObject?.get("url")?.asString ?: ""
                            val headers = jsonObject?.get("requestHeaders")?.asString
                            val body = jsonObject?.get("requestBody")?.asString
                            val curl = CurlGenerator.generate(method, url, headers, body)
                            copyToClipboard(context, "cURL", curl)
                        }) {
                            Text("Copy cURL")
                        }
                        TextButton(onClick = {
                            val body = jsonObject?.get("requestBody")?.asString
                            if (body.isNullOrBlank()) {
                                Toast.makeText(context, "Request body is empty", Toast.LENGTH_SHORT).show()
                            } else {
                                copyToClipboard(context, "Request Body", body)
                            }
                        }) {
                            Text("Copy Req")
                        }
                        TextButton(onClick = {
                            val body = jsonObject?.get("responseBody")?.asString
                            if (body.isNullOrBlank()) {
                                Toast.makeText(context, "Response body is empty", Toast.LENGTH_SHORT).show()
                            } else {
                                copyToClipboard(context, "Response Body", body)
                            }
                        }) {
                            Text("Copy Res")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (logType == "API") {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).padding(8.dp)) {
                    JsonViewer(jsonString = jsonString, initialType = selectedTab)
                }
            } else {
                Box(modifier = Modifier.weight(1f).padding(8.dp)) {
                    JsonViewer(jsonString = jsonString, initialType = 0)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun NetloggerDetailScreenPreview() {
    MaterialTheme {
        val sampleApiLog = """
            {
                "method": "POST",
                "url": "https://api.example.com/v1/login",
                "requestHeaders": "{\"Content-Type\": \"application/json\"}",
                "requestBody": "{\"username\": \"test\", \"password\": \"123456\"}",
                "responseBody": "{\"token\": \"abcxyz\", \"user\": {\"id\": 1}}"
            }
        """.trimIndent()
        
        NetloggerDetailScreen(
            logType = "API",
            jsonString = sampleApiLog,
            onBack = {}
        )
    }
}
