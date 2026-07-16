package com.netlogger.sampleapp.ui

import android.app.Application
import com.netlogger.lib.Netlogger
import com.netlogger.lib.NetloggerConfig
import com.netlogger.lib.domain.model.LogLevel
import com.netlogger.lib.presentation.util.LogUtil

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo Netlogger
        Netlogger.init(this, NetloggerConfig(
            captureBodies = true,
            captureGeneralLogs = true,
            allowShakeDetector = true,
            maximumLogLevel = LogLevel.ALL
        ))
        LogUtil.log("abc", "ấnkdnaksd")
    }
}