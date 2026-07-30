package com.drama.x.drama.series.dramax.dramaseries.ads

enum class ResumeAdsEntryMode {
    OPEN_RESUME,
    WELCOME,
    NONE
}

object ResumeAdsEntryRule {
    fun currentMode(): ResumeAdsEntryMode {
        val canUseOpenResume = AdRemoteConfig.openResume.isEnable
        if (canUseOpenResume) return ResumeAdsEntryMode.OPEN_RESUME

        val canUseWelcome = AdRemoteConfig.interWelcomeBack.isEnable
        return if (canUseWelcome) ResumeAdsEntryMode.WELCOME else ResumeAdsEntryMode.NONE
    }

    fun shouldEnableOpenResume(): Boolean =
        currentMode() == ResumeAdsEntryMode.OPEN_RESUME

    fun shouldShowWelcomeOnResume(): Boolean =
        currentMode() == ResumeAdsEntryMode.WELCOME && !AdRemoteConfig.openResume.isEnable
}
