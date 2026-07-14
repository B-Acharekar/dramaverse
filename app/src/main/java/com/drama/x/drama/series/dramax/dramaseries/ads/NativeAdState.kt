package com.drama.x.drama.series.dramax.dramaseries.ads

import com.ads.module.ads.wrapper.ApNativeAd

sealed interface NativeAdState {
    data object Idle : NativeAdState
    data object Loading : NativeAdState
    data class Loaded(val nativeAd: ApNativeAd) : NativeAdState
    data class Failed(val reason: String = "") : NativeAdState
    data class Disabled(val reason: String = "") : NativeAdState
}
