package com.netlogger.lib.presentation.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netlogger.lib.presentation.ui.components.JsonViewer
import com.netlogger.lib.presentation.ui.components.highlightText

internal object NetloggerDetailColors {
    val Teal = Color(0xFF00796B)
    val DarkBg = Color(0xFF1E293B)
    val SectionBg = Color(0xFFF8FAFC)
    val Border = Color(0xFFE2E8F0)
    val Label = Color(0xFF64748B)
    val Text = Color(0xFF1E293B)
    val GreenBadge = Color(0xFFD1FAE5)
    val GreenText = Color(0xFF065F46)
    val BlueBadge = Color(0xFFDBEAFE)
    val BlueText = Color(0xFF1E40AF)
}

@Composable
internal fun DetailInfoCard(
    statusCode: String,
    method: String,
    duration: String,
    protocol: String,
    searchQuery: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NetloggerDetailColors.Border, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoItem(
                "STATUS CODE",
                statusCode,
                NetloggerDetailColors.GreenBadge,
                NetloggerDetailColors.GreenText,
                searchQuery
            )
            InfoItem(
                "METHOD",
                method,
                NetloggerDetailColors.BlueBadge,
                NetloggerDetailColors.BlueText,
                searchQuery
            )
            InfoItem("DURATION", duration, searchQuery = searchQuery)
            InfoItem("PROTOCOL", protocol, searchQuery = searchQuery)
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    badgeBg: Color? = null,
    badgeFg: Color? = null,
    searchQuery: String = ""
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NetloggerDetailColors.Label
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (badgeBg != null && badgeFg != null) {
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = highlightText(value, searchQuery, badgeFg),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Text(
                text = highlightText(value, searchQuery, NetloggerDetailColors.Text),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
internal fun UrlSection(url: String, searchQuery: String = "", onCopy: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "REQUEST URL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NetloggerDetailColors.Label
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(NetloggerDetailColors.DarkBg)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = highlightText(url, searchQuery, Color.White),
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onCopy() }
                )
            }
        }
    }
}

@Composable
internal fun TimelineSection(requestTime: Long, responseTime: Long, totalDuration: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NetloggerDetailColors.Border, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PERFORMANCE TIMELINE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NetloggerDetailColors.Label
            )
            Spacer(modifier = Modifier.height(16.dp))

            TimelineRow("Request", requestTime, totalDuration, NetloggerDetailColors.Teal)
            Spacer(modifier = Modifier.height(12.dp))
            TimelineRow("Response", responseTime, totalDuration, Color(0xFF6EE7B7))

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total Latency",
                    fontWeight = FontWeight.Medium,
                    color = NetloggerDetailColors.Label
                )
                Text(
                    "${totalDuration}ms",
                    fontWeight = FontWeight.Bold,
                    color = NetloggerDetailColors.Text
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(label: String, time: Long, total: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.width(80.dp),
            fontSize = 13.sp,
            color = NetloggerDetailColors.Label,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        Spacer(modifier = Modifier.width(12.dp))
        val fraction = if (total > 0) (time.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${time}ms",
            fontSize = 13.sp,
            color = NetloggerDetailColors.Label,
            modifier = Modifier.width(50.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
internal fun JsonSection(
    title: String,
    jsonString: String?,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    currentSearchIndex: Int = -1,
    onSearchResultsChanged: (Int) -> Unit = {},
    onCurrentSearchPositionChanged: (String) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NetloggerDetailColors.Text
            )
            TextButton(
                onClick = onCopy,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy", fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(NetloggerDetailColors.DarkBg)
                .padding(8.dp)
                .heightIn(max = 500.dp)
        ) {
            if (jsonString.isNullOrBlank()) {
                Text("Data Not Found", fontSize = 13.sp, color = NetloggerDetailColors.Label)
            } else {
                JsonViewer(
                    jsonString = jsonString,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(),
                    searchQuery = searchQuery,
                    currentSearchIndex = currentSearchIndex,
                    onSearchResultsChanged = onSearchResultsChanged,
                    onCurrentSearchPositionChanged = onCurrentSearchPositionChanged
                )
            }
        }
    }
}

@Composable
internal fun ExpandableHeadersSection(
    title: String,
    headers: String?,
    searchQuery: String = "",
    currentSearchIndex: Int = -1,
    onSearchResultsChanged: (Int) -> Unit = {},
    onCurrentSearchPositionChanged: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(true) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) expanded = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NetloggerDetailColors.Border, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NetloggerDetailColors.Text
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = NetloggerDetailColors.Label
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                ) {
                    if (headers.isNullOrBlank()) {
                        Text("No headers", fontSize = 13.sp, color = NetloggerDetailColors.Label)
                    } else {
                        JsonViewer(
                            jsonString = headers,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            isLight = true,
                            searchQuery = searchQuery,
                            currentSearchIndex = currentSearchIndex,
                            onSearchResultsChanged = onSearchResultsChanged,
                            onCurrentSearchPositionChanged = onCurrentSearchPositionChanged
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetloggerDetailTopAppBar(
    title: String,
    onBack: () -> Unit,
    searchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {},
    searchResultCount: Int = 0,
    currentSearchIndex: Int = 0,
    currentSearchPosition: String = "",
    onPrevSearch: () -> Unit = {},
    onNextSearch: () -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchToggle: (Boolean) -> Unit = {}
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(fontSize = 16.sp, color = NetloggerDetailColors.Text),
                        cursorBrush = SolidColor(NetloggerDetailColors.Teal),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search...", color = Color.Gray, fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = buildString {
                                append(if (searchResultCount > 0) "${currentSearchIndex + 1}/$searchResultCount" else "0/0")
                                if (currentSearchPosition.isNotBlank()) {
                                    append(" - ")
                                    append(currentSearchPosition)
                                }
                            },
                            fontSize = 12.sp,
                            color = NetloggerDetailColors.Label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .width(120.dp)
                        )
                        IconButton(onClick = onPrevSearch) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Prev")
                        }
                        IconButton(onClick = onNextSearch) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                        }
                    }
                }
            } else {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NetloggerDetailColors.Text
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = if (isSearchActive) {
                    { onSearchToggle(false) }
                } else onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (!isSearchActive) {
                IconButton(onClick = { onSearchToggle(true) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            } else if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChanged("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            navigationIconContentColor = NetloggerDetailColors.Text,
            titleContentColor = NetloggerDetailColors.Text,
            actionIconContentColor = NetloggerDetailColors.Text
        )
    )
}
