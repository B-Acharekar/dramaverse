package com.drama.x.drama.series.dramax.dramaseries

import com.drama.x.drama.series.dramax.dramaseries.devconfig.DevConfig

object WidgetUninstallGate {
    fun shouldDisplay(context: android.content.Context): Boolean =
        DevConfig.isUnlimitedAdsEnabled(context)
}
