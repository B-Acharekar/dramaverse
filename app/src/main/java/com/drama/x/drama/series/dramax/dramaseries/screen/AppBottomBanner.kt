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
import androidx.compose.runtime.remember
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
    val placementName = "banner_collapsible_home"
    if (activity == null || !config.canRequest || !isNetworkAvailable(context)) {
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
                (LayoutInflater.from(viewContext)
                    .inflate(R.layout.layout_splash_banner, null, false) as FrameLayout).apply {
                    visibility = View.VISIBLE
                    findViewById<ShimmerFrameLayout>(com.ads.module.R.id.shimmer_container_banner)?.startShimmer()
                    Log.d(ADS_TAG, "[$placementName] REQUEST via ERain id=${config.id}")
                    ERainAd.getInstance().loadBannerFragment(
                        activity,
                        config.id,
                        this,
                        object : AdCallback() {
                            override fun onAdLoaded() {
                                visibility = View.VISIBLE
                                Log.d(ADS_TAG, "[$placementName] LOADED via ERain")
                            }

                            override fun onAdFailedToLoad(error: LoadAdError?) {
                                visibility = View.GONE
                                Log.e(ADS_TAG, "[$placementName] FAILED via ERain message=${error?.message}")
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
