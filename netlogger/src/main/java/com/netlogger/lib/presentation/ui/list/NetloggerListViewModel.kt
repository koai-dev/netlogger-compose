package com.netlogger.lib.presentation.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.usecase.ClearLogsUseCase
import com.netlogger.lib.domain.usecase.GetLogsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NetloggerListViewModel(
    private val getLogsUseCase: GetLogsUseCase,
    private val clearLogsUseCase: ClearLogsUseCase
) : ViewModel() {

    private val _logs = MutableStateFlow<List<LogListItem>>(emptyList())
    val logs: StateFlow<List<LogListItem>> = _logs

    private var allLogs: List<LogEntry> = emptyList()
    private var currentQuery = ""
    private var currentTypeFilter: String? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    init {
        loadLogs()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            getLogsUseCase().collectLatest { data ->
                allLogs = data
                applyFilters()
            }
        }
    }

    fun search(query: String) {
        currentQuery = query.lowercase()
        applyFilters()
    }

    fun filterByType(type: String?) {
        currentTypeFilter = type
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = allLogs
        if (currentTypeFilter != null && currentTypeFilter != "ALL") {
            filtered = filtered.filter { it.type.name == currentTypeFilter }
        }
        if (currentQuery.isNotBlank()) {
            filtered = filtered.filter { log ->
                when (log) {
                    is LogEntry.Api -> {
                        log.url.lowercase().contains(currentQuery) ||
                        log.method.lowercase().contains(currentQuery)
                    }
                    is LogEntry.General -> {
                        log.message.lowercase().contains(currentQuery) ||
                        log.tag.lowercase().contains(currentQuery)
                    }
                }
            }
        }
        _logs.value = groupByDate(filtered)
    }

    private fun groupByDate(logs: List<LogEntry>): List<LogListItem> {
        if (logs.isEmpty()) return emptyList()

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val logCalendar = Calendar.getInstance()

        val result = mutableListOf<LogListItem>()
        var lastDateLabel: String? = null

        for (log in logs) {
            logCalendar.time = Date(log.timestamp)

            val label = when {
                isSameDay(logCalendar, today) -> "Today"
                isSameDay(logCalendar, yesterday) -> "Yesterday"
                else -> dateFormat.format(Date(log.timestamp))
            }

            if (label != lastDateLabel) {
                result.add(LogListItem.DateHeader(label))
                lastDateLabel = label
            }
            result.add(LogListItem.LogItem(log))
        }
        return result
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun clearLogs() {
        viewModelScope.launch {
            clearLogsUseCase()
        }
    }
}
