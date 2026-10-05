package com.netlogger.lib

import com.netlogger.lib.data.repository.NetloggerRepositoryImpl
import com.netlogger.lib.domain.model.LogEntry
import com.netlogger.lib.platform.currentTimeMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.test.*

class IosStorageTest {
    @Test fun roomDatabaseStoresPrunesAndClearsLogs() = runBlocking {
        val db = createIosDatabase(NetloggerConfig())
        try {
            val repo = NetloggerRepositoryImpl(db.logDao(), maxLogEntries = 2, retentionMillis = 60_000)
            val now = currentTimeMillis()
            repo.saveLog(LogEntry.General(timestamp = now - 120_000, tag = "test", message = "expired"))
            for (i in 0..2) repo.saveLog(LogEntry.General(timestamp = now + i, tag = "test", message = "item-$i"))
            val logs = withTimeout(5_000) { repo.getAllLogs().first() }
            assertEquals(listOf("item-2", "item-1"), logs.map { (it as LogEntry.General).message })
            assertTrue(logs.all { it.id > 0 })
            repo.clearLogs()
            assertTrue(repo.getAllLogs().first().isEmpty())
        } finally { db.close() }
    }
}
