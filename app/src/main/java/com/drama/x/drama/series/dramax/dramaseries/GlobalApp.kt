package com.drama.x.drama.series.dramax.dramaseries

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.drama.x.drama.series.dramax.dramaseries.ads.AdRemoteConfig
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsInitializationState
import com.drama.x.drama.series.dramax.dramaseries.ads.ResumeAdsEntryRule
import com.ads.module.admob.Admob
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.application.AdsMultiDexApplication
import com.ads.module.config.AdjustConfig
import com.ads.module.config.ERainAdConfig
import com.drama.x.drama.series.dramax.dramaseries.devconfig.DevConfig
import com.google.android.gms.ads.MobileAds

private const val TAG = "DramaXAds"

class GlobalApp : AdsMultiDexApplication() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        var currentActivity: Activity? = null
    }

    override fun onCreate() {
        Log.d(TAG, "APP_INIT_START")
        super.onCreate()

        Log.d(TAG, "MOBILE_ADS_INIT_REQUESTED")
        MobileAds.initialize(this) {
            Log.d(TAG, "MOBILE_ADS_INIT_COMPLETED")
            AdsInitializationState.markMobileAdsReady()
        }
        DevConfig.init(
            context = this,
            nkhStudioVersion = BuildConfig.ERAIN_STUDIO_VERSION,
            playServicesAdsVersion = BuildConfig.PLAY_SERVICES_ADS_VERSION,
            gdprModuleVersion = BuildConfig.GDPR_MODULE_VERSION
        )

        /*
         * Ad config must be available before Splash makes decisions. Debug reads Google
         * test IDs from assets first; Firebase Remote Config may override it later.
         */
        AdRemoteConfig.initializeFromAssets(this)
        Log.d(TAG, "LOCAL_AD_CONFIG_READY")

        initAds()

        val lifecycleObserver = if (ResumeAdsEntryRule.shouldShowWelcomeOnResume()) {
            AppLifecycleObserver()
        } else {
            null
        }
        registerActivityLifecycleCallbacks(AppActivityLifecycleCallbacks(lifecycleObserver))
    }

    private fun initAds() {
        Log.d(TAG, "ERAIN_INIT_START")
        val environment = if (BuildConfig.DEBUG) {
            ERainAdConfig.ENVIRONMENT_DEVELOP
        } else {
            ERainAdConfig.ENVIRONMENT_PRODUCTION
        }

        val erainAdConfig = ERainAdConfig(this, environment).apply {
            adjustConfig = AdjustConfig(
                true,
                getString(R.string.adjust_token)
            )
            facebookClientToken = getString(R.string.facebook_client_token)
            adjustTokenTiktok = getString(R.string.event_token)
            intervalInterstitialAd = 15
            idAdResume = ""
        }

        ERainAd.getInstance().init(this, erainAdConfig)
        Admob.getInstance().setDisableAdResumeWhenClickAds(true)
        Admob.getInstance().setOpenActivityAfterShowInterAds(true)
        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity::class.java)
        Log.d(
            TAG,
            "ERAIN_INIT_COMPLETED environment=$environment " +
                "adjust=true facebook=true tiktok=true"
        )
    }

}
