package com.netlogger.lib.presentation.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object NetloggerListColors {
    val Screen = Color(0xFFF7F8FF)
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

@Composable
internal fun NetloggerHeader(onClearLogs: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(Color(0xFFFAFBFE))
            .border(width = 0.6.dp, color = Color(0xFFDDE4EA))
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TerminalIcon()
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "NetScanner Pro",
            color = NetloggerListColors.Ink,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )
        TrashIcon(modifier = Modifier.clickable(onClick = onClearLogs))
        Spacer(modifier = Modifier.width(24.dp))
        GearIcon(modifier = Modifier.clickable(onClick = onSettingsClick))
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
            .padding(start = 32.dp, top = 24.dp, end = 32.dp)
            .height(76.dp)
            .background(Color.White)
            .border(1.5.dp, NetloggerListColors.Border)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchIcon()
        Spacer(modifier = Modifier.width(20.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            singleLine = true,
            textStyle = TextStyle(
                color = NetloggerListColors.Ink,
                fontSize = 27.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search logs...", color = NetloggerListColors.Gear, fontSize = 27.sp)
                inner()
            }
        )
        ClearIcon(modifier = Modifier.clickable(onClick = onClearQuery))
        Spacer(modifier = Modifier.width(24.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(NetloggerListColors.Border)
        )
        Spacer(modifier = Modifier.width(24.dp))
        FilterIcon()
    }
}

@Composable
internal fun FilterChipsRow(selectedFilter: NetloggerFilter, onFilterSelected: (NetloggerFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, top = 24.dp, end = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NetloggerFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Text(
                text = filter.title,
                color = if (selected) Color.White else Color(0xFF3E494B),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) NetloggerListColors.Teal else NetloggerListColors.Chip)
                    .border(1.5.dp, NetloggerListColors.Border, CircleShape)
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
internal fun DateHeader(label: String) {
    Text(
        text = label,
        color = Color(0xFF3F4D4F),
        fontSize = 34.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 56.dp, bottom = 16.dp)
    )
}

@Composable
internal fun LogEntryCard(log: LogEntry, onClick: (LogEntry) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .height(96.dp)
            .background(Color.White)
            .border(1.5.dp, NetloggerListColors.Border)
            .clickable { onClick(log) }
            .padding(start = 24.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogBadge(log)
        Spacer(modifier = Modifier.width(20.dp))
        LogMainText(log, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = log.timeText(),
            color = NetloggerListColors.Muted,
            fontSize = 24.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun LogBadge(log: LogEntry) {
    val badge = log.badgeText()
    val isSuccess = badge.startsWith("200")
    val isError = log.isErrorLog()
    val bg = if (isSuccess) NetloggerListColors.GreenBg else if (isError) NetloggerListColors.RedBg else Color(0xFFEAF2FF)
    val fg = if (isSuccess) NetloggerListColors.GreenText else if (isError) NetloggerListColors.Red else Color(0xFF244EBC)
    val border = if (isSuccess) NetloggerListColors.GreenBorder else if (isError) NetloggerListColors.RedBorder else Color(0xFFC7DBFF)
    Box(
        modifier = Modifier
            .background(bg)
            .border(1.dp, border)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = badge, color = fg, fontSize = 22.sp, fontFamily = FontFamily.Monospace, lineHeight = 24.sp)
    }
}

@Composable
private fun LogMainText(log: LogEntry, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val leading = log.leadingText()
        Text(
            text = leading,
            color = if (log.isErrorLog()) NetloggerListColors.Red else NetloggerListColors.Ink,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = log.bodyText(),
            color = Color(0xFF3E494B),
            fontSize = 25.sp,
            fontFamily = FontFamily.Monospace,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (log is LogEntry.Api) Text("${log.totalDuration}ms", color = NetloggerListColors.Muted, fontSize = 20.sp)
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

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NetloggerComponentsPreview() {
    MaterialTheme {
        Column(Modifier.background(NetloggerListColors.Screen).padding(16.dp)) {
            NetloggerHeader({}, {})
            NetloggerSearchBar("", {}, {})
            FilterChipsRow(NetloggerFilter.ALL) {}
            DateHeader("Today")
            sampleLogListItems().filterIsInstance<LogListItem.LogItem>().take(3).forEach {
                LogEntryCard(log = it.log, onClick = {})
            }
        }
    }
}
