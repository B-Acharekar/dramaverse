package com.drama.x.drama.series.dramax.dramaseries

import android.app.Activity
import android.app.Application
import android.os.Bundle

class AppActivityLifecycleCallbacks(
    private val appLifecycleObserver: AppLifecycleObserver? = null
) : Application.ActivityLifecycleCallbacks {
    private var startedCount = 0

    override fun onActivityStarted(activity: Activity) {
        GlobalApp.currentActivity = activity
        startedCount++
        if (startedCount == 1) {
            appLifecycleObserver?.onMoveToForeground()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        GlobalApp.currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (GlobalApp.currentActivity === activity) {
            GlobalApp.currentActivity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStopped(activity: Activity) {
        startedCount = (startedCount - 1).coerceAtLeast(0)
        if (startedCount == 0) {
            appLifecycleObserver?.onMoveToBackground()
        }
    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (GlobalApp.currentActivity === activity) {
            GlobalApp.currentActivity = null
        }
    }
}
