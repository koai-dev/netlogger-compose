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
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.netlogger.lib.presentation.di.netloggerModule
import com.netlogger.lib.presentation.manager.INetloggerManager
import com.netlogger.lib.presentation.ui.NetloggerActivity
import com.netlogger.lib.presentation.util.ShakeDetector
import okhttp3.Interceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import java.lang.ref.WeakReference

object Netlogger {

    private var shakeDetector: ShakeDetector? = null
    private var sensorManager: SensorManager? = null
    private var currentActivityRef: WeakReference<Activity>? = null
    private var fab: FloatingActionButton? = null

    /**
     * Khởi tạo module Netlogger. 
     * Tự động handle việc load module Koin và đăng ký lắc điện thoại.
     */
    fun init(application: Application) {
        try {
            startKoin {
                androidContext(application)
                modules(netloggerModule)
            }
        } catch (e: Exception) {
            loadKoinModules(netloggerModule)
        }

        setupShakeDetector(application)
    }

    private fun setupShakeDetector(application: Application) {
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector {
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
                val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                sensorManager?.registerListener(
                    shakeDetector,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
                )

                showFloatingButton(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (currentActivityRef?.get() == activity) {
                    currentActivityRef?.clear()
                    currentActivityRef = null
                }
                sensorManager?.unregisterListener(shakeDetector)
                hideFloatingButton(activity)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /**
     * Lấy Interceptor để gắn vào OkHttpClient.
     */
    fun getInterceptor(): Interceptor {
        val manager = GlobalContext.get().get<INetloggerManager>()
        return manager.getInterceptor()
    }

    @SuppressLint("RestrictedApi")
    private fun showFloatingButton(activity: Activity) {
        if (fab != null) return

        val root = activity.window.decorView as FrameLayout
        fab = FloatingActionButton(activity).apply {
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
