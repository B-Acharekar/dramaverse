package com.drama.x.drama.series.dramax.dramaseries

import android.util.Log
import com.drama.x.drama.series.dramax.dramaseries.ads.AdRemoteConfig
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsInitializationState
import com.ads.module.admob.Admob
import com.ads.module.ads.ERainAd
import com.ads.module.application.AdsMultiDexApplication
import com.ads.module.config.AdjustConfig
import com.ads.module.config.ERainAdConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.ads.MobileAds

private const val TAG = "DramaXAds"

class GlobalApp : AdsMultiDexApplication() {
    override fun onCreate() {
        Log.d(TAG, "APP_INIT_START")
        super.onCreate()

        warnIfGooglePlayServicesOutdated()
        Log.d(TAG, "MOBILE_ADS_INIT_REQUESTED")
        MobileAds.initialize(this) {
            Log.d(TAG, "MOBILE_ADS_INIT_COMPLETED")
            AdsInitializationState.markMobileAdsReady()
        }

        /*
         * Ad config must be available before Splash makes decisions. Debug reads Google
         * test IDs from assets first; Firebase Remote Config may override it later.
         */
        AdRemoteConfig.initializeFromAssets(this)
        Log.d(TAG, "LOCAL_AD_CONFIG_READY")

        initAds()
    }

    private fun initAds() {
        Log.d(TAG, "ERAIN_INIT_START")
        val environment = if (BuildConfig.DEBUG) {
            ERainAdConfig.ENVIRONMENT_DEVELOP
        } else {
            ERainAdConfig.ENVIRONMENT_PRODUCTION
        }

        val erainAdConfig = ERainAdConfig(this, environment).apply {
            adjustConfig = AdjustConfig(true, getString(R.string.adjust_token))
            facebookClientToken = getString(R.string.facebook_client_token)
            adjustTokenTiktok = getString(R.string.tiktok_token)
            intervalInterstitialAd = 35
            idAdResume = ""
        }

        ERainAd.getInstance().init(this, erainAdConfig)
        Admob.getInstance().setDisableAdResumeWhenClickAds(true)
        Admob.getInstance().setOpenActivityAfterShowInterAds(true)
        Log.d(
            TAG,
            "ERAIN_INIT_COMPLETED environment=$environment adjust=true facebook=true tiktok=true"
        )
    }

    private fun warnIfGooglePlayServicesOutdated() {
        val availability = GoogleApiAvailability.getInstance()
        val status = availability.isGooglePlayServicesAvailable(this)
        if (status != ConnectionResult.SUCCESS) {
            Log.w(TAG, "GOOGLE_PLAY_SERVICES_UNAVAILABLE_OR_OUTDATED status=$status")
        }
    }
}
