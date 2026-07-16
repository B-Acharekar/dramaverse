package com.drama.x.drama.series.dramax.dramaseries.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ads.module.BuildConfig
import com.drama.x.drama.series.dramax.dramaseries.BuildConfig as AppBuildConfig
import com.drama.x.drama.series.dramax.dramaseries.R
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.ads.wrapper.ApNativeAd
import com.ads.module.funtion.AdCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

object AdsManager {
    private val _nativeLanguageAdLive = MutableLiveData<NativeAdState>(NativeAdState.Idle)
    val nativeLanguageAdLive: LiveData<NativeAdState> = _nativeLanguageAdLive

    private val _nativeLanguageClickAdLive = MutableLiveData<NativeAdState>(NativeAdState.Idle)
    val nativeLanguageClickAdLive: LiveData<NativeAdState> = _nativeLanguageClickAdLive

    private val _nativeOnboardingPageOneAdLive = MutableLiveData<NativeAdState>(NativeAdState.Idle)
    val nativeOnboardingPageOneAdLive: LiveData<NativeAdState> = _nativeOnboardingPageOneAdLive

    private val _nativeOnboardingPageThreeAdLive = MutableLiveData<NativeAdState>(NativeAdState.Idle)
    val nativeOnboardingPageThreeAdLive: LiveData<NativeAdState> = _nativeOnboardingPageThreeAdLive

    private val _nativeOnboardingFullscreenAdLive = MutableLiveData<NativeAdState>(NativeAdState.Idle)
    val nativeOnboardingFullscreenAdLive: LiveData<NativeAdState> = _nativeOnboardingFullscreenAdLive

    private val _nativeOnboardingWelcomeAdLive = MutableLiveData<NativeAdState>(NativeAdState.Idle)
    val nativeOnboardingWelcomeAdLive: LiveData<NativeAdState> = _nativeOnboardingWelcomeAdLive

    private val _nativeUninstallAdLive = MutableLiveData<NativeAdState>(NativeAdState.Idle)
    val nativeUninstallAdLive: LiveData<NativeAdState> = _nativeUninstallAdLive

    private val _nativeSurveyUninstallAdLive = MutableLiveData<NativeAdState>(NativeAdState.Idle)
    val nativeSurveyUninstallAdLive: LiveData<NativeAdState> = _nativeSurveyUninstallAdLive
    private var splashInterstitial: ApInterstitialAd? = null
    private var lastWelcomeBackShowMs = 0L
    private var suppressNextResumeInterstitial = false
    private var suppressNextResumeSetMs = 0L
    private var suppressNextNativeClearOnDestroy = false

    fun preserveNativeAdsForActivityRecreate() {
        suppressNextNativeClearOnDestroy = true
        Log.d(ADS_TAG, "native ad clear suppressed for upcoming activity recreate")
    }

    fun consumeSuppressNativeClearOnDestroy(): Boolean {
        val suppress = suppressNextNativeClearOnDestroy
        suppressNextNativeClearOnDestroy = false
        return suppress
    }

    fun consumeSuppressNextResumeInterstitial(): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        val suppress = suppressNextResumeInterstitial
        val suppressAgeMs = now - suppressNextResumeSetMs
        suppressNextResumeInterstitial = false
        suppressNextResumeSetMs = 0L
        val shouldSuppress = suppress && suppressAgeMs in 0L..2_500L
        if (suppress && !shouldSuppress) {
            Log.d(ADS_TAG, "resume suppression expired ageMs=$suppressAgeMs")
        }
        return shouldSuppress
    }

    private fun suppressImmediateResumeInterstitial(reason: String) {
        suppressNextResumeInterstitial = true
        suppressNextResumeSetMs = android.os.SystemClock.elapsedRealtime()
        Log.d(ADS_TAG, "resume suppression set reason=$reason")
    }

    fun suppressResumeInterstitialForExternalDialog(reason: String) {
        suppressImmediateResumeInterstitial(reason)
    }

    fun loadNativeLanguage(activity: Activity, firstVisit: Boolean) {
        val config = AdRemoteConfig.nativeLanguage(firstVisit)
        loadNativePlacement(
            activity = activity,
            placementName = if (firstVisit) "native_language_1" else "native_language_2",
            config = config,
            layoutRes = R.layout.layout_native_language_large,
            liveData = _nativeLanguageAdLive
        )
    }

    fun loadNativeLanguageClick(activity: Activity, firstVisit: Boolean) {
        val config = AdRemoteConfig.nativeLanguageClick(firstVisit)
        loadNativePlacement(
            activity = activity,
            placementName = if (firstVisit) "native_language_1_click" else "native_language_2_click",
            config = config,
            layoutRes = R.layout.layout_native_language_large,
            liveData = _nativeLanguageClickAdLive
        )
    }

    fun preloadOnboardingAds(activity: Activity, firstVisit: Boolean) {
        loadNativeOnboardingPageOne(activity, firstVisit)
        loadNativeOnboardingFullscreen(activity, firstVisit)
        loadNativeOnboardingWelcome(activity, firstVisit)
    }

    fun loadNativeOnboardingPageOne(activity: Activity, firstVisit: Boolean) {
        val sdkShouldDisplay = if (firstVisit) {
            ERainAd.getInstance().getShouldDisplayNativeOnboardingNormal1()
        } else {
            ERainAd.getInstance().getShouldDisplayNativeOnboardingNormal2()
        }
        Log.d(
            ADS_TAG,
            "ERAIN_GATE native_onboarding_page_one firstVisit=$firstVisit " +
                "buildEnabled=${AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE} sdkShouldDisplay=$sdkShouldDisplay"
        )
        val config = AdRemoteConfig.nativeOnboardingFirstPage(firstVisit).withSdkDisplayGate(
            AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE && sdkShouldDisplay
        )
        loadNativePlacement(
            activity = activity,
            placementName = if (firstVisit) "native_onboarding_1_1" else "native_onboarding_2_1",
            config = config,
            layoutRes = R.layout.layout_native_onboarding_compact,
            liveData = _nativeOnboardingPageOneAdLive
        )
    }

    fun loadNativeOnboardingPageThree(activity: Activity, firstVisit: Boolean) {
        val sdkShouldDisplay = ERainAd.getInstance().getShouldDisplayNativeOnboardingNormal2()
        Log.d(
            ADS_TAG,
            "ERAIN_GATE native_onboarding_page_three firstVisit=$firstVisit " +
                "buildEnabled=${AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE} sdkShouldDisplay=$sdkShouldDisplay"
        )
        val config = AdRemoteConfig.nativeOnboardingFourthPage(firstVisit).withSdkDisplayGate(
            AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE && sdkShouldDisplay
        )
        loadNativePlacement(
            activity = activity,
            placementName = if (firstVisit) "native_onboarding_1_4" else "native_onboarding_2_4",
            config = config,
            layoutRes = R.layout.layout_native_onboarding_compact,
            liveData = _nativeOnboardingPageThreeAdLive
        )
    }

    fun loadNativeOnboardingFullscreen(activity: Activity, firstVisit: Boolean) {
        val sdkShouldDisplay = if (firstVisit) {
            ERainAd.getInstance().getShouldDisplayNativeOnboardingFull1()
        } else {
            ERainAd.getInstance().getShouldDisplayNativeOnboardingFull2()
        }
        Log.d(
            ADS_TAG,
            "ERAIN_GATE native_onboarding_fullscreen firstVisit=$firstVisit " +
                "buildEnabled=${AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE} sdkShouldDisplay=$sdkShouldDisplay"
        )
        val config = AdRemoteConfig.nativeOnboardingFull(firstVisit).withSdkDisplayGate(
            AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE && sdkShouldDisplay
        )
        loadNativePlacement(
            activity = activity,
            placementName = if (firstVisit) "native_onboarding_fullscreen_1_2" else "native_onboarding_fullscreen_2_2",
            config = config,
            layoutRes = R.layout.layout_native_full_screen,
            liveData = _nativeOnboardingFullscreenAdLive
        )
    }

    fun loadNativeOnboardingWelcome(activity: Activity, firstVisit: Boolean) {
        val sdkShouldDisplay = ERainAd.getInstance().getShouldDisplayNativeOnboardingNormal2()
        Log.d(
            ADS_TAG,
            "ERAIN_GATE native_onboarding_welcome firstVisit=$firstVisit " +
                "buildEnabled=${AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE} sdkShouldDisplay=$sdkShouldDisplay"
        )
        val config = AdRemoteConfig.nativeOnboardingFourthPage(firstVisit).withSdkDisplayGate(
            AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE && sdkShouldDisplay
        )
        loadNativePlacement(
            activity = activity,
            placementName = if (firstVisit) "native_onboarding_1_4_welcome" else "native_onboarding_2_4_welcome",
            config = config,
            layoutRes = R.layout.layout_native_onboarding_compact,
            liveData = _nativeOnboardingWelcomeAdLive
        )
    }

    fun loadNativeUninstall(activity: Activity) {
        loadNativePlacement(
            activity = activity,
            placementName = "native_uninstall",
            config = AdRemoteConfig.nativeUninstall,
            layoutRes = R.layout.layout_native_language_large,
            liveData = _nativeUninstallAdLive
        )
    }

    fun loadNativeSurveyUninstall(activity: Activity) {
        loadNativePlacement(
            activity = activity,
            placementName = "native_survey_uninstall",
            config = AdRemoteConfig.nativeSurveyUninstall,
            layoutRes = R.layout.layout_native_language_large,
            liveData = _nativeSurveyUninstallAdLive
        )
    }

    fun loadAndShowInterstitial(
        activity: Activity,
        placementName: String,
        config: AdUnitConfig,
        timeoutMs: Long = 6_000L,
        onFinished: () -> Unit
    ) {
        val sdkShouldDisplayInterOnboarding = if (placementName == "inter_onboarding") {
            ERainAd.getInstance().getShouldDisplayInterOnboarding()
        } else {
            true
        }
        if (placementName == "inter_onboarding") {
            Log.d(
                ADS_TAG,
                "ERAIN_GATE inter_onboarding buildEnabled=${AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE} " +
                    "sdkShouldDisplay=$sdkShouldDisplayInterOnboarding"
            )
        }
        val gatedConfig = if (placementName == "inter_onboarding") {
            config.withSdkDisplayGate(
                AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE && sdkShouldDisplayInterOnboarding
            )
        } else {
            config
        }
        Log.d(ADS_TAG, "$placementName decision enabled=${gatedConfig.isEnable} id=${gatedConfig.id.maskAdUnit()}")
        if (!gatedConfig.canRequest || !isNetworkAvailable(activity)) {
            Log.d(ADS_TAG, "$placementName skipped canRequest=${gatedConfig.canRequest} network=${isNetworkAvailable(activity)}")
            onFinished()
            return
        }
        val effectiveTimeoutMs = maxOf(timeoutMs, gatedConfig.timeoutMs)
        var finished = false
        fun finishOnce(reason: String) {
            if (finished) return
            finished = true
            Log.d(ADS_TAG, "$placementName finished reason=$reason")
            onFinished()
        }
        Log.d(ADS_TAG, "$placementName REQUEST via ERain timeoutMs=$effectiveTimeoutMs")
        Handler(Looper.getMainLooper()).postDelayed({ finishOnce("timeout") }, effectiveTimeoutMs)
        ERainAd.getInstance().getInterstitialAds(
            activity,
            gatedConfig.id,
            object : AdCallback() {
                override fun onApInterstitialLoad(interstitialAd: ApInterstitialAd?) {
                    if (finished) return
                    if (interstitialAd == null) {
                        finishOnce("load_null")
                        return
                    }
                    Log.d(ADS_TAG, "$placementName loaded via ERain")
                    ERainAd.getInstance().forceShowInterstitial(
                        activity,
                        interstitialAd,
                        object : AdCallback() {
                            override fun onInterstitialShow() {
                                suppressImmediateResumeInterstitial("${placementName}_show")
                                Log.d(ADS_TAG, "$placementName displayed via ERain")
                            }

                            override fun onNextAction() {
                                finishOnce("next_action")
                            }

                            override fun onAdClosed() {
                                finishOnce("closed")
                            }

                            override fun onAdFailedToShow(error: AdError?) {
                                Log.e(ADS_TAG, "$placementName failed_to_show via ERain message=${error?.message}")
                                finishOnce("show_failed")
                            }
                        },
                        true
                    )
                }

                override fun onAdFailedToLoad(error: LoadAdError?) {
                    error?.let { logAdFailure(placementName, it) }
                    finishOnce("load_failed")
                }
            }
        )
    }

    private fun AdUnitConfig.withSdkDisplayGate(shouldDisplay: Boolean): AdUnitConfig =
        if (shouldDisplay) this else copy(isEnable = false)

    fun showWelcomeBackInterstitial(activity: Activity) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastWelcomeBackShowMs < 35_000L) {
            Log.d(ADS_TAG, "inter_welcome_back skipped cooldown elapsedMs=${now - lastWelcomeBackShowMs}")
            return
        }
        lastWelcomeBackShowMs = now
        Log.d(ADS_TAG, "inter_welcome_back resume eligible")
        loadAndShowInterstitial(
            activity = activity,
            placementName = "inter_welcome_back",
            config = AdRemoteConfig.interWelcomeBack,
            timeoutMs = 12_000L,
            onFinished = {}
        )
    }

    fun loadSplashInterstitial(
        activity: Activity,
        uninstallFlow: Boolean = false,
        onLoaded: () -> Unit = {},
        onFailed: (LoadAdError?) -> Unit = {}
    ) {
        val config = if (uninstallFlow) AdRemoteConfig.interSplashUninstall else AdRemoteConfig.interSplash
        val placementName = if (uninstallFlow) "inter_splash_uninstall" else "inter_splash"
        Log.d(ADS_TAG, "Splash interstitial decision placement=$placementName enabled=${config.isEnable} id=${config.id.maskAdUnit()}")
        if (!config.canRequest || !isNetworkAvailable(activity)) {
            onFailed(null)
            return
        }
        Log.d(ADS_TAG, "SPLASH_INTER_REQUEST placement=$placementName")
        ERainAd.getInstance().getInterstitialAds(
            activity,
            config.id,
            object : AdCallback() {
                override fun onApInterstitialLoad(interstitialAd: ApInterstitialAd?) {
                    if (interstitialAd == null) {
                        splashInterstitial = null
                        onFailed(null)
                        return
                    }
                    splashInterstitial = interstitialAd
                    Log.d(ADS_TAG, "SPLASH_INTER_LOADED placement=$placementName via ERain")
                    onLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError?) {
                    splashInterstitial = null
                    Log.d(ADS_TAG, "SPLASH_INTER_FAILED")
                    error?.let { logAdFailure(placementName, it) }
                    onFailed(error)
                }
            }
        )
    }

    fun showSplashInterstitialIfReady(activity: Activity, onClosed: () -> Unit = {}) {
        val ad = splashInterstitial
        if (ad == null) {
            onClosed()
            return
        }
        splashInterstitial = null
        ERainAd.getInstance().forceShowInterstitial(
            activity,
            ad,
            object : AdCallback() {
                override fun onInterstitialShow() {
                    suppressImmediateResumeInterstitial("splash_inter_show")
                    Log.d(ADS_TAG, "SPLASH_INTER_DISPLAYED via ERain")
                }

                override fun onNextAction() {
                    Log.d(ADS_TAG, "SPLASH_INTER_DISMISSED via ERain")
                    onClosed()
                }

                override fun onAdClosed() {
                    Log.d(ADS_TAG, "SPLASH_INTER_CLOSED via ERain")
                    onClosed()
                }

                override fun onAdFailedToShow(error: AdError?) {
                    Log.e(ADS_TAG, "SPLASH_INTER_FAILED_TO_SHOW via ERain: ${error?.message}")
                    onClosed()
                }
            },
            true
        )
    }

    fun clearLanguageAds() {
        _nativeLanguageAdLive.value?.destroyLoadedAdSafely()
        _nativeLanguageClickAdLive.value?.destroyLoadedAdSafely()
        _nativeOnboardingPageOneAdLive.value?.destroyLoadedAdSafely()
        _nativeOnboardingPageThreeAdLive.value?.destroyLoadedAdSafely()
        _nativeOnboardingFullscreenAdLive.value?.destroyLoadedAdSafely()
        _nativeOnboardingWelcomeAdLive.value?.destroyLoadedAdSafely()
        _nativeUninstallAdLive.value?.destroyLoadedAdSafely()
        _nativeSurveyUninstallAdLive.value?.destroyLoadedAdSafely()
        _nativeLanguageAdLive.value = NativeAdState.Idle
        _nativeLanguageClickAdLive.value = NativeAdState.Idle
        _nativeOnboardingPageOneAdLive.value = NativeAdState.Idle
        _nativeOnboardingPageThreeAdLive.value = NativeAdState.Idle
        _nativeOnboardingFullscreenAdLive.value = NativeAdState.Idle
        _nativeOnboardingWelcomeAdLive.value = NativeAdState.Idle
        _nativeUninstallAdLive.value = NativeAdState.Idle
        _nativeSurveyUninstallAdLive.value = NativeAdState.Idle
    }

    private fun loadNativePlacement(
        activity: Activity,
        placementName: String,
        config: AdUnitConfig,
        layoutRes: Int,
        liveData: MutableLiveData<NativeAdState>
    ) {
        Log.d(ADS_TAG, "[$placementName] ELIGIBILITY enabled=${config.isEnable} id=${config.id.maskAdUnit()}")
        if (activity.isFinishing || activity.isDestroyed) {
            liveData.postValue(NativeAdState.Failed("activity_destroyed"))
            Log.d(ADS_TAG, "[$placementName] FAILED reason=activity_destroyed")
            return
        }
        if (!config.canRequest) {
            liveData.value?.destroyLoadedAdSafely()
            liveData.postValue(NativeAdState.Disabled("disabled_or_empty_id"))
            Log.d(ADS_TAG, "[$placementName] DISABLED enabled=${config.isEnable} id=${config.id.maskAdUnit()}")
            return
        }
        if (!isNetworkAvailable(activity)) {
            liveData.postValue(NativeAdState.Failed("no_network"))
            Log.d(ADS_TAG, "[$placementName] FAILED reason=no_network")
            return
        }
        when (val state = liveData.value) {
            NativeAdState.Loading -> {
                Log.d(ADS_TAG, "[$placementName] REQUEST skipped duplicate state=LOADING")
                return
            }
            is NativeAdState.Loaded -> {
                Log.d(ADS_TAG, "[$placementName] REQUEST skipped cached_loaded")
                liveData.postValue(state)
                return
            }
            is NativeAdState.Disabled, is NativeAdState.Failed, NativeAdState.Idle, null -> Unit
        }

        liveData.postValue(NativeAdState.Loading)
        Log.d(ADS_TAG, "[$placementName] REQUEST layoutRes=$layoutRes via ERain")
        requestNative(activity, placementName, config, layoutRes, liveData, allowDebugRetry = true)
    }

    private fun requestNative(
        activity: Activity,
        placementName: String,
        config: AdUnitConfig,
        layoutRes: Int,
        liveData: MutableLiveData<NativeAdState>,
        allowDebugRetry: Boolean
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            liveData.postValue(NativeAdState.Failed("activity_destroyed"))
            return
        }
        ERainAd.getInstance().loadNativeAdResultCallback(
            activity,
            config.id,
            layoutRes,
            object : AdCallback() {
                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    if (activity.isFinishing || activity.isDestroyed) {
                        nativeAd.destroySafely()
                        liveData.postValue(NativeAdState.Failed("activity_destroyed_after_load"))
                        return
                    }
                    if (nativeAd.isNotReady()) {
                        liveData.postValue(NativeAdState.Failed("null_or_not_ready"))
                        Log.d(ADS_TAG, "[$placementName] FAILED reason=null_or_not_ready")
                        return
                    }
                    liveData.value?.destroyLoadedAdSafely()
                    liveData.postValue(NativeAdState.Loaded(nativeAd))
                    Log.d(ADS_TAG, "[$placementName] LOADED via ERain")
                }

                override fun onAdFailedToLoad(error: LoadAdError?) {
                    liveData.postValue(NativeAdState.Failed(error?.message.orEmpty()))
                    Log.d(ADS_TAG, "[$placementName] FAILED via ERain")
                    error?.let { logAdFailure(placementName, it) }
                    if (allowDebugRetry && BuildConfig.DEBUG) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                Log.d(ADS_TAG, "[$placementName] REQUEST retry via ERain")
                                liveData.postValue(NativeAdState.Loading)
                                requestNative(activity, placementName, config, layoutRes, liveData, allowDebugRetry = false)
                            }
                        }, 1_500L)
                    }
                }
            }
        )
    }

    private fun logAdFailure(placementName: String, error: LoadAdError) {
        Log.e(
            ADS_TAG,
            "$placementName failed: code=${error.code}, domain=${error.domain}, " +
                "message=${error.message}, responseInfo=${error.responseInfo}, cause=${error.cause}"
        )
        error.responseInfo?.adapterResponses?.forEach { adapter ->
            Log.d(
                ADS_TAG,
                "Adapter=${adapter.adapterClassName}, latency=${adapter.latencyMillis}, error=${adapter.adError}"
            )
        }
    }
}
