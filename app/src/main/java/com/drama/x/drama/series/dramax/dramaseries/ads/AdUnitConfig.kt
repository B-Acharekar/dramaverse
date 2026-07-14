package com.drama.x.drama.series.dramax.dramaseries.ads

data class AdUnitConfig(
    val id: String = "",
    val isEnable: Boolean = false,
    val timeoutMs: Long = 0L,
    val reloadIntervalSeconds: Int = 0
) {
    val canRequest: Boolean
        get() = isEnable && id.isNotBlank()
}

data class AdsConfig(
    val configId: String = "",
    val adsEnabled: Boolean = true,
    val placements: Map<String, AdUnitConfig> = emptyMap()
) {
    fun placement(name: String): AdUnitConfig {
        val placement = placements[name] ?: AdUnitConfig()
        return if (adsEnabled) placement else placement.copy(isEnable = false)
    }
}
