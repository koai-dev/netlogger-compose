@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.netlogger.lib.presentation.ui

import com.netlogger.lib.Netlogger
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.UIKit.UIEvent
import platform.UIKit.UIEventSubtype
import platform.UIKit.UIEventSubtypeMotionShake
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.UIScreen

/**
 * Host window that forwards UIKit shake gestures to Netlogger without taking first responder
 * away from text fields. Opt in with NetloggerConfig.allowShakeDetector before using it.
 * Hosts with their own UIWindow subclass can forward motionEnded to Netlogger.handleShake instead.
 */
fun createNetloggerWindow(): UIWindow = ShakeWindow(UIScreen.mainScreen.bounds)

/** Scene-based variant for apps using UISceneDelegate. */
fun createNetloggerWindow(windowScene: UIWindowScene): UIWindow = ShakeWindow(windowScene)

internal class ShakeWindow : UIWindow {
    constructor(frame: CValue<CGRect>) : super(frame = frame)
    constructor(windowScene: UIWindowScene) : super(windowScene = windowScene)

    override fun motionEnded(motion: UIEventSubtype, withEvent: UIEvent?) {
        if (motion == UIEventSubtypeMotionShake && Netlogger.handleShake(this)) return
        super.motionEnded(motion, withEvent)
    }
}
