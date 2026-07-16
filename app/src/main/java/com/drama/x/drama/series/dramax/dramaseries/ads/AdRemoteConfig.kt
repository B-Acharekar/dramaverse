package com.drama.x.drama.series.dramax.dramaseries.ads

import android.content.Context
import android.util.Log
import com.drama.x.drama.series.dramax.dramaseries.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.json.JSONObject

private const val TAG = "DramaXAds"

object AdRemoteConfig {
    private const val DEBUG_ASSET = "ad_config_debug.json"
    private const val RELEASE_ASSET = "ad_config.json"

    private val falseVersionDisabledPlacements = setOf(
        "native_onboarding_1_1",
        "native_onboarding_2_1",
        "native_onboarding_1_4",
        "native_onboarding_2_4",
        "native_onboarding_fullscreen_1_2",
        "native_onboarding_fullscreen_2_2",
        "inter_onboarding"
    )

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

    val interSplash: AdUnitConfig get() = activeConfig.placement("inter_splash")
    val bannerSplash: AdUnitConfig get() = activeConfig.placement("banner_splash")
    val interOnboarding: AdUnitConfig get() = placement("inter_onboarding")
    val interWelcomeBack: AdUnitConfig get() = activeConfig.placement("inter_welcome_back")
    val interSplashUninstall: AdUnitConfig get() = activeConfig.placement("inter_splash_uninstall")
    val bannerSplashUninstall: AdUnitConfig get() = activeConfig.placement("banner_splash_uninstall")
    val nativeUninstall: AdUnitConfig get() = activeConfig.placement("native_uninstall")
    val nativeSurveyUninstall: AdUnitConfig get() = activeConfig.placement("native_survey_uninstall")
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

    private fun placement(name: String): AdUnitConfig {
        val config = activeConfig.placement(name)
        return if (!BuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE && name in falseVersionDisabledPlacements) {
            config.copy(isEnable = false)
        } else {
            config
        }
    }

    fun initializeFromAssets(context: Context) {
        val assetName = if (BuildConfig.DEBUG) DEBUG_ASSET else RELEASE_ASSET
        runCatching {
            context.assets.open(assetName).bufferedReader().use { it.readText() }
        }.onSuccess {
            activeConfig = parse(it)
            Log.d(TAG, "AD_CONFIG_SOURCE=LOCAL_DEBUG forceLocal=${BuildConfig.FORCE_LOCAL_AD_CONFIG}")
            logActiveConfig("Local active")
        }.onFailure {
            Log.e(TAG, "Failed to load local ad config asset=$assetName", it)
        }
    }

    fun applyRemoteJson(json: String?) {
        if (BuildConfig.DEBUG && BuildConfig.FORCE_LOCAL_AD_CONFIG) {
            Log.d(TAG, "AD_CONFIG_SOURCE=LOCAL_DEBUG Firebase ad_remote_config ignored for QA")
            return
        }
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

    fun applyRemoteProfileJson(json: String?, profileKey: String): Boolean {
        if (BuildConfig.DEBUG && BuildConfig.FORCE_LOCAL_AD_CONFIG) {
            Log.d(TAG, "AD_CONFIG_SOURCE=LOCAL_DEBUG Firebase $profileKey profile ignored for QA")
            return true
        }
        if (json.isNullOrBlank() || json.trim() == "{}") {
            Log.d(TAG, "Remote profile '$profileKey' empty; trying fallback config source")
            return false
        }
        return runCatching {
            val root = JSONObject(json)
            activeConfig = if (root.has("parameters")) {
                parseFirebaseTemplateProfile(root, profileKey)
            } else {
                parse(json)
            }
            Log.d(TAG, "AD_CONFIG_SOURCE=FIREBASE_REMOTE_PROFILE profile=$profileKey buildDebug=${BuildConfig.DEBUG}")
            logActiveConfig("Remote profile active")
            true
        }.onFailure {
            Log.e(TAG, "Remote profile '$profileKey' invalid; trying fallback config source", it)
        }.getOrDefault(false)
    }

    fun applyRemoteValues(config: FirebaseRemoteConfig?): Boolean {
        config ?: return false
        if (BuildConfig.DEBUG && BuildConfig.FORCE_LOCAL_AD_CONFIG) {
            Log.d(TAG, "AD_CONFIG_SOURCE=LOCAL_DEBUG Firebase individual ad keys ignored for QA")
            return true
        }

        val remotePlacements = activeConfig.placements.toMutableMap()
        val hasMasterSwitch = config.hasRemoteValue("ads_enabled")
        var appliedCount = 0
        placementNames.forEach { name ->
            val current = activeConfig.placement(name)
            val idKey = "${name}_id"
            val enabledKey = "${name}_enabled"
            val timeoutKey = "${name}_timeout_ms"
            val reloadKey = "${name}_reload_interval_seconds"
            val hasAnyRemoteValue = config.hasRemoteValue(idKey) ||
                config.hasRemoteValue(enabledKey) ||
                config.hasRemoteValue(timeoutKey) ||
                config.hasRemoteValue(reloadKey)

            if (hasAnyRemoteValue) {
                remotePlacements[name] = AdUnitConfig(
                    id = if (config.hasRemoteValue(idKey)) config.getString(idKey).trim() else current.id,
                    isEnable = if (config.hasRemoteValue(enabledKey)) config.getBoolean(enabledKey) else current.isEnable,
                    timeoutMs = if (config.hasRemoteValue(timeoutKey)) config.getLong(timeoutKey).coerceAtLeast(0L) else current.timeoutMs,
                    reloadIntervalSeconds = if (config.hasRemoteValue(reloadKey)) {
                        config.getLong(reloadKey).toInt().coerceAtLeast(0)
                    } else {
                        current.reloadIntervalSeconds
                    }
                )
                appliedCount++
            }
        }

        if (appliedCount == 0 && !hasMasterSwitch) {
            Log.d(TAG, "Remote individual ad config empty; keeping local active config")
            return false
        }

        val parsed = AdsConfig(
            configId = config.getString("ad_config_id").ifBlank { "firebase_individual_keys" },
            adsEnabled = if (hasMasterSwitch) config.getBoolean("ads_enabled") else activeConfig.adsEnabled,
            placements = remotePlacements
        )
        activeConfig = parsed
        Log.d(TAG, "AD_CONFIG_SOURCE=FIREBASE_REMOTE_INDIVIDUAL adsEnabled=${parsed.adsEnabled} appliedPlacements=$appliedCount")
        logActiveConfig("Remote individual active")
        return true
    }

    private fun FirebaseRemoteConfig.hasRemoteValue(key: String): Boolean =
        getValue(key).source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE

    private fun parse(json: String): AdsConfig {
        val root = JSONObject(json)
        val placements = placementNames.associateWith { key ->
            val item = root.optJSONObject(key)
            AdUnitConfig(
                id = item?.optString("id").orEmpty(),
                isEnable = item?.optBoolean("isEnable", false) ?: false,
                timeoutMs = item?.optLong("timeoutMs", 0L) ?: 0L,
                reloadIntervalSeconds = item?.optInt("reloadIntervalSeconds", 0) ?: 0
            )
        }
        return AdsConfig(
            configId = root.optString("config_id"),
            adsEnabled = root.optBoolean("ads_enabled", true),
            placements = placements
        )
    }

    private fun parseFirebaseTemplateProfile(root: JSONObject, profileKey: String): AdsConfig {
        val params = root.optJSONObject("parameters") ?: JSONObject()
        val remotePlacements = activeConfig.placements.toMutableMap()
        placementNames.forEach { name ->
            val current = activeConfig.placement(name)
            remotePlacements[name] = AdUnitConfig(
                id = params.defaultValue("${name}_id") ?: current.id,
                isEnable = params.defaultValue("${name}_enabled")?.toBooleanStrictOrNull() ?: current.isEnable,
                timeoutMs = params.defaultValue("${name}_timeout_ms")?.toLongOrNull()?.coerceAtLeast(0L) ?: current.timeoutMs,
                reloadIntervalSeconds = params.defaultValue("${name}_reload_interval_seconds")
                    ?.toIntOrNull()
                    ?.coerceAtLeast(0)
                    ?: current.reloadIntervalSeconds
            )
        }
        return AdsConfig(
            configId = params.defaultValue("ad_config_id") ?: "firebase_profile_$profileKey",
            adsEnabled = params.defaultValue("ads_enabled")?.toBooleanStrictOrNull() ?: activeConfig.adsEnabled,
            placements = remotePlacements
        )
    }

    private fun JSONObject.defaultValue(key: String): String? {
        val parameter = optJSONObject(key) ?: return null
        return parameter.optJSONObject("defaultValue")
            ?.optString("value")
            ?.takeIf { it.isNotBlank() }
            ?: parameter.optString("value").takeIf { it.isNotBlank() }
    }

    private fun logActiveConfig(prefix: String) {
        Log.d(TAG, "$prefix config ID=${activeConfig.configId.ifBlank { "unknown" }}")
        Log.d(TAG, "ads_enabled=${activeConfig.adsEnabled}")
        placementNames.forEach { name ->
            val item = activeConfig.placement(name)
            Log.d(TAG, "$name enabled=${item.isEnable} id=${item.id.maskAdUnit()}")
        }
    }
}

internal fun String.maskAdUnit(): String =
    if (isBlank()) "<empty>" else replace(Regex("/\\d+"), "/***")
