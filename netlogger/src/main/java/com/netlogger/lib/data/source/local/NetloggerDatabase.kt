package com.netlogger.lib.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.netlogger.lib.data.source.local.dao.LogDao
import com.netlogger.lib.data.source.local.entity.LogEntity

@Database(entities = [LogEntity::class], version = 1, exportSchema = false)
abstract class NetloggerDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
}
