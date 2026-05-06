package com.netlogger.lib.presentation.ui.list

import com.netlogger.lib.domain.model.LogEntry

/**
 * Presentation model for items displayed in the log list RecyclerView.
 * Supports date-header grouping alongside individual log entries.
 */
sealed class LogListItem {
    data class DateHeader(val dateLabel: String) : LogListItem()
    data class LogItem(val log: LogEntry) : LogListItem()
}
