package com.netlogger.lib.presentation.util

import okhttp3.HttpUrl

internal fun NetloggerRedactor.redactUrl(url: HttpUrl): String = redactUrl(url.toString())
