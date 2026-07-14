package com.drama.x.drama.series.dramax.dramaseries.ads

import android.content.Context
import android.util.Log
import com.drama.x.drama.series.dramax.dramaseries.BuildConfig
import com.drama.x.drama.series.dramax.dramaseries.R
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG = "DramaXAds"
private const val RC_AD_REMOTE_CONFIG = "ad_remote_config"
private const val RC_PROD_PROFILE = "prod"
private const val RC_TESTING_PROFILE = "testing"

object RemoteConfigUtils {
    @Volatile
    private var cachedConfig: FirebaseRemoteConfig? = null

    fun configure(context: Context): FirebaseRemoteConfig? = runCatching {
        cachedConfig?.let { return it }
        FirebaseApp.initializeApp(context)
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build()
            )
            setDefaultsAsync(R.xml.remote_config_defaults)
            cachedConfig = this
        }
    }.onFailure {
        Log.w(TAG, "Firebase Remote Config unavailable; asset ad defaults remain active.", it)
    }.getOrNull()

    suspend fun fetchAndApply(config: FirebaseRemoteConfig?, timeoutMs: Long = 1500L): Boolean {
        if (config == null) return false
        val activated = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Boolean> { cont ->
                config.fetchAndActivate().addOnCompleteListener { task ->
                    if (cont.isActive) cont.resume(task.isSuccessful)
                }
            }
        } ?: false
        val profileKey = selectedAdProfileKey()
        if (!AdRemoteConfig.applyRemoteProfileJson(config.getString(profileKey), profileKey)) {
            if (!AdRemoteConfig.applyRemoteValues(config)) {
                AdRemoteConfig.applyRemoteJson(config.getString(RC_AD_REMOTE_CONFIG))
            }
        }
        Log.d(TAG, "Remote Config fetch/apply completed success=$activated")
        return activated
    }

    fun applyCached(config: FirebaseRemoteConfig?) {
        config ?: return
        val profileKey = selectedAdProfileKey()
        if (!AdRemoteConfig.applyRemoteProfileJson(config.getString(profileKey), profileKey)) {
            if (!AdRemoteConfig.applyRemoteValues(config)) {
                AdRemoteConfig.applyRemoteJson(config.getString(RC_AD_REMOTE_CONFIG))
            }
        }
    }

    private fun selectedAdProfileKey(): String =
        if (BuildConfig.DEBUG) RC_TESTING_PROFILE else RC_PROD_PROFILE
}
