package com.netlogger.lib.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.netlogger.lib.data.source.local.dao.LogDao
import com.netlogger.lib.data.source.local.entity.LogEntity

@androidx.room.ConstructedBy(NetloggerDatabaseConstructor::class)
@Database(entities = [LogEntity::class], version = 1, exportSchema = false)
abstract class NetloggerDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
}

@Suppress("KotlinNoActualForExpect")
expect object NetloggerDatabaseConstructor : androidx.room.RoomDatabaseConstructor<NetloggerDatabase> {
    override fun initialize(): NetloggerDatabase
}
