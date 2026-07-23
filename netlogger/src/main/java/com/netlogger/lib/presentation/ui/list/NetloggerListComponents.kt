package com.netlogger.lib.presentation.ui.list

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.netlogger.lib.R
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogSeverity
import com.netlogger.lib.presentation.ui.components.NetloggerIconButton
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object NetloggerListColors {
    val Screen = Color(0xFFFFFFFF)
    val Ink = Color(0xFF121D33)
    val Muted = Color(0xFF667274)
    val Border = Color(0xFFB8C6C3)
    val Chip = Color(0xFFE5EDF8)
    val Teal = Color(0xFF00796B)
    val Red = Color(0xFFBD1A20)
    val Gear = Color(0xFF728096)
    val GreenBg = Color(0xFFD8FFE6)
    val GreenBorder = Color(0xFFA8F0C3)
    val GreenText = Color(0xFF067A3A)
    val RedBg = Color(0xFFFFF0F0)
    val RedBorder = Color(0xFFFFC8C8)
    val ErrorText = Color(0xFF9B1117)
    val BlueBg = Color(0xFFEAF2FF)
    val BlueBorder = Color(0xFFC7DBFF)
    val BlueText = Color(0xFF244EBC)
    val SurfaceSoft = Color(0xFFF8FAFC)
    val QueryBg = Color(0xFFF1F5F9)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetloggerHeader(
    onClearLogs: () -> Unit,
    onSettingsClick: () -> Unit,
    onClose: () -> Unit = {}
) {
    Column {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_terminal),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NetScanner Pro",
                        color = NetloggerListColors.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NetloggerIconButton(
                        icon = R.drawable.ic_delete,
                        colorFilter = NetloggerListColors.Red,
                        onClick = onClearLogs
                    )
                    NetloggerIconButton(
                        icon = R.drawable.ic_settings,
                        colorFilter = NetloggerListColors.Gear,
                        onClick = onSettingsClick
                    )
                    NetloggerIconButton(
                        icon = R.drawable.ic_close,
                        colorFilter = NetloggerListColors.Gear,
                        onClick = onClose
                    )

                }
            },
            colors = TopAppBarDefaults.topAppBarColors().copy(
                containerColor = NetloggerListColors.Screen,
            )
        )
        HorizontalDivider()
    }
}

@Composable
internal fun NetloggerSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onFilterClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.5.dp, NetloggerListColors.Border, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NetloggerIconButton(
            icon = R.drawable.ic_search,
            colorFilter = NetloggerListColors.Gear,
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            singleLine = true,
            textStyle = TextStyle(
                color = NetloggerListColors.Ink,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) Text(
                    "Search logs...",
                    color = NetloggerListColors.Gear,
                    fontSize = 16.sp
                )
                inner()
            }
        )
        NetloggerIconButton(
            icon = R.drawable.ic_close,
            colorFilter = NetloggerListColors.Gear,
            onClick = onClearQuery
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(NetloggerListColors.Border)
        )
        NetloggerIconButton(
            icon = R.drawable.ic_filter_list,
            colorFilter = NetloggerListColors.Gear,
            onClick = onFilterClick
        )
    }
}

@Composable
internal fun FilterChipsRow(
    filters: List<NetloggerFilter>,
    selectedFilter: NetloggerFilter,
    onFilterSelected: (NetloggerFilter) -> Unit,
    onFilterMoved: (Int, Int) -> Unit = { _, _ -> },
    onFilterMoveFinished: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val currentFilters by rememberUpdatedState(filters)
    val currentOnFilterMoved by rememberUpdatedState(onFilterMoved)
    val currentOnFilterMoveFinished by rememberUpdatedState(onFilterMoveFinished)
    val hapticFeedback = LocalHapticFeedback.current
    var draggedFilterId by remember { mutableStateOf<String?>(null) }
    var dragPointerX by remember { mutableStateOf(0f) }
    var dragTouchOffsetX by remember { mutableStateOf(0f) }
    var activeDragOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastMoveTargetId by remember { mutableStateOf<String?>(null) }
    var autoScrollDirection by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val reorderEdgePx = with(density) { 48.dp.toPx() }
    val autoScrollStepPx = with(density) { 12.dp.toPx() }

    fun moveDraggedFilterIfNeeded() {
        val sourceId = draggedFilterId ?: return
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val sourceItem = visibleItems.firstOrNull { it.key == sourceId } ?: return
        val draggedCenter = dragPointerX - dragTouchOffsetX + sourceItem.size / 2f
        val targetItem = visibleItems.firstOrNull {
            it.key != sourceId &&
                draggedCenter >= it.offset &&
                draggedCenter <= it.offset + it.size
        }
        if (targetItem == null) {
            lastMoveTargetId = null
            return
        }

        val targetId = targetItem.key as? String ?: return
        if (targetId == lastMoveTargetId) return

        val fromIndex = activeDragOrder.indexOf(sourceId)
        val toIndex = activeDragOrder.indexOf(targetId)
        if (fromIndex != -1 && toIndex != -1) {
            activeDragOrder = activeDragOrder.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            lastMoveTargetId = targetId
            currentOnFilterMoved(fromIndex, toIndex)
        }
    }

    LaunchedEffect(draggedFilterId, autoScrollDirection) {
        while (draggedFilterId != null && autoScrollDirection != 0) {
            val consumed = listState.scrollBy(autoScrollDirection * autoScrollStepPx)
            moveDraggedFilterIfNeeded()
            if (consumed == 0f) break
            delay(16)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            val selected = filter == selectedFilter
            item(key = filter.id) {
                val isDragging = draggedFilterId == filter.id
                Text(
                    text = filter.title,
                    color = if (selected) Color.White else Color(0xFF3E494B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            if (isDragging) {
                                val itemInfo = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == filter.id }
                                translationX = itemInfo?.let {
                                    dragPointerX - it.offset - dragTouchOffsetX
                                } ?: 0f
                                scaleX = 1.04f
                                scaleY = 1.04f
                                shadowElevation = 8.dp.toPx()
                            }
                        }
                        .pointerInput(filter.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { touchOffset ->
                                    val itemInfo = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == filter.id }
                                        ?: return@detectDragGesturesAfterLongPress
                                    draggedFilterId = filter.id
                                    dragTouchOffsetX = touchOffset.x
                                    dragPointerX = itemInfo.offset + touchOffset.x
                                    activeDragOrder = currentFilters.map(NetloggerFilter::id)
                                    lastMoveTargetId = null
                                    autoScrollDirection = 0
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragPointerX += dragAmount.x

                                    val layoutInfo = listState.layoutInfo
                                    autoScrollDirection = when {
                                        dragPointerX <
                                            layoutInfo.viewportStartOffset + reorderEdgePx -> -1
                                        dragPointerX >
                                            layoutInfo.viewportEndOffset - reorderEdgePx -> 1
                                        else -> 0
                                    }
                                    moveDraggedFilterIfNeeded()
                                },
                                onDragEnd = {
                                    draggedFilterId = null
                                    activeDragOrder = emptyList()
                                    lastMoveTargetId = null
                                    autoScrollDirection = 0
                                    currentOnFilterMoveFinished()
                                },
                                onDragCancel = {
                                    draggedFilterId = null
                                    activeDragOrder = emptyList()
                                    lastMoveTargetId = null
                                    autoScrollDirection = 0
                                    currentOnFilterMoveFinished()
                                }
                            )
                        }
                        .clip(CircleShape)
                        .background(if (selected) NetloggerListColors.Teal else NetloggerListColors.Chip)
                        .border(1.5.dp, NetloggerListColors.Border, CircleShape)
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
internal fun DateHeader(label: String) {
    Text(
        text = label,
        color = Color(0xFF3F4D4F),
        fontSize = 18.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
internal fun LogEntryCard(log: LogEntry, onClick: (LogEntry) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, NetloggerListColors.Border, RoundedCornerShape(8.dp))
            .clickable { onClick(log) }
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        LogBadge(log)
        Spacer(modifier = Modifier.width(10.dp))
        when (log) {
            is LogEntry.Api -> ApiLogContent(log, modifier = Modifier.weight(1f))
            is LogEntry.General -> GeneralLogContent(log, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LogBadge(log: LogEntry) {
    val badge = log.badgeText()
    val isSuccess = badge.startsWith("200")
    val isError = log.isErrorLog()
    val bg =
        if (isSuccess) NetloggerListColors.GreenBg else if (isError) NetloggerListColors.RedBg else NetloggerListColors.BlueBg
    val fg =
        if (isSuccess) NetloggerListColors.GreenText else if (isError) NetloggerListColors.ErrorText else NetloggerListColors.BlueText
    val border =
        if (isSuccess) NetloggerListColors.GreenBorder else if (isError) NetloggerListColors.RedBorder else NetloggerListColors.BlueBorder
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badge,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ApiLogContent(log: LogEntry.Api, modifier: Modifier = Modifier) {
    val urlParts = remember(log.url) { log.url.toUrlParts() }
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MethodPill(log.method)
            Spacer(modifier = Modifier.width(8.dp))
            StartEllipsizedText(
                text = urlParts.path,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    color = if (log.isErrorLog()) NetloggerListColors.ErrorText else NetloggerListColors.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (urlParts.query.isNotEmpty()) {
                Text(
                    text = urlParts.query,
                    color = NetloggerListColors.Muted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NetloggerListColors.QueryBg)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            MetaText("${log.totalDuration}ms")
            Spacer(modifier = Modifier.width(10.dp))
            MetaText(log.timeText())
        }
    }
}

@Composable
private fun GeneralLogContent(log: LogEntry.General, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[${log.tag}]",
                color = if (log.isErrorLog()) NetloggerListColors.ErrorText else NetloggerListColors.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            MetaText(log.timeText())
        }
        Text(
            text = log.message,
            color = Color(0xFF3E494B),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
private fun MethodPill(method: String) {
    Text(
        text = method,
        color = NetloggerListColors.Ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(NetloggerListColors.SurfaceSoft)
            .border(1.dp, NetloggerListColors.Border, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun MetaText(text: String) {
    Text(
        text = text,
        color = NetloggerListColors.Muted,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        maxLines = 1
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun StartEllipsizedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle
) {
    BoxWithConstraints(modifier = modifier) {
        val textMeasurer = rememberTextMeasurer()
        val maxWidthPx = constraints.maxWidth
        val displayText = remember(text, maxWidthPx, style) {
            text.startEllipsizedToFit(maxWidthPx) { candidate ->
                textMeasurer.measure(
                    text = AnnotatedString(candidate),
                    style = style,
                    maxLines = 1
                ).size.width
            }
        }
        Text(
            text = displayText,
            style = style,
            maxLines = 1
        )
    }
}

private fun String.startEllipsizedToFit(maxWidthPx: Int, measureWidth: (String) -> Int): String {
    if (isEmpty() || maxWidthPx <= 0 || measureWidth(this) <= maxWidthPx) return this
    val ellipsis = "..."
    if (measureWidth(ellipsis) > maxWidthPx) return ellipsis

    var low = 0
    var high = length
    var best = ellipsis
    while (low <= high) {
        val keep = (low + high) / 2
        val candidate = ellipsis + takeLast(keep)
        if (measureWidth(candidate) <= maxWidthPx) {
            best = candidate
            low = keep + 1
        } else {
            high = keep - 1
        }
    }
    return best
}

private fun String.toUrlParts(): UrlParts {
    val uri = runCatching { toUri() }.getOrNull()
    val rawPath = uri?.encodedPath.orEmpty().ifBlank { "/" }
    val rawQuery = uri?.encodedQuery.orEmpty()
    return UrlParts(
        path = rawPath,
        query = rawQuery.takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()
    )
}

private data class UrlParts(
    val path: String,
    val query: String
)

private fun LogEntry.badgeText() = when (this) {
    is LogEntry.Api -> if (statusCode in 200..299) "$statusCode\nOK" else "$statusCode\nERR"
    is LogEntry.General -> if (level == LogSeverity.ERROR) "ERROR" else level.name
}

private fun LogEntry.isErrorLog() = when (this) {
    is LogEntry.Api -> statusCode !in 200..299
    is LogEntry.General -> level == LogSeverity.ERROR
}

@Composable
private fun LogEntry.timeText(): String {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    return remember(timestamp) { formatter.format(Date(timestamp)) }
}

@Preview(showBackground = true)
@Composable
private fun NetloggerComponentsPreview1() {
    MaterialTheme {
        NetloggerHeader({}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun NetloggerComponentsPreview2() {
    MaterialTheme {
        NetloggerSearchBar("", {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun NetloggerComponentsPreview3() {
    MaterialTheme {
        FilterChipsRow(
            filters = NetloggerFilter.defaultFilters,
            selectedFilter = NetloggerFilter.ALL,
            onFilterSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetloggerComponentsPreview4() {
    MaterialTheme {
        DateHeader("Today")
    }
}

@Preview(showBackground = true)
@Composable
private fun NetloggerComponentsPreview() {
    MaterialTheme {

        Column {
            sampleLogListItems().filterIsInstance<LogListItem.LogItem>().take(3).forEach {
                LogEntryCard(log = it.log, onClick = {})
            }
        }
    }
}
