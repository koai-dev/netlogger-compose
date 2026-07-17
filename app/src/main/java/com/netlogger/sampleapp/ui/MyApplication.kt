package com.netlogger.sampleapp.ui

import android.app.Application
import com.netlogger.lib.Netlogger
import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.presentation.util.LogUtil
import com.netlogger.sampleapp.BuildConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo Netlogger
        Netlogger.init(this, NetloggerConfig(
            captureBodies = true,
            captureGeneralLogs = true,
            allowShakeDetector = true,
            maximumLogLevel = LogLevel.ALL,
            enableLogcatOutput = BuildConfig.DEBUG
        ))
        LogUtil.log("abc", "ấnkdnaksd")
    }
}