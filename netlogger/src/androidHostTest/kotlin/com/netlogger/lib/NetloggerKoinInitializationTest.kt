package com.netlogger.lib

import android.app.Application
import com.netlogger.lib.di.NETLOGGER_CONFIG_QUALIFIER
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class NetloggerKoinInitializationTest {

    @Before
    fun setUp() {
        stopKoin()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `starts global Koin when host has not started it`() {
        val config = NetloggerConfig()

        val installedKoin = Netlogger.installNetloggerKoin(Application(), config)

        assertSame(installedKoin, GlobalContext.get())
        assertEquals(
            config,
            installedKoin.get<NetloggerConfig>(NETLOGGER_CONFIG_QUALIFIER)
        )
    }

    @Test
    fun `loads module into existing Koin without replacing host container`() {
        val marker = HostMarker()
        val hostKoin = startKoin {
            modules(module { single { marker } })
        }.koin
        val config = NetloggerConfig()

        val installedKoin = Netlogger.installNetloggerKoin(Application(), config)

        assertSame(hostKoin, installedKoin)
        assertSame(marker, installedKoin.get<HostMarker>())
        assertEquals(
            config,
            installedKoin.get<NetloggerConfig>(NETLOGGER_CONFIG_QUALIFIER)
        )
    }

    private class HostMarker
}
