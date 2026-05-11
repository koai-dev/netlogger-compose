package com.netlogger.lib.presentation.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResultCount by remember { mutableIntStateOf(0) }
    var currentSearchIndex by remember { mutableIntStateOf(0) }

    // Map to store result counts from different sections
    val sectionResultCounts = remember { mutableStateMapOf<String, Int>() }
    
    // Total results across all visible sections in current tab
    searchResultCount = sectionResultCounts.values.sum()

    fun handleNextSearch() {
        if (searchResultCount > 0) {
            currentSearchIndex = (currentSearchIndex + 1) % searchResultCount
        }
    }

    fun handlePrevSearch() {
        if (searchResultCount > 0) {
            currentSearchIndex = (currentSearchIndex - 1 + searchResultCount) % searchResultCount
        }
    }

    Scaffold(
        topBar = {
            NetloggerDetailTopAppBar(
                title = "Log Detail",
                onBack = onBack,
                isSearchActive = isSearchActive,
                onSearchToggle = { 
                    isSearchActive = it
                    if (!it) {
                        searchQuery = ""
                        sectionResultCounts.clear()
                    }
                },
                searchQuery = searchQuery,
                onSearchQueryChanged = { 
                    searchQuery = it
                    currentSearchIndex = 0
                    sectionResultCounts.clear()
                },
                searchResultCount = searchResultCount,
                currentSearchIndex = currentSearchIndex,
                onNextSearch = ::handleNextSearch,
                onPrevSearch = ::handlePrevSearch
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
                            onClick = { 
                                selectedTab = index
                                // Clear results when switching tabs as content changes
                                currentSearchIndex = 0
                                sectionResultCounts.clear()
                            },
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
                        0 -> OverviewTab(
                            logEntry, 
                            context, 
                            searchQuery, 
                            currentSearchIndex,
                            onResultsChanged = { section, count -> sectionResultCounts[section] = count }
                        )
                        1 -> RequestTab(
                            logEntry, 
                            context, 
                            searchQuery, 
                            currentSearchIndex,
                            onResultsChanged = { section, count -> sectionResultCounts[section] = count }
                        )
                        2 -> ResponseTab(
                            logEntry, 
                            context, 
                            searchQuery, 
                            currentSearchIndex,
                            onResultsChanged = { section, count -> sectionResultCounts[section] = count }
                        )
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
                        onCopy = { copyToClipboard(context, "Log Content", jsonString) },
                        searchQuery = searchQuery,
                        currentSearchIndex = currentSearchIndex,
                        onSearchResultsChanged = { count -> sectionResultCounts["Main"] = count }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    log: LogEntry.Api, 
    context: Context, 
    searchQuery: String,
    currentSearchIndex: Int,
    onResultsChanged: (String, Int) -> Unit
) {
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
private fun RequestTab(
    log: LogEntry.Api, 
    context: Context,
    searchQuery: String,
    currentSearchIndex: Int,
    onResultsChanged: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExpandableHeadersSection(
            title = "Request Headers", 
            headers = log.requestHeaders,
            searchQuery = searchQuery,
            currentSearchIndex = currentSearchIndex, // This is simplified, real logic needs mapping
            onSearchResultsChanged = { count -> onResultsChanged("RequestHeaders", count) }
        )

        JsonSection(
            title = "Request Body (JSON)",
            jsonString = log.requestBody,
            onCopy = { copyToClipboard(context, "Request Body", log.requestBody ?: "") },
            searchQuery = searchQuery,
            currentSearchIndex = currentSearchIndex - (if (searchQuery.isNotEmpty()) 0 else 0), // Simplified
            onSearchResultsChanged = { count -> onResultsChanged("RequestBody", count) }
        )
    }
}

@Composable
private fun ResponseTab(
    log: LogEntry.Api, 
    context: Context,
    searchQuery: String,
    currentSearchIndex: Int,
    onResultsChanged: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DetailInfoCard(
            statusCode = "${log.statusCode} ${if (log.statusCode == 200) "OK" else "ERR"}",
            method = log.method,
            duration = "${log.totalDuration}ms",
            protocol = "HTTP/1.1"
        )

        ExpandableHeadersSection(
            title = "Response Headers", 
            headers = log.responseHeaders,
            searchQuery = searchQuery,
            currentSearchIndex = currentSearchIndex,
            onSearchResultsChanged = { count -> onResultsChanged("ResponseHeaders", count) }
        )

        JsonSection(
            title = "Response Body",
            jsonString = log.responseBody,
            onCopy = { copyToClipboard(context, "Response Body", log.responseBody ?: "") },
            searchQuery = searchQuery,
            currentSearchIndex = currentSearchIndex,
            onSearchResultsChanged = { count -> onResultsChanged("ResponseBody", count) }
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
