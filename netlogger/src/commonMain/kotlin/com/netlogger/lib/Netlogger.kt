package com.netlogger.lib

import io.ktor.client.HttpClientConfig

/** Initialize in the platform host, then configure HTTP clients from shared Kotlin code. */
expect object Netlogger {
    fun isInitialized(): Boolean
    fun configureClient(client: HttpClientConfig<*>)
}
