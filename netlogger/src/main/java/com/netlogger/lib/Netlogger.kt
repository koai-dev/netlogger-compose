package com.netlogger.lib

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.netlogger.lib.data.repository.NetloggerRepositoryImpl
import com.netlogger.lib.data.repository.SettingsRepositoryImpl
import com.netlogger.lib.data.source.local.NetloggerDatabase
import com.netlogger.lib.domain.repository.INetloggerRepository
import com.netlogger.lib.domain.repository.SettingsRepository
import com.netlogger.lib.domain.usecase.*
import com.netlogger.lib.presentation.manager.INetloggerManager
import com.netlogger.lib.presentation.manager.NetloggerInterceptor
import com.netlogger.lib.presentation.manager.NetloggerManagerImpl
import com.netlogger.lib.presentation.ui.NetloggerActivity
import com.netlogger.lib.presentation.util.ShakeDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import java.lang.ref.WeakReference

object Netlogger {

    private var shakeDetector: ShakeDetector? = null
    private var sensorManager: SensorManager? = null
    private var currentActivityRef: WeakReference<Activity>? = null
    private var fab: FloatingActionButton? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isShakeEnabled = true
    private var sensitivity = 2.7f
    private var hasAutoResetExecuted = false

    // Manual Dependency Injection Container
    internal lateinit var database: NetloggerDatabase
    internal lateinit var repository: INetloggerRepository
    internal lateinit var settingsRepository: SettingsRepository
    internal lateinit var netloggerManager: INetloggerManager
    
    // Use Cases
    internal lateinit var getSettingsUseCase: GetSettingsUseCase
    internal lateinit var saveSettingsUseCase: SaveSettingsUseCase
    internal lateinit var getLogsUseCase: GetLogsUseCase
    internal lateinit var clearLogsUseCase: ClearLogsUseCase
    internal lateinit var saveApiLogUseCase: SaveApiLogUseCase
    internal lateinit var saveGeneralLogUseCase: SaveGeneralLogUseCase

    /**
     * Khởi tạo module Netlogger. 
     * Tự động handle việc khởi tạo dependencies và đăng ký lắc điện thoại.
     */
    fun init(application: Application) {
        initializeDependencies(application)
        
        checkAutoReset()
        observeSettings()
        setupShakeDetector(application)
    }

    private fun initializeDependencies(context: Context) {
        if (::database.isInitialized) return

        database = Room.databaseBuilder(
            context.applicationContext,
            NetloggerDatabase::class.java,
            "netlogger_database"
        ).fallbackToDestructiveMigration(false).build()

        val logDao = database.logDao()
        repository = NetloggerRepositoryImpl(logDao)
        settingsRepository = SettingsRepositoryImpl(context.applicationContext)

        saveApiLogUseCase = SaveApiLogUseCase(repository)
        saveGeneralLogUseCase = SaveGeneralLogUseCase(repository)
        getLogsUseCase = GetLogsUseCase(repository)
        clearLogsUseCase = ClearLogsUseCase(repository)
        getSettingsUseCase = GetSettingsUseCase(settingsRepository)
        saveSettingsUseCase = SaveSettingsUseCase(settingsRepository)

        val interceptor = NetloggerInterceptor(saveApiLogUseCase)
        netloggerManager = NetloggerManagerImpl(saveGeneralLogUseCase, interceptor)
    }

    private fun checkAutoReset() {
        if (hasAutoResetExecuted) return
        hasAutoResetExecuted = true

        scope.launch {
            val settings = getSettingsUseCase().first()
            if (settings.autoResetOnStart) {
                clearLogsUseCase()
            }
        }
    }

    private fun observeSettings() {
        getSettingsUseCase().onEach { settings ->
            isShakeEnabled = settings.enableShakeDetector
            sensitivity = settings.shakeSensitivity
            shakeDetector?.sensitivity = sensitivity

            // Re-register listener if needed
            currentActivityRef?.get()?.let { activity ->
                if (isShakeEnabled) {
                    registerShakeListener()
                } else {
                    unregisterShakeListener()
                }
            }
        }.launchIn(scope)
    }

    private fun setupShakeDetector(application: Application) {
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector(sensitivity = sensitivity) {
            currentActivityRef?.get()?.let { activity ->
                if (activity !is NetloggerActivity) {
                    val intent = Intent(activity, NetloggerActivity::class.java)
                    activity.startActivity(intent)
                }
            }
        }

        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                currentActivityRef = WeakReference(activity)
                if (isShakeEnabled) {
                    registerShakeListener()
                }
                showFloatingButton(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (currentActivityRef?.get() == activity) {
                    currentActivityRef?.clear()
                    currentActivityRef = null
                }
                unregisterShakeListener()
                hideFloatingButton(activity)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun registerShakeListener() {
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager?.registerListener(
            shakeDetector,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    private fun unregisterShakeListener() {
        sensorManager?.unregisterListener(shakeDetector)
    }

    /**
     * Lấy Interceptor để gắn vào OkHttpClient.
     */
    fun getInterceptor(): Interceptor {
        return netloggerManager.getInterceptor()
    }

    @SuppressLint("RestrictedApi")
    private fun showFloatingButton(activity: Activity) {
        if (fab != null) return

        // Wrap the activity context with a MaterialComponents theme to avoid crash if host app uses a different theme
        val contextWrapper = ContextThemeWrapper(
            activity,
            com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar
        )

        val root = activity.window.decorView as FrameLayout
        fab = FloatingActionButton(contextWrapper).apply {
            setImageResource(android.R.drawable.ic_menu_info_details)
            size = FloatingActionButton.SIZE_NORMAL
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(activity, R.color.netlogger_bg)
            )

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, 64, 64)
            }
            layoutParams = params
            setOnClickListener {
                if (activity !is NetloggerActivity) {
                    val intent = Intent(activity, NetloggerActivity::class.java)
                    activity.startActivity(intent)
                }
            }
        }
        root.addView(fab)
    }

    private fun hideFloatingButton(activity: Activity) {
        fab?.let {
            val root = activity.window.decorView as FrameLayout
            root.removeView(it)
            fab = null
        }
    }
}
