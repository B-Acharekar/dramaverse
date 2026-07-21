package com.drama.x.drama.series.dramax.dramaseries.ads

object ResumeAdsEntryRule {
    fun shouldShowWelcomeOnResume(): Boolean =
        AdRemoteConfig.interWelcomeBack.isEnable
}
