package com.drama.x.drama.series.dramax.dramaseries.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.ads.ADS_TAG
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.ads.NativeAdState
import com.drama.x.drama.series.dramax.dramaseries.model.OnboardingPage
import com.drama.x.drama.series.dramax.dramaseries.model.OnboardingViewModel
import com.drama.x.drama.series.dramax.dramaseries.model.OnboardingVisual
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowCompat.getInsetsController
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ads.module.ads.wrapper.ApNativeAd
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.drama.x.drama.series.dramax.dramaseries.BuildConfig as AppBuildConfig


private const val FULLSCREEN_NATIVE_PAGER_INDEX = 2

private val ReferenceWidth = 360.dp   // baseline design width
private val ReferenceHeight = 780.dp  // baseline design height

private data class ScreenScale(
    val widthScale: Float,
    val heightScale: Float
)

private val LocalScreenScale = staticCompositionLocalOf { ScreenScale(1f, 1f) }

@Composable
private fun rememberScreenScale(): ScreenScale {
    val configuration = LocalConfiguration.current
    // Clamp so extreme devices (small feature phones / huge tablets) don't
    // blow the layout up or shrink it into unreadability.
    val widthScale = (configuration.screenWidthDp / ReferenceWidth.value)
        .coerceIn(0.80f, 1.35f)
    val heightScale = (configuration.screenHeightDp / ReferenceHeight.value)
        .coerceIn(0.78f, 1.40f)
    return ScreenScale(widthScale, heightScale)
}

/** Scale a Dp against the current screen's height ratio. */
private fun Dp.h(scale: ScreenScale): Dp = this * scale.heightScale

/** Scale a Dp against the current screen's width ratio. */
private fun Dp.w(scale: ScreenScale): Dp = this * scale.widthScale

@Composable
fun OnboardingScreen(
    onEntered: () -> Unit,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Hide system nav bar
    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val controller = getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
        onDispose {
            if (window != null) {
                val controller = getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    val initialOnboardingAdState = remember {
        if (AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE) {
            NativeAdState.Loading
        } else {
            NativeAdState.Disabled("onboarding_ads_disabled_for_false_version")
        }
    }
    var onboardingPageOneAdState by remember { mutableStateOf<NativeAdState>(initialOnboardingAdState) }
    var onboardingFullscreenAdState by remember { mutableStateOf<NativeAdState>(initialOnboardingAdState) }
    var onboardingWelcomeAdState by remember { mutableStateOf<NativeAdState>(initialOnboardingAdState) }
    val showFullNativePage = onboardingFullscreenAdState is NativeAdState.Loading ||
            onboardingFullscreenAdState is NativeAdState.Loaded
    val pagerPageCount = uiState.pages.size + if (showFullNativePage) 1 else 0
    val pagerState = rememberPagerState(pageCount = { pagerPageCount })


    //ad height
    var adHeight by remember { mutableStateOf(0.dp) }


    LaunchedEffect(Unit) {
        onEntered()
        if (!AppBuildConfig.ENABLE_ONBOARDING_ADS_FOR_LIVE) {
            return@LaunchedEffect
        }
        activity?.let {
            AdsManager.loadNativeOnboardingPageOne(it, firstVisit = true)
            AdsManager.loadNativeOnboardingFullscreen(it, firstVisit = true)
            AdsManager.loadNativeOnboardingWelcome(it, firstVisit = true)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectPage(pagerState.currentPage.toOnboardingPageIndex(showFullNativePage))
    }

    DisposableEffect(Unit) {
        val pageOneObserver = Observer<NativeAdState> { state ->
            onboardingPageOneAdState = state
            Log.d(ADS_TAG, "ONBOARDING_NATIVE_1 observer state=${state.javaClass.simpleName}")
        }
        val fullscreenObserver = Observer<NativeAdState> { state ->
            onboardingFullscreenAdState = state
            Log.d(ADS_TAG, "ONBOARDING_FULLSCREEN_NATIVE observer state=${state.javaClass.simpleName}")
        }
        val welcomeObserver = Observer<NativeAdState> { state ->
            onboardingWelcomeAdState = state
            Log.d(ADS_TAG, "ONBOARDING_NATIVE_WELCOME observer state=${state.javaClass.simpleName}")
        }
        AdsManager.nativeOnboardingPageOneAdLive.observeForever(pageOneObserver)
        AdsManager.nativeOnboardingFullscreenAdLive.observeForever(fullscreenObserver)
        AdsManager.nativeOnboardingWelcomeAdLive.observeForever(welcomeObserver)
        onDispose {
            AdsManager.nativeOnboardingPageOneAdLive.removeObserver(pageOneObserver)
            AdsManager.nativeOnboardingFullscreenAdLive.removeObserver(fullscreenObserver)
            AdsManager.nativeOnboardingWelcomeAdLive.removeObserver(welcomeObserver)
        }
    }

    CompositionLocalProvider(LocalScreenScale provides rememberScreenScale()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF111113))
        ) {
            OnboardingGlowBackground()
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            ) { pageIndex ->
                if (showFullNativePage && pageIndex == FULLSCREEN_NATIVE_PAGER_INDEX) {
                    OnboardingFullscreenNativeAd(
                        state = onboardingFullscreenAdState
                    )
                    return@HorizontalPager
                }

                val onboardingPageIndex = pageIndex.toOnboardingPageIndex(showFullNativePage)
                val page = uiState.pages[onboardingPageIndex]
                val selectedPage = pagerState.currentPage.toOnboardingPageIndex(showFullNativePage)
                val onNext: () -> Unit = {
                    if (pagerState.currentPage == pagerPageCount - 1) {
                        onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage + 1,
                                animationSpec = tween(360, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                }

                if (page.visual == OnboardingVisual.Welcome) {
                    WelcomePageContent(
                        page = page,
                        pageIndex = onboardingPageIndex,
                        pageCount = uiState.pages.size,
                        selectedPage = selectedPage,
                        nativeAdState = onboardingWelcomeAdState,
                        onNext = onNext
                    )
                } else {
                    OnboardingPageContent(
                        page = page,
                        pageIndex = onboardingPageIndex,
                        pageCount = uiState.pages.size,
                        selectedPage = selectedPage,
                        nativeAdState = when (onboardingPageIndex) {
                            0 -> onboardingPageOneAdState
                            else -> NativeAdState.Disabled("not_this_page")
                        },
                        onNext = onNext
                    )
                }
            }
        }
    }
}

private fun Int.toOnboardingPageIndex(showFullNativePage: Boolean): Int =
    if (showFullNativePage && this > FULLSCREEN_NATIVE_PAGER_INDEX) this - 1 else this

//@Composable
//private fun OnboardingPageContent(
//    page: OnboardingPage,
//    pageIndex: Int,
//    pageCount: Int,
//    selectedPage: Int,
//    nativeAdState: NativeAdState,
//    onNext: () -> Unit
//) {
//    val scale = LocalScreenScale.current
//    val pageHasNativePlacement = pageIndex == 0
//    val shouldReserveNativeSpace =
//        pageHasNativePlacement && (nativeAdState is NativeAdState.Loading || nativeAdState is NativeAdState.Loaded)
//
//    // --- Image geometry scales with screen height, keeping the same
//    //     proportion of the screen it originally occupied. ---
//    val visualHeight = (if (shouldReserveNativeSpace) 320.dp else 540.dp).h(scale)
//
//    // --- Everything below reacts to ad occurrence, still screen-relative ---
//    val textTopPadding = visualHeight -
//            (if (shouldReserveNativeSpace) 40.dp.h(scale) else 0.dp)
//
//    val imagebottompadding = if (shouldReserveNativeSpace) 320.dp.h(scale) else 0.dp
//    val rowBottomPadding = if (shouldReserveNativeSpace) 320.dp.h(scale) else 18.dp.h(scale)
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        Column() {
//            // Fixed image — height/position constant relative to screen size
//            Box(
//                modifier = Modifier
////                    .align(Alignment.TopCenter)
//                    .fillMaxWidth()
//                    .weight(1f)
//                    .padding(horizontal = 22.dp.w(scale)),
//                contentAlignment = Alignment.Center
//            ) {
//                when (page.visual) {
//                    OnboardingVisual.DramaPhone -> DramaPhoneVisual(painterResource(R.drawable.onboarding1image))
//                    OnboardingVisual.Collections -> CollectionsVisual()
//                    OnboardingVisual.RomancePhone -> RomanceVisual()
//                    OnboardingVisual.Welcome -> Unit
//                }
//                // Title + subtitle — shifts based on ad occurrence
//                Column(
//                    modifier = Modifier
//                        .align(Alignment.BottomCenter)
//                        .fillMaxWidth()
//                        .padding(horizontal = 22.dp.w(scale))
////                    .padding(top = textTopPadding)
//                    ,
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    OnboardingTitle(page)
//                    Spacer(modifier = Modifier.height(16.dp.h(scale)))
//                    Text(
//                        text = stringResource(page.description),
//                        color = Color(0xFFC1A4A9),
//                        fontSize = 14.sp,
//                        lineHeight = 20.sp,
//                        textAlign = TextAlign.Center,
//                        fontWeight = FontWeight.SemiBold,
//                        letterSpacing = 0.sp,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 10.dp.w(scale))
//                    )
//                }
//            }
//
//            Column(
////                modifier = Modifier.align(Alignment.BottomCenter)
//            ){
//                // Indicator + button — shifts based on ad occurrence
//                Row(
//                    modifier = Modifier
////                    .align(Alignment.BottomCenter)
//                        .fillMaxWidth()
//                        .padding(horizontal = 22.dp.w(scale))
////                    .padding(bottom = rowBottomPadding)
//                    ,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    PageIndicator(
//                        pageCount = pageCount,
//                        selectedPage = selectedPage,
//                        modifier = Modifier.weight(1f)
//                    )
//                    OnboardingActionButton(
//                        label = if (pageIndex == pageCount - 1) R.string.get_started else R.string.next_btn,
//                        onClick = onNext
//                    )
//                }
//
//                if (shouldReserveNativeSpace) {
//                    OnboardingNativeAd(
//                        state = nativeAdState,
//                        modifier = Modifier
////                        .align(Alignment.BottomCenter)
//                            .requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
//                    )
//                }
//            }
//        }
//
//    }
//}


@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int,
    pageCount: Int,
    selectedPage: Int,
    nativeAdState: NativeAdState,
    onNext: () -> Unit
) {
    val scale = LocalScreenScale.current
    val pageHasNativePlacement = pageIndex == 0
    val shouldReserveNativeSpace =
        pageHasNativePlacement && (nativeAdState is NativeAdState.Loading || nativeAdState is NativeAdState.Loaded)
    var subtitleAreaHeightPx by remember { mutableStateOf(0) }
    val subtitleAreaHeight = with(LocalDensity.current) { subtitleAreaHeightPx.toDp() }
    val controlsTopSpacer = if (page.visual == OnboardingVisual.DramaPhone) {
        subtitleAreaHeight
    } else {
        0.dp
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- Section 1: visual media (enlarged) with title+desc overlapped at the bottom ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Image gets its own tighter padding → appears bigger
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp.w(scale)),
                contentAlignment = Alignment.Center
            ) {
                when (page.visual) {
                    OnboardingVisual.DramaPhone -> DramaPhoneVisual(
                        painter = painterResource(R.drawable.onboarding1image),
                        bottomExtensionPx = subtitleAreaHeightPx
                    )
                    OnboardingVisual.Collections -> CollectionsVisual()
                    OnboardingVisual.RomancePhone -> RomanceVisual()
                    OnboardingVisual.Welcome -> Unit
                }
            }

            // Text keeps its original padding/size, unaffected by the image's enlargement
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = if (page.visual == OnboardingVisual.DramaPhone) {
                            subtitleAreaHeightPx.toFloat()
                        } else {
                            0f
                        }
                    }
                    .padding(horizontal = 22.dp.w(scale)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingTitle(page)
                Column(
                    modifier = Modifier.onSizeChanged { subtitleAreaHeightPx = it.height },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp.h(scale)))
                    Text(
                        text = stringResource(page.description),
                        color = Color(0xFFC1A4A9),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp.w(scale))
                    )
                }
            }
        }

        // --- Section 2: indicator + button (and native ad, when reserved) ---
        Column {
            Spacer(modifier = Modifier.height(controlsTopSpacer))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp.w(scale))
                    .padding(bottom = if(!shouldReserveNativeSpace) 18.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageIndicator(
                    pageCount = pageCount,
                    selectedPage = selectedPage,
                    modifier = Modifier.weight(1f)
                )
                OnboardingActionButton(
                    label = if (pageIndex == pageCount - 1) R.string.get_started else R.string.next_btn,
                    onClick = onNext
                )
            }

            if (shouldReserveNativeSpace) {
                OnboardingNativeAd(
                    state = nativeAdState,
                    modifier = Modifier
                        .requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageWithAdContent(
    page: OnboardingPage,
    pageCount: Int,
    selectedPage: Int,
    nativeAdState: NativeAdState,
    onNext: () -> Unit
) {
    val scale = LocalScreenScale.current
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(404.dp.h(scale))
                .padding(horizontal = 22.dp.w(scale), vertical = 2.dp.h(scale)),
            contentAlignment = Alignment.TopCenter
        ) {
            DramaPhoneVisual(painterResource(R.drawable.onboarding1image))
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 22.dp.w(scale))
                .padding(top = 344.dp.h(scale)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnboardingTitle(page)
            Spacer(modifier = Modifier.height(16.dp.h(scale)))
            Text(
                text = stringResource(page.description),
                color = Color(0xFFC1A4A9),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp.w(scale))
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 22.dp.w(scale))
                .padding(bottom = 320.dp.h(scale)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageIndicator(
                pageCount = pageCount,
                selectedPage = selectedPage,
                modifier = Modifier.weight(1f)
            )
            OnboardingActionButton(
                label = R.string.next_btn,
                onClick = onNext
            )
        }

        OnboardingNativeAd(
            state = nativeAdState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
        )
    }
}

@Composable
private fun OnboardingTitle(page: OnboardingPage) {
    if (page.accentTitle == 0) {
        Text(
            text = stringResource(page.title),
            color = Color.White,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp
        )
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(page.title),
                color = Color.White,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
            Text(
                text = stringResource(page.accentTitle),
                color = Color(0xFFFFB2B9),
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun OnboardingNativeAd(
    state: NativeAdState,
    modifier: Modifier = Modifier
) {
    val scale = LocalScreenScale.current
    ErainNativeAdHost(
        placementName = "onboarding_page_native",
        state = state,
        modifier = modifier,
        height = 320.dp.h(scale),
        showFailureMessage = true
    )
}

@Composable
private fun OnboardingFullscreenNativeAd(
    state: NativeAdState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2111113)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is NativeAdState.Loaded -> {
                    NativeAdFullscreenView(
                        apNativeAd = state.nativeAd,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                is NativeAdState.Loading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading ad…",
                            color = Color(0xFFB9B0B5),
                            fontSize = 13.sp
                        )
                    }
                }
                else -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NativeAdFullscreenView(
    apNativeAd: ApNativeAd,
    modifier: Modifier = Modifier
) {
    val admobNativeAd = apNativeAd.admobNativeAd

    if (admobNativeAd == null) {
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            LayoutInflater.from(context)
                .inflate(R.layout.layout_native_full_screen, null) as NativeAdView
        },
        update = { adView ->
            bindNativeAd(adView, admobNativeAd)
        }
    )
}

private fun bindNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    val iconView = adView.findViewById<android.widget.ImageView>(R.id.ad_app_icon)
    val headlineView = adView.findViewById<android.widget.TextView>(R.id.ad_headline)
    val advertiserView = adView.findViewById<android.widget.TextView>(R.id.ad_advertiser)
    val bodyView = adView.findViewById<android.widget.TextView>(R.id.ad_body)
    val ctaView = adView.findViewById<android.widget.Button>(R.id.ad_call_to_action)

    adView.mediaView = mediaView
    adView.iconView = iconView
    adView.headlineView = headlineView
    adView.advertiserView = advertiserView
    adView.bodyView = bodyView
    adView.callToActionView = ctaView

    headlineView.text = nativeAd.headline
    bodyView.text = nativeAd.body
    ctaView.text = nativeAd.callToAction

    nativeAd.icon?.let { icon ->
        iconView.setImageDrawable(icon.drawable)
        iconView.visibility = android.view.View.VISIBLE
    } ?: run {
        iconView.visibility = android.view.View.GONE
    }

    if (nativeAd.advertiser != null) {
        advertiserView.text = nativeAd.advertiser
        advertiserView.visibility = android.view.View.VISIBLE
    } else {
        advertiserView.visibility = android.view.View.GONE
    }

    nativeAd.mediaContent?.let { mediaView.mediaContent = it }

    adView.setNativeAd(nativeAd)
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    selectedPage: Int,
    modifier: Modifier = Modifier
) {
    val scale = LocalScreenScale.current
    Row(
        modifier = modifier.padding(start = 4.dp.w(scale)),
        horizontalArrangement = Arrangement.spacedBy(7.dp.w(scale)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .height(5.dp.h(scale))
                    .width(if (selectedPage == index) 27.dp.w(scale) else 5.dp.w(scale))
                    .background(
                        color = if (selectedPage == index) Color.White else Color(0xFF3C3C3F),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun OnboardingActionButton(
    label: Int,
    onClick: () -> Unit
) {
    val scale = LocalScreenScale.current
    Box(
        modifier = Modifier
            .height(44.dp.h(scale))
            .width(112.dp.w(scale))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF86011D),
                        Color(0xFF140105)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(label),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun OnboardingGlowBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF111113))
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xAA5E1724),
                    Color(0x33311A22),
                    Color.Transparent
                ),
                start = Offset(size.width, 0f),
                end = Offset(size.width * 0.22f, size.height * 0.62f)
            )
        )
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xCC2F165B),
                    Color(0x55201035),
                    Color.Transparent
                ),
                start = Offset(0f, size.height),
                end = Offset(size.width * 0.74f, size.height * 0.23f)
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, Color(0x99111113), Color(0xFF111113)),
                startY = size.height * 0.38f,
                endY = size.height
            )
        )
    }
}

@Composable
private fun DramaPhoneVisual(
    painter: Painter,
    bottomExtensionPx: Int = 0
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val intrinsicSize = painter.intrinsicSize
        val aspectRatio = if (intrinsicSize.width > 0f && intrinsicSize.height > 0f) {
            intrinsicSize.width / intrinsicSize.height
        } else {
            1f
        }
        val heightAtFullWidth = maxWidth / aspectRatio
        val baseHeight = if (heightAtFullWidth <= maxHeight) {
            heightAtFullWidth
        } else {
            maxHeight
        }
        val bottomExtension = with(density) { bottomExtensionPx.toDp() }
        val imageHeight = baseHeight + bottomExtension
        val imageWidth = imageHeight * aspectRatio

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .requiredWidth(imageWidth)
                .requiredHeight(imageHeight)
                .graphicsLayer { translationY = bottomExtensionPx.toFloat() }
                .clip(RoundedCornerShape(50.dp))
        )
    }
}

@Composable
private fun CollectionsVisual() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.onboarding2image),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.06f)
                .clip(RoundedCornerShape(26.dp))
        )
    }
}

@Composable
private fun RomanceVisual() {
    val painter = painterResource(R.drawable.onboarding3image)
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(Color.Transparent, shape),
        contentAlignment = Alignment.TopCenter
    ) {
        val intrinsicSize = painter.intrinsicSize
        val aspectRatio = if (intrinsicSize.width > 0f && intrinsicSize.height > 0f) {
            intrinsicSize.width / intrinsicSize.height
        } else {
            1f
        }
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(aspectRatio)   // CHANGED — replaces wrapContentSize(); sizes the layout to the actual fitted dimensions
                .clip(RoundedCornerShape(16.dp))
        )
    }
}
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun WelcomePageContent(
    page: OnboardingPage,
    pageIndex: Int,
    pageCount: Int,
    selectedPage: Int,
    nativeAdState: NativeAdState,
    onNext: () -> Unit
) {
    val scale = LocalScreenScale.current
    val shouldReserveNativeSpace = nativeAdState is NativeAdState.Loading || nativeAdState is NativeAdState.Loaded
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.welocome_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth
        )

        // Frosted glass card — truly centered on screen, scales with screen height
        Column(
            modifier = Modifier
                .align(if (shouldReserveNativeSpace) Alignment.TopCenter else Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 22.dp.w(scale))
                .then(
                    if (shouldReserveNativeSpace) {
                        Modifier.padding(top = 120.dp.h(scale))
                    } else {
                        Modifier.padding(bottom = 86.dp.h(scale))
                    }
                )
                .background(Color(0x1AFFFFFF), RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp.w(scale), vertical = 18.dp.h(scale)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(106.dp.h(scale)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.applogo),
                    contentDescription = "app logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp.h(scale)))

            Text(
                text = stringResource(page.title),
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
            Text(
                text = stringResource(page.accentTitle),
                color = Color(0xFFF5B94A),
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )

            Spacer(modifier = Modifier.height(16.dp.h(scale)))

            Text(
                text = stringResource(page.description),
                color = Color(0xFFE4D9DB),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ){
            // Indicator + button — pinned to the bottom, independent of the card's position
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp.w(scale))
                    .padding(bottom = if(!shouldReserveNativeSpace) 18.dp else 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageIndicator(
                    pageCount = pageCount,
                    selectedPage = selectedPage,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .height(46.dp.h(scale))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF86011D),
                                    Color(0xFF140105)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onNext)
                        .padding(horizontal = 26.dp.w(scale)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(if (pageIndex == pageCount - 1) R.string.get_started else R.string.next_btn),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.sp
                    )
                }
            }
            if (shouldReserveNativeSpace) {
                OnboardingNativeAd(
                    state = nativeAdState,
                    modifier = Modifier
                        .requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
                )
            }
        }

    }
}

