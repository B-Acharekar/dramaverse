package com.drama.x.drama.series.dramax.dramaseries.ads

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import com.ads.module.ads.ERainAd
import com.drama.x.drama.series.dramax.dramaseries.R

private const val DEFAULT_CTA_HEIGHT_DP = 40
private const val MIN_CTA_HEIGHT_DP = 36
private const val MAX_CTA_HEIGHT_DP = 52
private const val MAX_CTA_HEIGHT_WITHOUT_HIGH_GATE_DP = 46

object NativeAdUiStandards {
    fun apply(root: View, context: Context) {
        val cta = root.findViewById<Button>(R.id.ad_call_to_action) ?: return
        val requestedHeight = RemoteConfigUtils.ctaHeightDp(context)
        val allowHighCta = ERainAd.getInstance().getShouldDisplayHighCTA(true)
        val maxHeight = if (allowHighCta) {
            MAX_CTA_HEIGHT_DP
        } else {
            MAX_CTA_HEIGHT_WITHOUT_HIGH_GATE_DP
        }
        val clampedHeight = requestedHeight.coerceIn(MIN_CTA_HEIGHT_DP, maxHeight)
        val heightPx = (clampedHeight * context.resources.displayMetrics.density).toInt()
        cta.layoutParams = cta.layoutParams.apply {
            height = heightPx
        }
        cta.minHeight = 0
        cta.minimumHeight = 0
        cta.requestLayout()
        Log.d(
            ADS_TAG,
            "CTA_HEIGHT_APPLIED requestedDp=$requestedHeight clampedDp=$clampedHeight allowHighCta=$allowHighCta"
        )
    }

    fun defaultCtaHeightDp(): Int = DEFAULT_CTA_HEIGHT_DP
}
