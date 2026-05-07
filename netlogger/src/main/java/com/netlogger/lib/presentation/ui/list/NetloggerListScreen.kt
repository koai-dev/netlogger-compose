package com.netlogger.lib.presentation.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.netlogger.lib.domain.model.LogEntry

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
        onSettingsClick = onClose,
        onClearLogs = viewModel::clearLogs,
        onSearch = viewModel::search,
        onFilterSelected = viewModel::filterByType
    )
}

@Composable
internal fun NetloggerListContent(
    logs: List<LogListItem>,
    onLogClicked: (LogEntry, String) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit = {},
    onClearLogs: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onFilterSelected: (String?) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(NetloggerFilter.ALL) }
    val gson = remember { Gson() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetloggerListColors.Screen)
    ) {
        NetloggerHeader(
            onClearLogs = onClearLogs,
            onSettingsClick = onSettingsClick
        )
        NetloggerSearchBar(
            query = searchQuery,
            onQueryChanged = {
                searchQuery = it
                onSearch(it)
            },
            onClearQuery = {
                searchQuery = ""
                onSearch("")
            }
        )
        FilterChipsRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { filter ->
                selectedFilter = filter
                onFilterSelected(filter.queryValue)
            }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            items(logs) { item ->
                when (item) {
                    is LogListItem.DateHeader -> DateHeader(item.dateLabel)
                    is LogListItem.LogItem -> LogEntryCard(
                        log = item.log,
                        onClick = { onLogClicked(it, gson.toJson(it)) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }
}


@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NetloggerListContentPreview() {
    MaterialTheme {
        NetloggerListContent(logs = sampleLogListItems())
    }
}
