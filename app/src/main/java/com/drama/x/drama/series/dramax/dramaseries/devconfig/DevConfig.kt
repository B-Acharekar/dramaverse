package com.drama.x.drama.series.dramax.dramaseries.devconfig

import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ads.module.util.SharePreferenceUtils
import com.drama.x.drama.series.dramax.dramaseries.BuildConfig
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.ads.AdRemoteConfig
import com.google.android.gms.ads.MobileAds

object DevConfig {
    private const val PREFS_NAME = "dev_config"
    private const val KEY_UNLIMITED_ADS = "unlimited_ads"

    private var erainStudioVersion: String = "N/A"
    private var playServicesAdsVersion: String = "N/A"
    private var gdprModuleVersion: String = "N/A"

    fun init(
        context: Context,
        nkhStudioVersion: String,
        playServicesAdsVersion: String,
        gdprModuleVersion: String
    ) {
        this.erainStudioVersion = nkhStudioVersion
        this.playServicesAdsVersion = playServicesAdsVersion
        this.gdprModuleVersion = gdprModuleVersion
        context.applicationContext
    }

    fun resetOrganic(context: Context) {
        SharePreferenceUtils.setIsOrganic(context.applicationContext, true)
    }

    fun isUnlimitedAdsEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_UNLIMITED_ADS, false)

    fun setUnlimitedAdsEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        appContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_UNLIMITED_ADS, enabled)
            .apply()
        SharePreferenceUtils.setIsOrganic(appContext, !enabled)
    }

    private fun sdkRows(): List<Pair<String, String>> = listOf(
        "ERain Studio / ads module" to erainStudioVersion,
        "Google Play Services Ads" to playServicesAdsVersion,
        "GDPR module" to gdprModuleVersion
    )

    private fun trackingRows(context: Context): List<Pair<String, String>> = listOf(
        "Adjust Token" to context.getString(R.string.adjust_token).ifBlank { "N/A" },
        "Facebook App ID" to context.getString(R.string.facebook_app_id).ifBlank { "N/A" },
        "Facebook Client Token" to context.getString(R.string.facebook_client_token).ifBlank { "N/A" },
        "TikTok Event Token" to context.getString(R.string.event_token).ifBlank { "N/A" },
        "AdMob Application ID" to adMobApplicationId(context).ifBlank { "N/A" }
    )

    private data class MediationProbe(
        val displayName: String,
        val adapterKeywords: List<String>,
        val sdkClassNames: List<String> = emptyList()
    )

    data class MediationRow(
        val networkName: String,
        val adapterClassName: String,
        val statusLabel: String,
        val detail: String,
        val latencyMs: Long
    )

    private val mediationProbes = listOf(
        MediationProbe(
            displayName = "AdMob (Google)",
            adapterKeywords = listOf("admob", "AdMobAdapter", "GoogleMobileAds"),
            sdkClassNames = listOf("com.google.android.gms.ads.MobileAds")
        ),
        MediationProbe(
            displayName = "Meta Audience Network",
            adapterKeywords = listOf("facebook", "Facebook"),
            sdkClassNames = listOf("com.facebook.ads.AudienceNetworkAds")
        ),
        MediationProbe(
            displayName = "AppLovin",
            adapterKeywords = listOf("applovin", "AppLovin"),
            sdkClassNames = listOf("com.applovin.sdk.AppLovinSdk")
        ),
        MediationProbe(
            displayName = "Unity Ads",
            adapterKeywords = listOf("unity", "Unity"),
            sdkClassNames = listOf("com.unity3d.ads.UnityAds")
        ),
        MediationProbe(
            displayName = "ironSource",
            adapterKeywords = listOf("ironsource", "IronSource"),
            sdkClassNames = listOf("com.ironsource.mediationsdk.IronSource")
        ),
        MediationProbe(
            displayName = "Mintegral",
            adapterKeywords = listOf("mintegral", "Mintegral", "mbridge"),
            sdkClassNames = listOf("com.mbridge.msdk.MBridgeSDK")
        ),
        MediationProbe(
            displayName = "Pangle",
            adapterKeywords = listOf("pangle", "Pangle", "bytedance"),
            sdkClassNames = listOf("com.bytedance.sdk.openadsdk.TTAdSdk")
        ),
        MediationProbe(
            displayName = "Vungle",
            adapterKeywords = listOf("vungle", "Vungle"),
            sdkClassNames = listOf("com.vungle.warren.Vungle", "com.vungle.ads.VungleAds")
        ),
        MediationProbe(
            displayName = "Chartboost",
            adapterKeywords = listOf("chartboost", "Chartboost"),
            sdkClassNames = listOf("com.chartboost.sdk.Chartboost")
        ),
        MediationProbe(
            displayName = "InMobi",
            adapterKeywords = listOf("inmobi", "InMobi"),
            sdkClassNames = listOf("com.inmobi.sdk.InMobiSdk")
        ),
        MediationProbe(
            displayName = "DT Exchange (Fyber)",
            adapterKeywords = listOf("fyber", "inneractive", "Fyber")
        ),
        MediationProbe(
            displayName = "AdColony (deprecated)",
            adapterKeywords = listOf("adcolony", "AdColony"),
            sdkClassNames = listOf("com.adcolony.sdk.AdColony")
        )
    )

    private fun mediationRows(): List<MediationRow> {
        val adapterMap = runCatching {
            MobileAds.getInitializationStatus()?.adapterStatusMap.orEmpty()
        }.getOrDefault(emptyMap())
        val matchedKeys = mutableSetOf<String>()
        val rows = mutableListOf<MediationRow>()

        mediationProbes.forEach { probe ->
            val matchedEntry = adapterMap.entries.firstOrNull { (adapterClass, _) ->
                probe.adapterKeywords.any { keyword ->
                    adapterClass.contains(keyword, ignoreCase = true)
                }
            }
            if (matchedEntry != null) {
                matchedKeys += matchedEntry.key
            }
            rows += probe.toMediationRow(matchedEntry)
        }

        adapterMap.forEach { (adapterClass, status) ->
            if (adapterClass in matchedKeys) return@forEach
            if (adapterClass.contains("MobileAds", ignoreCase = true)) return@forEach
            rows += MediationRow(
                networkName = adapterClass.simplifiedNetworkName(),
                adapterClassName = adapterClass.simpleClassName(),
                statusLabel = status.initializationState.name,
                detail = status.description.orEmpty(),
                latencyMs = status.latency.toLong()
            )
        }

        return rows.sortedWith(
            compareBy<MediationRow> { it.statusLabel.statusSortOrder() }
                .thenBy { it.networkName.lowercase() }
        )
    }

    private fun MediationProbe.toMediationRow(
        matchedEntry: Map.Entry<String, com.google.android.gms.ads.initialization.AdapterStatus>?
    ): MediationRow {
        val sdkPresent = sdkClassNames.any { classExists(it) }
        if (matchedEntry != null) {
            val status = matchedEntry.value
            return MediationRow(
                networkName = displayName,
                adapterClassName = matchedEntry.key.simpleClassName(),
                statusLabel = status.initializationState.name,
                detail = status.description.orEmpty(),
                latencyMs = status.latency.toLong()
            )
        }
        return MediationRow(
            networkName = displayName,
            adapterClassName = "-",
            statusLabel = if (sdkPresent) "SDK ONLY" else "MISSING",
            detail = if (sdkPresent) {
                "SDK detected, mediation adapter not found"
            } else {
                "Adapter/SDK not found in app"
            },
            latencyMs = 0L
        )
    }

    private fun classExists(className: String): Boolean =
        runCatching {
            Class.forName(className)
            true
        }.getOrDefault(false)

    private fun String.simpleClassName(): String = substringAfterLast('.')

    private fun String.simplifiedNetworkName(): String =
        simpleClassName()
            .replace("MediationAdapter", "", ignoreCase = true)
            .replace("Adapter", "", ignoreCase = true)
            .ifBlank { simpleClassName() }

    private fun String.statusSortOrder(): Int = when (this) {
        "READY" -> 0
        "NOT_READY", "NOT READY" -> 1
        "SDK ONLY" -> 2
        else -> 3
    }

    private fun adMobApplicationId(context: Context): String = runCatching {
        val info = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        val value = info.metaData?.getString("com.google.android.gms.ads.APPLICATION_ID")
            ?: info.metaData?.get("com.google.android.gms.ads.APPLICATION_ID")?.toString()
        value.orEmpty()
    }.getOrDefault("")

    private fun buildBadge(context: Context): String =
        "${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"} - v${appVersionName(context)}"

    private fun appVersionName(context: Context): String =
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "N/A"
        }.getOrDefault("N/A")

    @Composable
    fun SettingDialog(
        onDismiss: () -> Unit,
        onShowChecklist: () -> Unit,
        onApply: (Boolean) -> Unit
    ) {
        val context = LocalContext.current
        val initialUnlimitedAds = remember { isUnlimitedAdsEnabled(context) }
        var unlimitedAds by remember { mutableStateOf(initialUnlimitedAds) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Developer Setting") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unlimited Ads", fontWeight = FontWeight.Bold)
                            Text("Show configured placements for QA testing", fontSize = 12.sp)
                        }
                        Switch(
                            checked = unlimitedAds,
                            onCheckedChange = { unlimitedAds = it }
                        )
                    }
                    OutlinedButton(
                        onClick = onShowChecklist,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Checklist")
                    }
                    OutlinedButton(
                        onClick = {
                            resetOrganic(context)
                            Toast.makeText(context, "Organic reset", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset organic")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val changed = unlimitedAds != initialUnlimitedAds
                        setUnlimitedAdsEnabled(context, unlimitedAds)
                        Toast.makeText(context, "Unlimited Ads saved", Toast.LENGTH_SHORT).show()
                        onApply(changed)
                    }
                ) {
                    Text("Apply")
                }
            }
        )
    }

    @Composable
    fun ChecklistScreen(onBack: () -> Unit) {
        val context = LocalContext.current
        val adConfig = AdRemoteConfig.snapshot()
        val sdkRows = sdkRows()
        val trackingRows = trackingRows(context)
        val mediationRows = mediationRows()

//        val placementRows = AdRemoteConfig.placementNames.map { name ->
//            name to adConfig.placement(name)
//        }

        val placementRows = AdRemoteConfig.placementNames.map { name ->
            name to AdRemoteConfig.placement(name)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF161616))
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable(onClick = onBack)
                        .padding(4.dp)
                )
                Text(
                    text = "Developer Checklist",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 58.dp, bottom = 18.dp)
                    .background(Color(0xFFFFB238), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = buildBadge(context),
                    color = Color(0xFF2A0D16),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ChecklistSection("SDK & Libraries", sdkRows.size) {
                        sdkRows.forEach { (label, value) -> ChecklistRow(label, value) }
                    }
                }
                item {
                    ChecklistSection("Tracking - Adjust & Facebook", trackingRows.size) {
                        trackingRows.forEach { (label, value) -> ChecklistRow(label, value) }
                    }
                }
                item {
                    ChecklistSection("Mediation Networks", mediationRows.size) {
                        MediationTableHeader()
                        mediationRows.forEach { row -> MediationChecklistRow(row) }
                    }
                }
                item {
                    ChecklistSection("Ad Config - Release\n(ad_config.json)", placementRows.size) {
                        ChecklistRow("Config ID", adConfig.configId.ifBlank { "unknown" })
                        ChecklistRow("Ads enabled", adConfig.adsEnabled.toString())
                        Spacer(modifier = Modifier.height(8.dp))
                        placementRows.forEach { (name, placement) ->
                            PlacementChecklistRow(
                                name = name,
                                adId = placement.id.ifBlank { "N/A" },
                                enabled = placement.isEnable
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.setOnAdminAdToggleListener(onOpen: () -> Unit): Modifier {
    var tapCount by remember { mutableStateOf(0) }
    var lastTapMs by remember { mutableStateOf(0L) }
    return clickable {
        val now = SystemClock.elapsedRealtime()
        tapCount = if (now - lastTapMs > 4_000L) 1 else tapCount + 1
        lastTapMs = now
        if (tapCount >= 10) {
            tapCount = 0
            onOpen()
        }
    }
}

@Composable
private fun ChecklistSection(
    title: String,
    count: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF86011D), RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = count.toString(),
                color = Color(0xFFFFD7DE),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(Color(0xFF242426), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ChecklistRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFFBDBDBD),
            fontSize = 13.sp,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun MediationTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF32151D), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Network",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1.05f)
        )
        Text(
            text = "Mediation",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(0.9f)
        )
        Text(
            text = "Adapter",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1.05f)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun MediationChecklistRow(row: DevConfig.MediationRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(Color(0xFF111113), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1.05f)) {
                Text(
                    text = row.networkName,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
                val latencyText = if (row.latencyMs > 0L) "Latency: ${row.latencyMs}ms" else row.detail
                Text(
                    text = latencyText.ifBlank { row.detail },
                    color = Color(0xFF9E9E9E),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
            StatusPill(
                status = row.statusLabel,
                modifier = Modifier.weight(0.9f)
            )
            Text(
                text = row.adapterClassName,
                color = Color(0xFFBDBDBD),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                modifier = Modifier.weight(1.05f)
            )
        }
    }
}

@Composable
private fun StatusPill(status: String, modifier: Modifier = Modifier) {
    val normalizedStatus = if (status == "NOT_READY") "NOT READY" else status
    val background = when (normalizedStatus) {
        "READY" -> Color(0xFFE5F6E8)
        "SDK ONLY", "NOT READY" -> Color(0xFFFFF4D9)
        else -> Color(0xFFEEEEEE)
    }
    val foreground = when (normalizedStatus) {
        "READY" -> Color(0xFF35A853)
        "SDK ONLY", "NOT READY" -> Color(0xFFD9A21F)
        else -> Color(0xFF9E9E9E)
    }
    Box(
        modifier = modifier
            .padding(horizontal = 6.dp)
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = normalizedStatus,
            color = foreground,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun PlacementChecklistRow(name: String, adId: String, enabled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (enabled) "ON" else "OFF",
                color = if (enabled) Color(0xFFFFB238) else Color(0xFFFF6B6B),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
            )
        }
        Text(
            text = adId,
            color = Color(0xFFBDBDBD),
            fontSize = 12.sp
        )
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
}
