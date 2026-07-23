package com.netlogger.lib.presentation.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.model.LogSeverity
import com.netlogger.lib.domain.usecase.ClearLogsUseCase
import com.netlogger.lib.domain.usecase.GetLogsUseCase
import com.netlogger.lib.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NetloggerListViewModel(
    private val getLogsUseCase: GetLogsUseCase,
    private val clearLogsUseCase: ClearLogsUseCase,
    tagTabs: List<String> = emptyList()
) : ViewModel() {

    val tagTabs: List<String> = tagTabs.filter(String::isNotBlank).distinct()

    private val _logs = MutableStateFlow<List<LogListItem>>(emptyList())
    val logs: StateFlow<List<LogListItem>> = _logs

    private var allLogs: List<LogEntry> = emptyList()
    private var currentQuery = ""
    private var currentTypeFilter: String? = null
    private var currentTagFilter: String? = null
    
    // Advanced filters
    private var selectedMethods: Set<String> = emptySet()
    private var selectedStatusGroups: Set<String> = emptySet()

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
        currentTagFilter = null
        applyFilters()
    }

    fun filterByTag(tag: String) {
        currentTypeFilter = null
        currentTagFilter = tag
        applyFilters()
    }
    
    fun applyAdvancedFilters(methods: Set<String>, statusGroups: Set<String>) {
        selectedMethods = methods
        selectedStatusGroups = statusGroups
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = filterByQuickFilter(
            logs = allLogs,
            type = currentTypeFilter,
            tag = currentTagFilter
        )
        
        // Search query
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
        
        // Method filter
        if (selectedMethods.isNotEmpty()) {
            filtered = filtered.filter { log ->
                log is LogEntry.Api && selectedMethods.contains(log.method)
            }
        }
        
        // Status filter
        if (selectedStatusGroups.isNotEmpty()) {
            filtered = filtered.filter { log ->
                if (log is LogEntry.Api) {
                    val code = log.statusCode
                    when {
                        code in 200..299 -> selectedStatusGroups.contains("2xx Success")
                        code in 300..399 -> selectedStatusGroups.contains("3xx Redirection")
                        code in 400..499 -> selectedStatusGroups.contains("4xx Client Error")
                        code in 500..599 -> selectedStatusGroups.contains("5xx Server Error")
                        else -> false
                    }
                } else false
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
    
    fun getSelectedMethods() = selectedMethods
    fun getSelectedStatusGroups() = selectedStatusGroups
}

internal fun filterByQuickFilter(
    logs: List<LogEntry>,
    type: String?,
    tag: String?
): List<LogEntry> {
    if (tag != null) {
        return logs.filter { log ->
            log is LogEntry.General && log.tag == tag
        }
    }

    if (type == null || type == "ALL") return logs

    return when (type) {
        "ERROR" -> logs.filter { log ->
            when (log) {
                is LogEntry.Api -> log.statusCode !in 200..299
                is LogEntry.General -> log.level == LogSeverity.ERROR
            }
        }
        else -> logs.filter { it.type.name == type }
    }
}
