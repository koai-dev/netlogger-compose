@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.netlogger.lib

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.netlogger.lib.data.repository.NetloggerRepositoryImpl
import com.netlogger.lib.data.repository.SettingsRepositoryImpl
import com.netlogger.lib.data.source.local.NetloggerDatabase
import com.netlogger.lib.domain.model.LogSeverity
import com.netlogger.lib.domain.usecase.*
import com.netlogger.lib.network.netlogger
import com.netlogger.lib.presentation.ui.list.NetloggerListViewModel
import com.netlogger.lib.presentation.ui.settings.NetloggerSettingsViewModel
import com.netlogger.lib.presentation.util.NetloggerConsoleLogger
import com.netlogger.lib.presentation.util.NetloggerRedactor
import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import platform.Foundation.*

/** iOS entry point. Initialize once on the main thread before constructing a client or UI. */
actual object Netlogger {
    private var initialized = false
    internal var configuration = NetloggerConfig()
        private set
    private lateinit var repository: NetloggerRepositoryImpl
    private lateinit var settings: SettingsRepositoryImpl
    private lateinit var redactor: NetloggerRedactor
    private lateinit var console: NetloggerConsoleLogger
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val queue = Channel<LogEntryToSave>(64, BufferOverflow.DROP_OLDEST)

    init {
        scope.launch {
            for (log in queue) {
                runCatching { console.logGeneral(log.tag, log.message, log.level) }
                runCatching { SaveGeneralLogUseCase(repository)(log.tag, log.message, log.level) }
            }
        }
    }

    fun init(config: NetloggerConfig = NetloggerConfig()) {
        check(NSThread.isMainThread) { "Initialize Netlogger on the main thread" }
        if (initialized) {
            check(configuration == config) { "Netlogger is already initialized with a different configuration" }
            return
        }
        val db = createIosDatabase(config)
        repository = NetloggerRepositoryImpl(db.logDao(), config.maxLogEntries, config.retentionMillis)
        settings = SettingsRepositoryImpl(config)
        redactor = NetloggerRedactor(config)
        console = NetloggerConsoleLogger(config, redactor)
        configuration = config
        initialized = true
        if (settings.getCurrentSettings().autoResetOnStart) scope.launch { repository.clearLogs() }
    }

    actual fun isInitialized(): Boolean = initialized
    actual fun configureClient(client: HttpClientConfig<*>) {
        check(initialized) { "Call Netlogger.init first" }
        client.netlogger(configuration, settings, repository)
    }
    internal fun listViewModel() = NetloggerListViewModel(GetLogsUseCase(repository), ClearLogsUseCase(repository), configuration.tagTabs, settings)
    internal fun settingsViewModel() = NetloggerSettingsViewModel(GetSettingsUseCase(settings), SaveSettingsUseCase(settings))
    internal fun log(tag: String, message: String, level: LogSeverity) {
        if (!initialized || !configuration.captureGeneralLogs) return
        queue.trySend(LogEntryToSave(redactor.redactText(tag.take(128)).take(128),
            redactor.redactText(message.take(configuration.maxGeneralMessageChars)).take(configuration.maxGeneralMessageChars), level))
    }
    private data class LogEntryToSave(val tag: String, val message: String, val level: LogSeverity)
}

internal fun createIosDatabase(config: NetloggerConfig): NetloggerDatabase {
    val builder = if (config.storage == NetloggerStorage.MEMORY_ONLY)
        Room.inMemoryDatabaseBuilder<NetloggerDatabase>()
    else Room.databaseBuilder<NetloggerDatabase>(name = persistentDatabasePath())
    return builder.setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default).build()
}

private fun persistentDatabasePath(): String {
    val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    val directory = "$base/Netlogger"
    check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null))
    val url = NSURL.fileURLWithPath(directory, isDirectory = true)
    check(url.setResourceValue(NSNumber(bool = true), NSURLIsExcludedFromBackupKey, null))
    return "$directory/netlogger_database.db"
}
