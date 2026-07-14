package com.drama.x.drama.series.dramax.dramaseries.ads

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

object AdsInitializationState {
    private val mobileAdsReady = AtomicBoolean(false)

    fun markMobileAdsReady() {
        mobileAdsReady.set(true)
    }

    suspend fun awaitMobileAdsReady(timeoutMs: Long = 4_000L): Boolean {
        if (mobileAdsReady.get()) return true
        return withTimeoutOrNull(timeoutMs) {
            while (!mobileAdsReady.get()) {
                delay(50)
            }
            true
        } == true
    }
}
