@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.netlogger.lib.presentation.ui

import kotlin.native.ref.WeakReference
import platform.Foundation.NSProcessInfo
import platform.UIKit.*

/** Main-thread only. Uses the event's own window, including in applications with multiple scenes. */
internal class IosShakePresenter {
    private var lastPresentation = Double.NEGATIVE_INFINITY

    fun present(window: UIWindow, debounce: Boolean = true): Boolean {
        val scene = window.windowScene
        if (window.hidden || !window.isKeyWindow() ||
            (scene != null && scene.activationState != UISceneActivationStateForegroundActive) ||
            (scene == null && UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateActive)
        ) return false

        var top = window.rootViewController ?: return false
        while (true) {
            if (top is NetloggerNavigationController) return true
            if (top.isBeingDismissed() || top.isBeingPresented()) return false
            top = top.presentedViewController ?: break
        }
        if (top is UIAlertController) return false
        val now = NSProcessInfo.processInfo.systemUptime
        if (debounce && now - lastPresentation < 0.5) return false

        lateinit var viewerRef: WeakReference<UIViewController>
        val viewer = NetloggerViewController {
            viewerRef.get()?.dismissViewControllerAnimated(true, completion = null)
        }
        viewerRef = WeakReference(viewer)
        lastPresentation = now
        top.presentViewController(viewer, animated = true, completion = null)
        return true
    }
}
