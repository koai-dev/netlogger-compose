package com.netlogger.lib.domain.repository

import com.netlogger.lib.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface INetloggerRepository {
    suspend fun saveLog(log: LogEntry)
    fun getAllLogs(): Flow<List<LogEntry>>
    fun getLogsByType(type: String): Flow<List<LogEntry>>
    suspend fun clearLogs()
}
