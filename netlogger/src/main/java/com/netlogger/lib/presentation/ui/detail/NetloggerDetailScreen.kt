package com.netlogger.lib.presentation.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
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
    var currentSearchIndex by remember { mutableIntStateOf(0) }
    var currentSearchPosition by remember { mutableStateOf("") }

    // Map to store result counts from different sections
    val sectionResultCounts = remember { mutableStateMapOf<String, Int>() }

    val activeSearchSections = when {
        logType != "API" -> listOf("Main")
        selectedTab == 1 -> listOf("RequestHeaders", "RequestBody")
        selectedTab == 2 -> listOf("ResponseHeaders", "ResponseBody")
        else -> emptyList()
    }

    // Total results across all visible sections in current tab
    val searchResultCount = activeSearchSections.sumOf { sectionResultCounts[it] ?: 0 }

    fun currentIndexForSection(section: String): Int {
        var offset = 0
        for (key in activeSearchSections) {
            val count = sectionResultCounts[key] ?: 0
            if (key == section) {
                val localIndex = currentSearchIndex - offset
                return if (localIndex in 0 until count) localIndex else -1
            }
            offset += count
        }
        return -1
    }

    fun handleCurrentSearchPosition(section: String, position: String) {
        if (currentIndexForSection(section) >= 0) {
            currentSearchPosition = if (position.isNotBlank()) "${section.searchSectionLabel()} $position" else ""
        }
    }

    fun handleNextSearch() {
        if (searchResultCount > 0) {
            currentSearchIndex = (currentSearchIndex + 1) % searchResultCount
            currentSearchPosition = ""
        }
    }

    fun handlePrevSearch() {
        if (searchResultCount > 0) {
            currentSearchIndex = (currentSearchIndex - 1 + searchResultCount) % searchResultCount
            currentSearchPosition = ""
        }
    }

    LaunchedEffect(searchResultCount) {
        if (searchResultCount == 0) {
            currentSearchIndex = 0
            currentSearchPosition = ""
        } else if (currentSearchIndex >= searchResultCount) {
            currentSearchIndex = searchResultCount - 1
            currentSearchPosition = ""
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
                        currentSearchPosition = ""
                        sectionResultCounts.clear()
                    }
                },
                searchQuery = searchQuery,
                onSearchQueryChanged = { 
                    searchQuery = it
                    currentSearchIndex = 0
                    currentSearchPosition = ""
                    sectionResultCounts.clear()
                },
                searchResultCount = searchResultCount,
                currentSearchIndex = currentSearchIndex,
                currentSearchPosition = currentSearchPosition,
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
                                currentSearchPosition = ""
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
                            currentSearchIndexForSection = ::currentIndexForSection,
                            onResultsChanged = { section, count -> sectionResultCounts[section] = count },
                            onCurrentPositionChanged = ::handleCurrentSearchPosition
                        )
                        2 -> ResponseTab(
                            logEntry, 
                            context, 
                            searchQuery, 
                            currentSearchIndexForSection = ::currentIndexForSection,
                            onResultsChanged = { section, count -> sectionResultCounts[section] = count },
                            onCurrentPositionChanged = ::handleCurrentSearchPosition
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
                        currentSearchIndex = currentIndexForSection("Main"),
                        onSearchResultsChanged = { count -> sectionResultCounts["Main"] = count },
                        onCurrentSearchPositionChanged = { position ->
                            handleCurrentSearchPosition("Main", position)
                        }
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
    currentSearchIndexForSection: (String) -> Int,
    onResultsChanged: (String, Int) -> Unit,
    onCurrentPositionChanged: (String, String) -> Unit
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
            currentSearchIndex = currentSearchIndexForSection("RequestHeaders"),
            onSearchResultsChanged = { count -> onResultsChanged("RequestHeaders", count) },
            onCurrentSearchPositionChanged = { position ->
                onCurrentPositionChanged("RequestHeaders", position)
            }
        )

        JsonSection(
            title = "Request Body (JSON)",
            jsonString = log.requestBody,
            onCopy = { copyToClipboard(context, "Request Body", log.requestBody ?: "") },
            searchQuery = searchQuery,
            currentSearchIndex = currentSearchIndexForSection("RequestBody"),
            onSearchResultsChanged = { count -> onResultsChanged("RequestBody", count) },
            onCurrentSearchPositionChanged = { position ->
                onCurrentPositionChanged("RequestBody", position)
            }
        )
    }
}

@Composable
private fun ResponseTab(
    log: LogEntry.Api, 
    context: Context,
    searchQuery: String,
    currentSearchIndexForSection: (String) -> Int,
    onResultsChanged: (String, Int) -> Unit,
    onCurrentPositionChanged: (String, String) -> Unit
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
            currentSearchIndex = currentSearchIndexForSection("ResponseHeaders"),
            onSearchResultsChanged = { count -> onResultsChanged("ResponseHeaders", count) },
            onCurrentSearchPositionChanged = { position ->
                onCurrentPositionChanged("ResponseHeaders", position)
            }
        )

        JsonSection(
            title = "Response Body",
            jsonString = log.responseBody,
            onCopy = { copyToClipboard(context, "Response Body", log.responseBody ?: "") },
            searchQuery = searchQuery,
            currentSearchIndex = currentSearchIndexForSection("ResponseBody"),
            onSearchResultsChanged = { count -> onResultsChanged("ResponseBody", count) },
            onCurrentSearchPositionChanged = { position ->
                onCurrentPositionChanged("ResponseBody", position)
            }
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    if (text.isBlank()) {
        Toast.makeText(context, "$label is empty", Toast.LENGTH_SHORT).show()
        return
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text).apply {
        description.extras = PersistableBundle().apply {
            putBoolean(SENSITIVE_CLIPBOARD_EXTRA, true)
        }
    }
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
}

private const val SENSITIVE_CLIPBOARD_EXTRA = "android.content.extra.IS_SENSITIVE"

private fun String.searchSectionLabel(): String = when (this) {
    "RequestHeaders" -> "Req headers"
    "RequestBody" -> "Req body"
    "ResponseHeaders" -> "Res headers"
    "ResponseBody" -> "Res body"
    else -> this
}
