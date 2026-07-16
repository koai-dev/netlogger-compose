package com.netlogger.lib.data.repository

import com.netlogger.lib.data.mapper.toDomain
import com.netlogger.lib.data.mapper.toEntity
import com.netlogger.lib.data.source.local.dao.LogDao
import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.repository.INetloggerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NetloggerRepositoryImpl(
    private val logDao: LogDao,
    private val maxLogEntries: Int,
    private val retentionMillis: Long
) : INetloggerRepository {

    constructor(logDao: LogDao) : this(
        logDao = logDao,
        maxLogEntries = NetloggerConfig.DEFAULT_MAX_LOG_ENTRIES,
        retentionMillis = NetloggerConfig.DEFAULT_RETENTION_MILLIS
    )
    
    override suspend fun saveLog(log: LogEntry) {
        val cutoffTimestamp = (System.currentTimeMillis() - retentionMillis).coerceAtLeast(0L)
        logDao.insertAndPrune(log.toEntity(), cutoffTimestamp, maxLogEntries)
    }

    override fun getAllLogs(): Flow<List<LogEntry>> {
        return logDao.getAllLogs().map { list -> list.map { it.toDomain() } }
    }

    override fun getLogsByType(type: String): Flow<List<LogEntry>> {
        return logDao.getLogsByType(type).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun clearLogs() {
        logDao.clearLogs()
    }
}
