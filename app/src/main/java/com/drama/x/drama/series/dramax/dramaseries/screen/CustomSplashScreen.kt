package com.drama.x.drama.series.dramax.dramaseries.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.ads.ADS_TAG
import com.drama.x.drama.series.dramax.dramaseries.ads.AdRemoteConfig
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsInitializationState
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.ads.RemoteConfigUtils
import com.drama.x.drama.series.dramax.dramaseries.ads.isNetworkAvailable
import com.ads.module.ads.ERainAd
import com.ads.module.funtion.AdCallback
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val MIN_SPLASH_BANNER_VISIBLE_MS = 2_200L
private const val MAX_SPLASH_BANNER_WAIT_MS = 6_000L

@Composable
fun CustomSplashScreen(
    uninstallFlow: Boolean = false,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val startMs = remember { SystemClock.elapsedRealtime() }
    val hasNavigated = remember { AtomicBoolean(false) }
    val interstitialResolved = remember { AtomicBoolean(false) }
    val bannerLoaded = remember { AtomicBoolean(false) }
    val bannerVisibleSinceMs = remember { AtomicLong(0L) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var splashAdsConfigReady by remember { mutableStateOf(false) }

    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose {}
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
        onDispose {
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            activity.window.navigationBarColor = android.graphics.Color.BLACK
        }
    }

    fun logStage(stage: String) {
        Log.d(ADS_TAG, "$stage elapsedMs=${SystemClock.elapsedRealtime() - startMs}")
    }

    fun continueFromSplash(reason: String) {
        mainHandler.post {
            if (hasNavigated.compareAndSet(false, true)) {
                Log.d(ADS_TAG, "SPLASH_NAVIGATE reason=$reason elapsedMs=${SystemClock.elapsedRealtime() - startMs}")
                onFinished()
            } else {
                Log.d(ADS_TAG, "SPLASH_NAVIGATION_SKIPPED_DUPLICATE reason=$reason elapsedMs=${SystemClock.elapsedRealtime() - startMs}")
            }
        }
    }

    fun showInterstitialAfterBannerWindow(activity: Activity) {
        fun waitUntilReadyOrTimedOut() {
            val bannerElapsedMs = SystemClock.elapsedRealtime() - bannerVisibleSinceMs.get()
            val minWindowElapsed = bannerElapsedMs >= MIN_SPLASH_BANNER_VISIBLE_MS
            val maxWaitElapsed = bannerElapsedMs >= MAX_SPLASH_BANNER_WAIT_MS
            if ((bannerLoaded.get() && minWindowElapsed) || maxWaitElapsed || hasNavigated.get()) {
                Log.d(
                    ADS_TAG,
                    "SPLASH_INTER_SHOW_GATE bannerLoaded=${bannerLoaded.get()} " +
                        "bannerElapsedMs=$bannerElapsedMs maxWaitElapsed=$maxWaitElapsed"
                )
                if (!hasNavigated.get()) {
                    AdsManager.showSplashInterstitialIfReady(activity) {
                        continueFromSplash("interstitial_closed")
                    }
                }
            } else {
                mainHandler.postDelayed({ waitUntilReadyOrTimedOut() }, 120L)
            }
        }
        waitUntilReadyOrTimedOut()
    }

    LaunchedEffect(activity) {
        logStage("SPLASH_FLOW_START")
        if (activity == null) {
            continueFromSplash("missing_activity")
            return@LaunchedEffect
        }

        AdsInitializationState.awaitMobileAdsReady(timeoutMs = 4_000L).also { ready ->
            Log.d(ADS_TAG, "SPLASH_MOBILE_ADS_READY=$ready elapsedMs=${SystemClock.elapsedRealtime() - startMs}")
        }

        logStage("SPLASH_REMOTE_CONFIG_START")
        val remoteResult = RemoteConfigUtils.fetchAndApply(
            RemoteConfigUtils.configure(context.applicationContext),
            timeoutMs = 3_000L
        )
        Log.d(ADS_TAG, "SPLASH_REMOTE_CONFIG_RESULT success=$remoteResult elapsedMs=${SystemClock.elapsedRealtime() - startMs}")
        bannerVisibleSinceMs.set(SystemClock.elapsedRealtime())
        splashAdsConfigReady = true

        AdsManager.loadSplashInterstitial(
            activity = activity,
            uninstallFlow = uninstallFlow,
            onLoaded = {
                if (interstitialResolved.compareAndSet(false, true) && !hasNavigated.get()) {
                    Log.d(ADS_TAG, "SPLASH_INTER_READY waiting_for_erain_banner")
                    showInterstitialAfterBannerWindow(activity)
                } else {
                    Log.d(ADS_TAG, "SPLASH_NAVIGATION_SKIPPED_DUPLICATE reason=interstitial_loaded_after_resolution")
                }
            },
            onFailed = {
                if (interstitialResolved.compareAndSet(false, true)) {
                    val bannerElapsedMs = SystemClock.elapsedRealtime() - bannerVisibleSinceMs.get()
                    val continueDelayMs = (MIN_SPLASH_BANNER_VISIBLE_MS - bannerElapsedMs).coerceAtLeast(0L)
                    Log.d(
                        ADS_TAG,
                        "SPLASH_INTER_FAILED delayingNavigationForBannerMs=$continueDelayMs bannerElapsedMs=$bannerElapsedMs"
                    )
                    mainHandler.postDelayed({
                        continueFromSplash("interstitial_failed_or_ineligible")
                    }, continueDelayMs)
                }
            }
        )
        AdsManager.loadNativeLanguage(activity, firstVisit = true)
        AdsManager.preloadOnboardingAds(activity, firstVisit = true)

        delay(30_000L)
        if (interstitialResolved.compareAndSet(false, true) && !hasNavigated.get()) {
            logStage("SPLASH_INTER_TIMEOUT")
            continueFromSplash("interstitial_timeout")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        DramaXGlowBackground()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringCenter = Offset(size.width / 2f, size.height * 0.47f)
            val base = size.minDimension * 0.52f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.052f),
                        Color.Transparent
                    ),
                    center = ringCenter,
                    radius = base * 1.34f
                ),
                radius = base * 1.34f,
                center = ringCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.11f),
                        Color(0x2EFFA8AE),
                        Color.Transparent
                    ),
                    center = ringCenter,
                    radius = base * 0.9f
                ),
                radius = base * 0.9f,
                center = ringCenter
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, top = 44.dp, end = 10.dp, bottom = 94.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.icon_2),
                contentDescription = null,
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name_display),
                color = Color(0xFFFFA8AE),
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                color = Color(0xFFE8E2E7),
                fontSize = 25.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(26.dp))
//            Text(
//                text = stringResource(R.string.splash_description),
//                color = Color(0xFFB6959B),
//                fontSize = 12.sp,
//                lineHeight = 17.sp,
//                textAlign = TextAlign.Center,
//                fontWeight = FontWeight.SemiBold,
//                letterSpacing = 0.sp
//            )
            Spacer(modifier = Modifier.height(38.dp))
            LoadingDots()
            Spacer(modifier = Modifier.weight(1.35f))
        }

        if (splashAdsConfigReady) {
            SplashBanner(
                uninstallFlow = uninstallFlow,
                onBannerLoaded = {
                    bannerVisibleSinceMs.set(SystemClock.elapsedRealtime())
                    bannerLoaded.set(true)
                    Log.d(ADS_TAG, "SPLASH_BANNER_READY_FOR_INTER_GATE")
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun SplashBanner(
    uninstallFlow: Boolean,
    onBannerLoaded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val config = if (uninstallFlow) AdRemoteConfig.bannerSplashUninstall else AdRemoteConfig.bannerSplash
    val placementName = if (uninstallFlow) "banner_splash_uninstall" else "banner_splash"
    if (activity == null || !config.canRequest || !isNetworkAvailable(context)) {
        Log.d(ADS_TAG, "[$placementName] DISABLED enabled=${config.isEnable} id=${config.id}")
        return
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .background(Color(0xFF111113))
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { viewContext ->
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
                                onBannerLoaded()
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
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

@Composable
fun DramaXGlowBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFA4AD), Color(0x99FF4857), Color.Transparent)
            ),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.82f, size.height * 0.14f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFB31F), Color(0xCCFF3957), Color.Transparent)
            ),
            radius = size.width * 0.34f,
            center = Offset(size.width * 0.18f, size.height * 0.78f)
        )
    }
}

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingDots")
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.72f,
                targetValue = 1.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 560,
                        delayMillis = index * 150,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "loadingDot$index"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.42f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 560,
                        delayMillis = index * 150,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "loadingDotAlpha$index"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .background(
                        color = Color(0xFFFFA4AD),
                        shape = CircleShape
                    )
            )
        }
    }
}
