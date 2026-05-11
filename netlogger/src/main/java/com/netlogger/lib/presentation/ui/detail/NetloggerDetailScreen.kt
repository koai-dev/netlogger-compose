package com.netlogger.lib.presentation.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.presentation.util.CurlGenerator

@Composable
fun NetloggerDetailScreen(
    initTab: Int = 0,
    logType: String,
    jsonString: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gson = remember { Gson() }
    val logEntry = remember(jsonString) {
        try {
            if (logType == "API") {
                gson.fromJson(jsonString, LogEntry.Api::class.java)
            } else {
                gson.fromJson(jsonString, LogEntry.General::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    var selectedTab by remember { mutableIntStateOf(initTab) }
    val tabs = listOf("Overview", "Request", "Response")

    Scaffold(
        topBar = {
            NetloggerDetailTopAppBar(
                title = "Log Detail",
                onBack = onBack
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (logType == "API" && logEntry is LogEntry.Api) {
                SecondaryTabRow(
                    selectedTab,
                    Modifier,
                    Color.White,
                    NetloggerDetailColors.Teal,
                    {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedTab),
                            color = NetloggerDetailColors.Teal
                        )
                    },
                    @Composable { HorizontalDivider() }) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selectedContentColor = NetloggerDetailColors.Teal,
                            unselectedContentColor = NetloggerDetailColors.Label
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> OverviewTab(logEntry, context)
                        1 -> RequestTab(logEntry, context)
                        2 -> ResponseTab(logEntry, context)
                    }
                }
            } else {
                // Non-API or Error fallback
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    JsonSection(
                        title = logType,
                        jsonString = jsonString,
                        onCopy = { copyToClipboard(context, "Log Content", jsonString) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(log: LogEntry.Api, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        DetailInfoCard(
            statusCode = "${log.statusCode} ${if (log.statusCode == 200) "OK" else "ERR"}",
            method = log.method,
            duration = "${log.totalDuration}ms",
            protocol = "HTTP/1.1"
        )

        UrlSection(
            url = log.url,
            onCopy = { copyToClipboard(context, "URL", log.url) }
        )

        TimelineSection(
            requestTime = log.requestTime,
            responseTime = log.responseTime,
            totalDuration = log.totalDuration
        )

        Button(
            onClick = {
                val curl =
                    CurlGenerator.generate(log.method, log.url, log.requestHeaders, log.requestBody)
                copyToClipboard(context, "cURL", curl)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NetloggerDetailColors.Teal),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy cURL", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RequestTab(log: LogEntry.Api, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExpandableHeadersSection(title = "Request Headers", headers = log.requestHeaders)

        JsonSection(
            title = "Request Body (JSON)",
            jsonString = log.requestBody,
            onCopy = { copyToClipboard(context, "Request Body", log.requestBody ?: "") }
        )
    }
}

@Composable
private fun ResponseTab(log: LogEntry.Api, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = NetloggerDetailColors.SectionBg,
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    log.method,
                    color = NetloggerDetailColors.Teal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    log.url,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                val statusText = if (log.statusCode == 200) "200 OK" else "${log.statusCode} ERR"
                Text(
                    statusText,
                    color = NetloggerDetailColors.Teal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${log.totalDuration}ms",
                    color = NetloggerDetailColors.Label,
                    fontSize = 12.sp
                )
            }
        }

        ExpandableHeadersSection(title = "Response Headers", headers = log.responseHeaders)

        JsonSection(
            title = "Response Body",
            jsonString = log.responseBody,
            onCopy = { copyToClipboard(context, "Response Body", log.responseBody ?: "") }
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    if (text.isBlank()) {
        Toast.makeText(context, "$label is empty", Toast.LENGTH_SHORT).show()
        return
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
}

@Preview(showBackground = true)
@Composable
fun NetloggerDetailScreenPreview() {
    MaterialTheme {
        val sampleApiLog = """
            {
                "method": "GET",
                "url": "https://api.netscannerpro.com/v1/network/diagnostics?region=us-east-1&filter=active",
                "requestHeaders": "{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer ***\"}",
                "requestBody": "{\"username\": \"stitch_designer\", \"action\": \"create_ui\", \"timestamp\": 1715682000}",
                "responseBody": "{\"status\": \"success\", \"data\": {\"id\": 1, \"name\": \"Netlogger\"}}",
                "statusCode": 200,
                "requestTime": 22,
                "responseTime": 123,
                "totalDuration": 145
            }
        """.trimIndent()

        NetloggerDetailScreen(
            logType = "API",
            jsonString = sampleApiLog,
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetloggerDetailScreenTabRequestPreview() {
    MaterialTheme {
        val sampleApiLog = """
            {
                "method": "GET",
                "url": "https://api.netscannerpro.com/v1/network/diagnostics?region=us-east-1&filter=active",
                "requestHeaders": "{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer ***\"}",
                "requestBody": "{\"username\": \"stitch_designer\", \"action\": \"create_ui\", \"timestamp\": 1715682000}",
                "responseBody": "{\"status\": \"success\", \"data\": {\"id\": 1, \"name\": \"Netlogger\"}}",
                "statusCode": 200,
                "requestTime": 22,
                "responseTime": 123,
                "totalDuration": 145
            }
        """.trimIndent()

        NetloggerDetailScreen(
            initTab = 1,
            logType = "API",
            jsonString = sampleApiLog,
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NetloggerDetailScreenTabResponsePreview() {
    MaterialTheme {
        val sampleApiLog = """
            {
                "method": "GET",
                "url": "https://api.netscannerpro.com/v1/network/diagnostics?region=us-east-1&filter=active",
                "requestHeaders": "{\"Content-Type\": \"application/json\", \"Authorization\": \"Bearer ***\"}",
                "requestBody": "{\"username\": \"stitch_designer\", \"action\": \"create_ui\", \"timestamp\": 1715682000}",
                "responseBody": "{\"status\": \"success\", \"data\": {\"id\": 1, \"name\": \"Netlogger\"}}",
                "statusCode": 200,
                "requestTime": 22,
                "responseTime": 123,
                "totalDuration": 145
            }
        """.trimIndent()

        NetloggerDetailScreen(
            initTab = 2,
            logType = "API",
            jsonString = sampleApiLog,
            onBack = {}
        )
    }
}
