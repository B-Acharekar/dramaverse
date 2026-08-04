package com.drama.x.drama.series.dramax.dramaseries.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ads.module.ads.ERainAd
import com.ads.module.funtion.AdCallback
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.ads.ADS_TAG
import com.drama.x.drama.series.dramax.dramaseries.ads.AdRemoteConfig
import com.drama.x.drama.series.dramax.dramaseries.ads.isNetworkAvailable
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.LoadAdError

val AppBottomBannerHeight = 50.dp

@Composable
fun shouldShowAppBottomBanner(): Boolean = AdRemoteConfig.bannerCollapsibleHome.canRequest

@Composable
fun AppBottomBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findBottomBannerActivity() }
    val config = AdRemoteConfig.bannerCollapsibleHome
    val placementName = "banner_collapsible_all"
    var isAdLoaded by remember { mutableStateOf(true) } // Start as true to show loading state
    
    Log.d(ADS_TAG, "[$placementName] AppBottomBanner check: activity=${activity != null} canRequest=${config.canRequest} network=${isNetworkAvailable(context)} id=${config.id}")
    
    if (activity == null || !config.canRequest || !isNetworkAvailable(context)) {
        Log.d(ADS_TAG, "[$placementName] SKIPPED - preconditions not met")
        return
    }

    if (!isAdLoaded) {
        Log.d(ADS_TAG, "[$placementName] Not rendering - ad failed to load")
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppBottomBannerHeight)
            .navigationBarsPadding()
            .background(Color(0xFF111113))
    ) {
        AndroidView(
            factory = { viewContext ->
                Log.d(ADS_TAG, "[$placementName] AndroidView factory called")
                (LayoutInflater.from(viewContext)
                    .inflate(R.layout.layout_splash_banner, null, false) as FrameLayout).apply {
                    minimumHeight = 50.dpToPx(viewContext)
                    visibility = View.VISIBLE
                    findViewById<ShimmerFrameLayout>(com.ads.module.R.id.shimmer_container_banner)?.startShimmer()

                    if (findViewById<FrameLayout>(com.ads.module.R.id.banner_container) == null) {
                        Log.e(ADS_TAG, "[$placementName] XML missing banner_container")
                        return@apply
                    }

                    post {
                        Log.d(
                            ADS_TAG,
                            "[$placementName] XML host laidOut containerWidth=$width containerHeight=$height " +
                                "visibility=$visibility attached=$isAttachedToWindow parent=${parent?.javaClass?.simpleName}"
                        )
                    }
                    Log.d(ADS_TAG, "[$placementName] REQUEST via ERain id=${config.id}")
                    ERainAd.getInstance().loadBannerFragment(
                        activity,
                        config.id,
                        this,
                        object : AdCallback() {
                            override fun onAdLoaded() {
                                visibility = View.VISIBLE
                                isAdLoaded = true
                                post {
                                    Log.d(
                                        ADS_TAG,
                                        "[$placementName] LOADED via ERain XML containerWidth=$width " +
                                            "containerHeight=$height visibility=$visibility attached=$isAttachedToWindow " +
                                            "childCount=$childCount parent=${parent?.javaClass?.simpleName}"
                                    )
                                }
                            }

                            override fun onAdFailedToLoad(error: LoadAdError?) {
                                visibility = View.GONE
                                isAdLoaded = false
                                Log.e(
                                    ADS_TAG,
                                    "[$placementName] FAILED via ERain code=${error?.code}, domain=${error?.domain}, " +
                                        "message=${error?.message}, responseInfo=${error?.responseInfo}, cause=${error?.cause}"
                                )
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(AppBottomBannerHeight)
        )
    }
}

private tailrec fun Context.findBottomBannerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findBottomBannerActivity()
    else -> null
}

private fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()
