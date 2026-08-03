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
    private var hasBeenBackgroundedBefore = false
    /** Duration the app was in background on the last stop/start cycle. Set in onMoveToForeground before stoppedAtMs is cleared. */
    private var lastStoppedElapsedMs = 0L

    /**
     * Returns true once if the app genuinely came back from background.
     * "Genuine" means the app was backgrounded for at least [minElapsedMs] milliseconds.
     * Brief system stops (permission dialogs, notifications) during fresh launch are < 3s and are excluded.
     * Resets flag on return of true.
     */
    fun consumeHasBeenBackgrounded(minElapsedMs: Long = 700L): Boolean {
        if (!hasBeenBackgroundedBefore) return false
        if (lastStoppedElapsedMs < minElapsedMs) {
            Log.d("DramaXAds", "consumeHasBeenBackgrounded skipped brief_background lastStoppedElapsedMs=$lastStoppedElapsedMs")
            return false
        }
        hasBeenBackgroundedBefore = false
        return true
    }

    fun onMoveToForeground() {
        val activity = GlobalApp.currentActivity
        // Capture elapsed BEFORE clearing stoppedAtMs so consumeHasBeenBackgrounded can use it
        lastStoppedElapsedMs = if (stoppedAtMs > 0L) SystemClock.elapsedRealtime() - stoppedAtMs else 0L
        val suppressResumeAd = AdsManager.consumeSuppressNextResumeInterstitial()
        Log.d(
            "DramaXAds",
            "APP_LIFECYCLE_ON_START hasStartedOnce=$hasStartedOnce stoppedElapsedMs=$lastStoppedElapsedMs " +
                "suppressResumeAd=$suppressResumeAd"
        )

        if (
            hasStartedOnce &&
            activity != null &&
            lastStoppedElapsedMs >= 700L &&
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
                ERainAd.getInstance().getShouldDisplayInterWelcomeBack(config.enableUaCheck)
            Log.d(
                "DramaXAds",
                "ERAIN_GATE inter_welcome_back enableUaCheck=${config.enableUaCheck} sdkShouldDisplay=$sdkAllowed"
            )
            if (sdkAllowed) {
                AdsManager.loadInterWelcome(activity = activity)
            }
        }

        hasStartedOnce = true
        stoppedAtMs = 0L
    }

    fun onMoveToBackground() {
        stoppedAtMs = SystemClock.elapsedRealtime()
        hasBeenBackgroundedBefore = true
        Log.d("DramaXAds", "APP_LIFECYCLE_ON_STOP stoppedAtMs=$stoppedAtMs")
    }
}
