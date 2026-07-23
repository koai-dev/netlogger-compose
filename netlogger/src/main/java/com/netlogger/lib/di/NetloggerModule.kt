package com.netlogger.lib.di

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.NetloggerStorage
import com.netlogger.lib.data.repository.NetloggerRepositoryImpl
import com.netlogger.lib.data.repository.SettingsRepositoryImpl
import com.netlogger.lib.data.source.local.NetloggerDatabase
import com.netlogger.lib.data.source.local.dao.LogDao
import com.netlogger.lib.domain.repository.INetloggerRepository
import com.netlogger.lib.domain.repository.SettingsRepository
import com.netlogger.lib.domain.usecase.ClearLogsUseCase
import com.netlogger.lib.domain.usecase.GetLogsUseCase
import com.netlogger.lib.domain.usecase.GetSettingsUseCase
import com.netlogger.lib.domain.usecase.SaveApiLogUseCase
import com.netlogger.lib.domain.usecase.SaveGeneralLogUseCase
import com.netlogger.lib.domain.usecase.SaveSettingsUseCase
import com.netlogger.lib.presentation.manager.INetloggerManager
import com.netlogger.lib.presentation.manager.NetloggerInterceptor
import com.netlogger.lib.presentation.manager.NetloggerManagerImpl
import com.netlogger.lib.presentation.ui.list.NetloggerListViewModel
import com.netlogger.lib.presentation.ui.settings.NetloggerSettingsViewModel
import com.netlogger.lib.presentation.util.NetloggerConsoleLogger
import com.netlogger.lib.presentation.util.NetloggerRedactor
import org.koin.core.module.Module
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

internal val NETLOGGER_CONFIG_QUALIFIER = named("com.netlogger.lib.configuration")

internal fun createNetloggerModule(
    application: Application,
    config: NetloggerConfig
): Module = module {
    single<NetloggerConfig>(NETLOGGER_CONFIG_QUALIFIER) { config }

    single<NetloggerDatabase> {
        createDatabase(
            context = application.applicationContext,
            config = get(NETLOGGER_CONFIG_QUALIFIER)
        )
    } withOptions {
        onClose { database -> database?.close() }
    }
    single<LogDao> { get<NetloggerDatabase>().logDao() }

    single<INetloggerRepository> {
        val currentConfig = get<NetloggerConfig>(NETLOGGER_CONFIG_QUALIFIER)
        NetloggerRepositoryImpl(
            logDao = get(),
            maxLogEntries = currentConfig.maxLogEntries,
            retentionMillis = currentConfig.retentionMillis
        )
    }
    single<SettingsRepository> {
        val currentConfig = get<NetloggerConfig>(NETLOGGER_CONFIG_QUALIFIER)
        SettingsRepositoryImpl(
            context = application.applicationContext,
            maximumLogLevel = currentConfig.maximumLogLevel,
            allowShakeDetector = currentConfig.allowShakeDetector,
            allowFloatingButton = currentConfig.allowFloatingButton
        )
    }

    single { SaveApiLogUseCase(get()) }
    single { SaveGeneralLogUseCase(get()) }
    single { GetLogsUseCase(get()) }
    single { ClearLogsUseCase(get()) }
    single { GetSettingsUseCase(get()) }
    single { SaveSettingsUseCase(get()) }

    single {
        NetloggerRedactor(get(NETLOGGER_CONFIG_QUALIFIER))
    }
    single {
        NetloggerConsoleLogger(
            config = get(NETLOGGER_CONFIG_QUALIFIER),
            redactor = get()
        )
    }
    single {
        val settingsRepository = get<SettingsRepository>()
        NetloggerInterceptor(
            saveApiLogUseCase = get(),
            getSettingsUseCase = get(),
            config = get(NETLOGGER_CONFIG_QUALIFIER),
            redactor = get(),
            initialLogLevel = settingsRepository.getCurrentSettings().logLevel
        )
    } withOptions {
        onClose { interceptor -> interceptor?.close() }
    }
    single<INetloggerManager> {
        val currentConfig = get<NetloggerConfig>(NETLOGGER_CONFIG_QUALIFIER)
        NetloggerManagerImpl(
            saveGeneralLogUseCase = get(),
            interceptor = get(),
            redactor = get(),
            maxGeneralMessageChars = currentConfig.maxGeneralMessageChars,
            captureGeneralLogs = currentConfig.captureGeneralLogs,
            consoleLogger = get()
        )
    } withOptions {
        onClose { manager -> (manager as? NetloggerManagerImpl)?.close() }
    }

    viewModel {
        val currentConfig = get<NetloggerConfig>(NETLOGGER_CONFIG_QUALIFIER)
        NetloggerListViewModel(
            getLogsUseCase = get(),
            clearLogsUseCase = get(),
            tagTabs = currentConfig.tagTabs
        )
    }
    viewModel { NetloggerSettingsViewModel(get(), get()) }
}

private fun createDatabase(context: Context, config: NetloggerConfig): NetloggerDatabase =
    when (config.storage) {
        NetloggerStorage.MEMORY_ONLY -> Room.inMemoryDatabaseBuilder(
            context,
            NetloggerDatabase::class.java
        ).build()

        NetloggerStorage.PERSISTENT_NO_BACKUP -> Room.databaseBuilder(
            NoBackupDatabaseContext(context),
            NetloggerDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

private class NoBackupDatabaseContext(base: Context) : ContextWrapper(base) {
    override fun getDatabasePath(name: String): File =
        File(noBackupFilesDir, "netlogger/$name").also { databaseFile ->
            databaseFile.parentFile?.mkdirs()
        }

    override fun deleteDatabase(name: String): Boolean =
        SQLiteDatabase.deleteDatabase(getDatabasePath(name))
}

private const val DATABASE_NAME = "netlogger_database.db"
