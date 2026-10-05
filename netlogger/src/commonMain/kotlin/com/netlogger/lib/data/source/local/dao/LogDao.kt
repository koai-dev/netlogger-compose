package com.netlogger.lib.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.netlogger.lib.data.source.local.entity.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM netlogger_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query(
        "DELETE FROM netlogger_logs WHERE id NOT IN " +
            "(SELECT id FROM netlogger_logs ORDER BY timestamp DESC, id DESC LIMIT :maxEntries)"
    )
    suspend fun trimToMaxEntries(maxEntries: Int)

    @Transaction
    suspend fun insertAndPrune(log: LogEntity, cutoffTimestamp: Long, maxEntries: Int) {
        insertLog(log)
        deleteOlderThan(cutoffTimestamp)
        trimToMaxEntries(maxEntries)
    }

    @Query("SELECT * FROM netlogger_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT * FROM netlogger_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: String): Flow<List<LogEntity>>

    @Query("DELETE FROM netlogger_logs")
    suspend fun clearLogs()
}
