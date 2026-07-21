package com.drama.x.drama.series.dramax.dramaseries

import android.util.Log
import com.ads.module.ads.ERainAd

private const val TAG = "DramaXWidget"

object WidgetUninstallGate {
    fun shouldDisplay(): Boolean =
        runCatching { ERainAd.getInstance().getShouldDisplayWidgetUninstall() }
            .onFailure { Log.w(TAG, "Widget uninstall gate failed; hiding widget uninstall action.", it) }
            .getOrDefault(false)
}
