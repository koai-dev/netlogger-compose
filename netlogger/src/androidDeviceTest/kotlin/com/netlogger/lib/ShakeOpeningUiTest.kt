@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.netlogger.lib

import android.app.Application
import android.hardware.SensorEvent
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.test.platform.app.InstrumentationRegistry
import com.netlogger.lib.presentation.util.ShakeDetector
import kotlin.test.Test

/** Uses the installed detector and real lifecycle/presentation; only the accelerometer sample is synthetic. */
class ShakeOpeningUiTest {
    @Test
    fun shakeOpensViewerAndSettingsOptOutStopsFurtherEvents() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        application.getSharedPreferences("netlogger_settings", 0).edit().remove("enable_shake").commit()
        instrumentation.runOnMainSync {
            Netlogger.init(application, NetloggerConfig(allowShakeDetector = true))
        }
        // Android does not expose a public SensorEvent constructor or sensor injection API on device.
        // Reflection is confined to this test; feed the real installed listener rather than opening the Activity directly.
        val detector = Netlogger::class.java.getDeclaredField("shakeDetector").apply { isAccessible = true }
            .get(Netlogger) as ShakeDetector
        val event = SensorEvent::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.newInstance(3)
        event.values.fill(40f)

        runComposeUiTest {
            setContent { Text("Shake UI host") }
            runOnIdle { detector.onSensorChanged(event) }
            onNodeWithText("NetScanner Pro").assertIsDisplayed()
            onNodeWithContentDescription("Settings").performClick()
            onNodeWithTag("netlogger.shake.toggle").performScrollTo().assertIsOn().performClick()
            onNodeWithTag("netlogger.shake.toggle").assertIsOff()
            onNodeWithContentDescription("Back").performClick()
            onNodeWithContentDescription("Close").performClick()
            onNodeWithText("Shake UI host").assertIsDisplayed()
            // Ensure the next synthetic event is outside the detector's debounce period.
            Thread.sleep(600)
            runOnIdle { detector.onSensorChanged(event) }
            onNodeWithText("Shake UI host").assertIsDisplayed()
            onNodeWithText("NetScanner Pro").assertDoesNotExist()
        }
        application.getSharedPreferences("netlogger_settings", 0).edit().remove("enable_shake").commit()
    }
}
