package com.netlogger.lib.presentation.di

import androidx.room.Room
import com.netlogger.lib.data.repository.NetloggerRepositoryImpl
import com.netlogger.lib.data.source.local.NetloggerDatabase
import com.netlogger.lib.domain.repository.INetloggerRepository
import com.netlogger.lib.domain.usecase.SaveApiLogUseCase
import com.netlogger.lib.domain.usecase.SaveGeneralLogUseCase
import com.netlogger.lib.presentation.manager.INetloggerManager
import com.netlogger.lib.presentation.manager.NetloggerInterceptor
import com.netlogger.lib.presentation.manager.NetloggerManagerImpl
import com.netlogger.lib.presentation.ui.list.NetloggerListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val netloggerModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            NetloggerDatabase::class.java,
            "netlogger_database"
        ).fallbackToDestructiveMigration(false).build()
    }

    single { get<NetloggerDatabase>().logDao() }

    // Repository
    single<INetloggerRepository> { NetloggerRepositoryImpl(get()) }
    single<com.netlogger.lib.domain.repository.SettingsRepository> { com.netlogger.lib.data.repository.SettingsRepositoryImpl(get()) }

    // UseCases
    factory { SaveApiLogUseCase(get()) }
    factory { SaveGeneralLogUseCase(get()) }
    factory { com.netlogger.lib.domain.usecase.GetLogsUseCase(get()) }
    factory { com.netlogger.lib.domain.usecase.ClearLogsUseCase(get()) }
    factory { com.netlogger.lib.domain.usecase.GetSettingsUseCase(get()) }
    factory { com.netlogger.lib.domain.usecase.SaveSettingsUseCase(get()) }

    // Interceptor & Manager
    single { NetloggerInterceptor(get()) }
    single<INetloggerManager> { NetloggerManagerImpl(get(), get()) }

    // ViewModel
    viewModel { NetloggerListViewModel(get(), get(), get()) }
    viewModel { com.netlogger.lib.presentation.ui.settings.NetloggerSettingsViewModel(get(), get()) }
}

