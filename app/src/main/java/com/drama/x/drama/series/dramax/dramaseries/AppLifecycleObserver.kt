package com.drama.x.drama.series.dramax.dramaseries

import android.os.SystemClock
import android.util.Log
import com.ads.module.ads.ERainAd
import com.drama.x.drama.series.dramax.dramaseries.ads.AdRemoteConfig
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.ads.ResumeAdsEntryRule
import com.drama.x.drama.series.dramax.dramaseries.devconfig.DevConfig

class AppLifecycleObserver {
    private var hasStartedOnce = false
    private var stoppedAtMs = 0L

    fun onMoveToForeground() {
        val activity = GlobalApp.currentActivity
        val stoppedElapsedMs = if (stoppedAtMs > 0L) SystemClock.elapsedRealtime() - stoppedAtMs else 0L
        val suppressResumeAd = AdsManager.consumeSuppressNextResumeInterstitial()
        Log.d(
            "DramaXAds",
            "APP_LIFECYCLE_ON_START hasStartedOnce=$hasStartedOnce stoppedElapsedMs=$stoppedElapsedMs " +
                "suppressResumeAd=$suppressResumeAd"
        )

        if (
            hasStartedOnce &&
            activity != null &&
            stoppedElapsedMs >= 700L &&
            !suppressResumeAd &&
            (
                ResumeAdsEntryRule.shouldShowWelcomeOnResume() ||
                    (
                        DevConfig.isUnlimitedAdsEnabled(activity) &&
                            AdRemoteConfig.interWelcomeBack.id.isNotBlank()
                    )
                )
        ) {
            val config = AdRemoteConfig.interWelcomeBack
            val sdkAllowed = DevConfig.isUnlimitedAdsEnabled(activity) ||
                !config.enableUaCheck ||
                ERainAd.getInstance().getShouldDisplayInterWelcomeBack()
            Log.d(
                "DramaXAds",
                "ERAIN_GATE inter_welcome_back enableUaCheck=${config.enableUaCheck} sdkShouldDisplay=$sdkAllowed"
            )
            if (sdkAllowed) {
                AdsManager.loadInterWelcome(
                    activity = activity,
                    onLoaded = {
                        AdsManager.showInterWelcome(activity)
                    }
                )
            }
        }

        hasStartedOnce = true
        stoppedAtMs = 0L
    }

    fun onMoveToBackground() {
        stoppedAtMs = SystemClock.elapsedRealtime()
        Log.d("DramaXAds", "APP_LIFECYCLE_ON_STOP stoppedAtMs=$stoppedAtMs")
    }
}
