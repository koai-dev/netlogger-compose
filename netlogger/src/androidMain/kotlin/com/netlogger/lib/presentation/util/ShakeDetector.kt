package com.netlogger.lib.presentation.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    var sensitivity: Float = 2.7f,
    private val onShake: () -> Unit
) : SensorEventListener {

    companion object {
        private const val SHAKE_SLOP_TIME_MS = 500
    }

    private var mShakeTimestamp: Long = 0

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Không dùng
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > sensitivity) {
            val now = System.currentTimeMillis()
            if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return
            }
            mShakeTimestamp = now
            onShake()
        }
    }
}
