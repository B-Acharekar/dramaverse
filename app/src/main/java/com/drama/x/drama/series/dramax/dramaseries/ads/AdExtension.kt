package com.drama.x.drama.series.dramax.dramaseries.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.ads.module.ads.wrapper.ApNativeAd

internal const val ADS_TAG = "DramaXAds"

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        connectivityManager.activeNetworkInfo?.isConnected == true
    }
}

fun ApNativeAd?.destroySafely() {
    runCatching { this?.admobNativeAd?.destroy() }
}

fun NativeAdState.destroyLoadedAdSafely() {
    if (this is NativeAdState.Loaded) {
        nativeAd.destroySafely()
    }
}
