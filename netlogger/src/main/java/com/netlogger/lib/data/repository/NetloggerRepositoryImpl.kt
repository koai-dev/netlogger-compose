package com.netlogger.lib.data.repository

import com.netlogger.lib.data.mapper.toDomain
import com.netlogger.lib.data.mapper.toEntity
import com.netlogger.lib.data.source.local.dao.LogDao
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.domain.repository.INetloggerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NetloggerRepositoryImpl(
    private val logDao: LogDao
) : INetloggerRepository {
    
    override suspend fun saveLog(log: LogEntry) {
        logDao.insertLog(log.toEntity())
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
