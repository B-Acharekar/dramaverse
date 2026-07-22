package com.drama.x.drama.series.dramax.dramaseries.ads

import android.content.Context
import android.util.Log
import com.drama.x.drama.series.dramax.dramaseries.BuildConfig
import org.json.JSONObject

private const val TAG = "DramaXAds"

object AdRemoteConfig {
    private const val DEBUG_ASSET = "ad_config_debug.json"
    private const val RELEASE_ASSET = "ad_config.json"

    private const val DEV_PREFS_NAME = "dev_config"
    private const val DEV_KEY_UNLIMITED_ADS = "unlimited_ads"


    private val unlimitedAdsGatedPlacements = setOf(
        "native_onboarding_1_1",
        "native_onboarding_2_1",
        "native_onboarding_1_4",
        "native_onboarding_2_4",
        "native_onboarding_fullscreen_1_2",
        "native_onboarding_fullscreen_2_2"
    )

    @Volatile
    private var appContext: Context? = null

    val placementNames = listOf(
        "inter_splash",
        "banner_splash",
        "native_language_1",
        "native_language_1_click",
        "native_language_2",
        "native_language_2_click",
        "native_onboarding_1_1",
        "native_onboarding_2_1",
        "native_onboarding_1_4",
        "native_onboarding_2_4",
        "native_onboarding_fullscreen_1_2",
        "native_onboarding_fullscreen_2_2",
        "inter_onboarding",
        "inter_welcome_back",
        "inter_splash_uninstall",
        "banner_splash_uninstall",
        "native_uninstall",
        "native_survey_uninstall",
        "inter_home",
        "native_home",
        "banner_collapsible_home",
        "inter_back",
        "native_all",
        "inter_all",
        "reward_all"
    )

    @Volatile
    private var activeConfig: AdsConfig = AdsConfig()

    val interSplash: AdUnitConfig get() = placement("inter_splash")
    val bannerSplash: AdUnitConfig get() = placement("banner_splash")
    val interOnboarding: AdUnitConfig get() = placement("inter_onboarding")
    val interWelcomeBack: AdUnitConfig get() = placement("inter_welcome_back")
    val interSplashUninstall: AdUnitConfig get() = placement("inter_splash_uninstall")
    val bannerSplashUninstall: AdUnitConfig get() = placement("banner_splash_uninstall")
    val nativeUninstall: AdUnitConfig get() = placement("native_uninstall")
    val nativeSurveyUninstall: AdUnitConfig get() = placement("native_survey_uninstall")
    fun nativeLanguage(firstVisit: Boolean): AdUnitConfig =
        placement(if (firstVisit) "native_language_1" else "native_language_2")
    fun nativeLanguageClick(firstVisit: Boolean): AdUnitConfig =
        placement(if (firstVisit) "native_language_1_click" else "native_language_2_click")
    fun nativeOnboardingFirstPage(firstVisit: Boolean): AdUnitConfig =
        placement(if (firstVisit) "native_onboarding_1_1" else "native_onboarding_2_1")
    fun nativeOnboardingFourthPage(firstVisit: Boolean): AdUnitConfig =
        placement(if (firstVisit) "native_onboarding_1_4" else "native_onboarding_2_4")
    fun nativeOnboardingFull(firstVisit: Boolean): AdUnitConfig =
        placement(if (firstVisit) "native_onboarding_fullscreen_1_2" else "native_onboarding_fullscreen_2_2")

    fun placement(name: String): AdUnitConfig {
        val base = activeConfig.placement(name)
        return if (name in unlimitedAdsGatedPlacements) {
            val overrideEnabled = isUnlimitedAdsEnabled()
            if (base.isEnable != overrideEnabled) {
                Log.d(
                    TAG,
                    "$name isEnable overridden by unlimitedAds toggle: json=${base.isEnable} -> $overrideEnabled"
                )
            }
            base.copy(isEnable = overrideEnabled)
        } else {
            base
        }
    }

    private fun isUnlimitedAdsEnabled(): Boolean {
        val ctx = appContext ?: return false
        return ctx.getSharedPreferences(DEV_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(DEV_KEY_UNLIMITED_ADS, false)
    }

    fun initializeFromAssets(context: Context) {
        appContext = context.applicationContext
        val assetName = if (BuildConfig.DEBUG) DEBUG_ASSET else RELEASE_ASSET
        runCatching {
            context.assets.open(assetName).bufferedReader().use { it.readText() }
        }.onSuccess {
            activeConfig = parse(it)
            Log.d(TAG, "AD_CONFIG_SOURCE=${if (BuildConfig.DEBUG) "LOCAL_DEBUG" else "LOCAL_RELEASE"}")
            logActiveConfig("Local active")
        }.onFailure {
            Log.e(TAG, "Failed to load local ad config asset=$assetName", it)
        }
    }

    fun applyRemoteJson(json: String?) {
        if (json.isNullOrBlank() || json.trim() == "{}") {
            Log.d(TAG, "Remote ad config empty; keeping local active config")
            return
        }
        runCatching {
            val parsed = parse(json)
            activeConfig = parsed
            Log.d(TAG, "AD_CONFIG_SOURCE=FIREBASE_REMOTE")
            logActiveConfig("Remote active")
        }.onFailure {
            Log.e(TAG, "Remote ad config invalid; keeping previous config", it)
        }
    }

    fun snapshot(): AdsConfig = activeConfig

    private fun parse(json: String): AdsConfig {
        val root = JSONObject(json)
        val placements = placementNames.associateWith { key ->
            val item = root.optJSONObject(key)
            AdUnitConfig(
                id = item?.optString("id").orEmpty(),
                isEnable = item?.optBoolean("isEnable", false) ?: false,
                timeoutMs = item?.optLong("timeoutMs", 0L) ?: 0L,
                reloadIntervalSeconds = item?.optInt("reloadIntervalSeconds", 0) ?: 0,
                enableUaCheck = item?.optBoolean("enable_ua_check", defaultEnableUaCheck(key))
                    ?: defaultEnableUaCheck(key)
            )
        }
        return AdsConfig(
            configId = root.optString("config_id"),
            adsEnabled = root.optBoolean("ads_enabled", true),
            placements = placements
        )
    }

    private fun logActiveConfig(prefix: String) {
        Log.d(TAG, "$prefix config ID=${activeConfig.configId.ifBlank { "unknown" }}")
        Log.d(TAG, "ads_enabled=${activeConfig.adsEnabled}")
        placementNames.forEach { name ->
            val item = placement(name)
            Log.d(TAG, "$name enabled=${item.isEnable} enableUaCheck=${item.enableUaCheck} id=${item.id.maskAdUnit()}")
        }
    }

    private fun defaultEnableUaCheck(name: String): Boolean = when (name) {
        "native_onboarding_fullscreen_1_2",
        "native_onboarding_fullscreen_2_2",
        "inter_onboarding" -> true
        else -> false
    }
}

internal fun String.maskAdUnit(): String =
    if (isBlank()) "<empty>" else replace(Regex("/\\d+"), "/***")
