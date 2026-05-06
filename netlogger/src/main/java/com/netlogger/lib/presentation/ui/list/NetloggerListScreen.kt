package com.netlogger.lib.presentation.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.netlogger.lib.domain.model.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetloggerListScreen(
    viewModel: NetloggerListViewModel,
    onLogClicked: (LogEntry, String) -> Unit,
    onClose: () -> Unit
) {
    val logs by viewModel.logs.collectAsState()
    NetloggerListContent(
        logs = logs,
        onLogClicked = onLogClicked,
        onClose = onClose,
        clearLogs = { viewModel.clearLogs() },
        search = { viewModel.search(it) },
        filterByType = { viewModel.filterByType(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetloggerListContent(
    logs: List<LogListItem>,
    onLogClicked: (LogEntry, String) -> Unit = { _, _ -> },
    onClose: () -> Unit = {},
    clearLogs: () -> Unit = {},
    search: (String) -> Unit = {},
    filterByType: (String?) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ALL") }
    var expanded by remember { mutableStateOf(false) }

    val gson = remember { Gson() }
    val types = listOf("ALL", "API", "GENERAL")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Netlogger") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Text("X", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    TextButton(onClick = { clearLogs() }) {
                        Text("Clear")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        search(it)
                    },
                    placeholder = { Text("Search...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.width(120.dp)
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        types.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    filterByType(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs) { item ->
                    when (item) {
                        is LogListItem.DateHeader -> {
                            Text(
                                text = item.dateLabel,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is LogListItem.LogItem -> {
                            LogItemRow(log = item.log) {
                                onLogClicked(it, gson.toJson(it))
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogItemRow(log: LogEntry, onClick: (LogEntry) -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val timeString = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(log) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeString,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))

                when (log) {
                    is LogEntry.Api -> {
                        Text(
                            text = log.method,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007BFF) // Info
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val statusColor =
                            if (log.statusCode in 200..299) Color(0xFF28A745) else Color(0xFFDC3545)
                        Text(
                            text = log.statusCode.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    is LogEntry.General -> {
                        val levelColor = when (log.level.name) {
                            "INFO" -> Color(0xFF007BFF)
                            "WARNING" -> Color(0xFFFFC107)
                            "ERROR" -> Color(0xFFDC3545)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            text = log.level.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = levelColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = log.tag,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            when (log) {
                is LogEntry.Api -> {
                    Text(
                        text = log.url,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                is LogEntry.General -> {
                    Text(
                        text = log.message,
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (log is LogEntry.Api) {
            Text(
                text = "${log.totalDuration}ms",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LogItemRowPreview() {
    MaterialTheme {
        Column {
            LogItemRow(
                log = LogEntry.Api(
                    url = "https://api.example.com/v1/users",
                    method = "GET",
                    requestHeaders = null,
                    requestBody = null,
                    responseHeaders = null,
                    responseBody = "{}",
                    statusCode = 200,
                    totalDuration = 150L,
                    timestamp = System.currentTimeMillis(),
                    tag = "API",
                    requestTime = 0L,
                    responseTime = 150L
                ),
                onClick = {}
            )
            HorizontalDivider()
            LogItemRow(
                log = LogEntry.General(
                    message = "System initialized successfully",
                    tag = "AppStart",
                    level = com.netlogger.lib.domain.model.LogLevel.INFO,
                    timestamp = System.currentTimeMillis()
                ),
                onClick = {}
            )
            HorizontalDivider()
            LogItemRow(
                log = LogEntry.Api(
                    url = "https://api.example.com/v1/login",
                    method = "POST",
                    requestHeaders = null,
                    requestBody = null,
                    responseHeaders = null,
                    responseBody = "{}",
                    statusCode = 500,
                    totalDuration = 450L,
                    timestamp = System.currentTimeMillis(),
                    tag = "API",
                    requestTime = 0L,
                    responseTime = 450L
                ),
                onClick = {}
            )
        }
    }
}

@Composable
@Preview
private fun NetloggerListContentPreview() {
    MaterialTheme {
        NetloggerListContent(logs = listOf(
            LogListItem.DateHeader(
                dateLabel = "Yesterday"
            ),
            LogListItem.LogItem(
                LogEntry.Api(
                    url = "https://api.example.com/v1/login",
                    method = "POST",
                    requestHeaders = null,
                    requestBody = null,
                    responseHeaders = null,
                    responseBody = "{}",
                    statusCode = 500,
                    totalDuration = 450L,
                    timestamp = System.currentTimeMillis(),
                    tag = "API",
                    requestTime = 0L,
                    responseTime = 450L
                )
            ),
            LogListItem.LogItem(
                LogEntry.General(
                    message = "System initialized successfully",
                    tag = "AppStart",
                    level = com.netlogger.lib.domain.model.LogLevel.INFO,
                    timestamp = System.currentTimeMillis()
                )
            ),
        ))
    }
}
