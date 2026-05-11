package com.netlogger.lib.presentation.ui.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netlogger.lib.R
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.presentation.ui.components.NetloggerIconButton
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetloggerHeader(onClearLogs: () -> Unit, onSettingsClick: () -> Unit) {
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
    onClearQuery: () -> Unit
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
            onClick = {
                //TODO: handle filter click
            }
        )
    }
}

@Composable
internal fun FilterChipsRow(
    selectedFilter: NetloggerFilter,
    onFilterSelected: (NetloggerFilter) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NetloggerFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Text(
                text = filter.title,
                color = if (selected) Color.White else Color(0xFF3E494B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) NetloggerListColors.Teal else NetloggerListColors.Chip)
                    .border(1.5.dp, NetloggerListColors.Border, CircleShape)
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
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
            .padding(vertical = 4.dp)
            .background(Color.White)
            .border(1.5.dp, NetloggerListColors.Border)
            .clickable { onClick(log) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogBadge(log)
        Spacer(modifier = Modifier.width(16.dp))
        LogMainText(log, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = log.timeText(),
            color = NetloggerListColors.Muted,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun LogBadge(log: LogEntry) {
    val badge = log.badgeText()
    val isSuccess = badge.startsWith("200")
    val isError = log.isErrorLog()
    val bg =
        if (isSuccess) NetloggerListColors.GreenBg else if (isError) NetloggerListColors.RedBg else Color(
            0xFFEAF2FF
        )
    val fg =
        if (isSuccess) NetloggerListColors.GreenText else if (isError) NetloggerListColors.Red else Color(
            0xFF244EBC
        )
    val border =
        if (isSuccess) NetloggerListColors.GreenBorder else if (isError) NetloggerListColors.RedBorder else Color(
            0xFFC7DBFF
        )
    Box(
        modifier = Modifier
            .background(bg)
            .border(1.dp, border)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badge,
            color = fg,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LogMainText(log: LogEntry, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val leading = log.leadingText()
        Text(
            text = leading,
            color = if (log.isErrorLog()) NetloggerListColors.Red else NetloggerListColors.Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = log.bodyText(),
            color = Color(0xFF3E494B),
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (log is LogEntry.Api) Text(
            "${log.totalDuration}ms",
            color = NetloggerListColors.Muted,
            fontSize = 14.sp
        )
    }
}

private fun LogEntry.badgeText() = when (this) {
    is LogEntry.Api -> if (statusCode in 200..299) "200\nOK" else "$statusCode\nERR"
    is LogEntry.General -> if (level == LogLevel.ERROR) "ERROR" else level.name
}

private fun LogEntry.leadingText() = when (this) {
    is LogEntry.Api -> method
    is LogEntry.General -> "[$tag]"
}

private fun LogEntry.bodyText() = when (this) {
    is LogEntry.Api -> url
    is LogEntry.General -> message
}

private fun LogEntry.isErrorLog() = when (this) {
    is LogEntry.Api -> statusCode !in 200..299
    is LogEntry.General -> level == LogLevel.ERROR
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
        FilterChipsRow(NetloggerFilter.ALL) {}
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