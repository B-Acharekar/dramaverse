package com.drama.x.drama.series.dramax.dramaseries.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.drama.x.drama.series.dramax.dramaseries.ads.ADS_TAG
import com.drama.x.drama.series.dramax.dramaseries.ads.NativeAdState
import com.ads.module.ads.ERainAd
import com.facebook.shimmer.ShimmerFrameLayout

@Composable
fun ErainNativeAdHost(
    placementName: String,
    state: NativeAdState,
    modifier: Modifier = Modifier,
    height: Dp = 320.dp,
    showFailureMessage: Boolean = false
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    when (state) {
        NativeAdState.Idle,
        is NativeAdState.Disabled,
        is NativeAdState.Failed -> {
            Log.d(ADS_TAG, "[$placementName] CONTAINER_HIDDEN state=${state.javaClass.simpleName}")
            if (showFailureMessage && state is NativeAdState.Failed) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(Color(0x26222226), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Network error: Ads unavailable",
                        color = Color(0xFFB9B0B5),
                        fontSize = 12.sp,
                        letterSpacing = 0.sp
                    )
                }
            }
        }

        NativeAdState.Loading -> {
            AdSkeleton(
                modifier = modifier,
                height = height
            )
        }

        is NativeAdState.Loaded -> {
            AndroidView(
                factory = { viewContext ->
                    FrameLayout(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        minimumHeight = height.value.toInt().dpToPx(viewContext)
                        val adContainer = FrameLayout(viewContext).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                            )
                        }
                        val shimmer = ShimmerFrameLayout(viewContext).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            addView(FrameLayout(viewContext).apply {
                                setBackgroundColor(android.graphics.Color.argb(45, 255, 255, 255))
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            })
                            startShimmer()
                        }
                        addView(adContainer)
                        addView(shimmer)
                        populateErainNative(activity, placementName, state, adContainer, shimmer)
                    }
                },
                update = { root ->
                    val adContainer = root.getChildAt(0) as? FrameLayout ?: return@AndroidView
                    val shimmer = root.getChildAt(1) as? ShimmerFrameLayout ?: return@AndroidView
                    populateErainNative(activity, placementName, state, adContainer, shimmer)
                    root.post {
                        Log.d(
                            ADS_TAG,
                            "[$placementName] DISPLAYED containerWidth=${root.width} " +
                                "containerHeight=${root.height} attached=${root.isAttachedToWindow}"
                        )
                    }
                },
                modifier = modifier
                    .fillMaxWidth()
                    .height(height)
            )
        }
    }
}

private fun populateErainNative(
    activity: Activity?,
    placementName: String,
    state: NativeAdState.Loaded,
    adContainer: FrameLayout,
    shimmer: ShimmerFrameLayout
) {
    if (activity == null || activity.isFinishing || activity.isDestroyed) {
        Log.d(ADS_TAG, "[$placementName] POPULATE skipped activity_invalid")
        return
    }
    Log.d(ADS_TAG, "[$placementName] POPULATE via ERain")
    ERainAd.getInstance().populateNativeAdView(activity, state.nativeAd, adContainer, shimmer)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

@Composable
private fun AdSkeleton(
    modifier: Modifier,
    height: Dp
) {
    val transition = rememberInfiniteTransition(label = "adSkeleton")
    val shimmerOffset by transition.animateFloat(
        initialValue = -320f,
        targetValue = 920f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "adSkeletonOffset"
    )
    val shimmer = Brush.linearGradient(
        colors = listOf(
            Color(0xFF211417),
            Color(0xFF4B242A),
            Color(0xFF211417)
        ),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 320f, 320f)
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(0xFF171214), RoundedCornerShape(8.dp))
            .border(0.7.dp, Color(0xFF513039), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 20.dp)
                .background(shimmer, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(22.dp)
                .background(shimmer, RoundedCornerShape(5.dp))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(shimmer, RoundedCornerShape(5.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(shimmer, RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(shimmer, RoundedCornerShape(20.dp))
        )
    }
}
