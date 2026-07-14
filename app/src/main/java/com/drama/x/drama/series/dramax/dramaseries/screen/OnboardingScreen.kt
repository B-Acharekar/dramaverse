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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.viewinterop.AndroidView
import com.ads.module.ads.wrapper.ApNativeAd
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView


private const val FULLSCREEN_NATIVE_PAGER_INDEX = 2

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
    var onboardingPageOneAdState by remember { mutableStateOf<NativeAdState>(NativeAdState.Loading) }
    var onboardingPageThreeAdState by remember { mutableStateOf<NativeAdState>(NativeAdState.Loading) }
    var onboardingFullscreenAdState by remember { mutableStateOf<NativeAdState>(NativeAdState.Loading) }
    var onboardingWelcomeAdState by remember { mutableStateOf<NativeAdState>(NativeAdState.Loading) }
    val showFullNativePage = false
    val pagerPageCount = uiState.pages.size + if (showFullNativePage) 1 else 0
    val pagerState = rememberPagerState(pageCount = { pagerPageCount })

    LaunchedEffect(Unit) {
        onEntered()
        activity?.let {
            AdsManager.loadNativeOnboardingPageOne(it, firstVisit = true)
            AdsManager.loadNativeOnboardingPageThree(it, firstVisit = true)
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
        val pageThreeObserver = Observer<NativeAdState> { state ->
            onboardingPageThreeAdState = state
            Log.d(ADS_TAG, "ONBOARDING_NATIVE_3 observer state=${state.javaClass.simpleName}")
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
        AdsManager.nativeOnboardingPageThreeAdLive.observeForever(pageThreeObserver)
        AdsManager.nativeOnboardingFullscreenAdLive.observeForever(fullscreenObserver)
        AdsManager.nativeOnboardingWelcomeAdLive.observeForever(welcomeObserver)
        onDispose {
            AdsManager.nativeOnboardingPageOneAdLive.removeObserver(pageOneObserver)
            AdsManager.nativeOnboardingPageThreeAdLive.removeObserver(pageThreeObserver)
            AdsManager.nativeOnboardingFullscreenAdLive.removeObserver(fullscreenObserver)
            AdsManager.nativeOnboardingWelcomeAdLive.removeObserver(welcomeObserver)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111113))
    ) {
        OnboardingGlowBackground()
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            if (showFullNativePage && pageIndex == FULLSCREEN_NATIVE_PAGER_INDEX) {
                OnboardingFullscreenNativeAd(
                    state = onboardingFullscreenAdState,
                    onContinue = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = pageIndex + 1,
                                animationSpec = tween(360, easing = FastOutSlowInEasing)
                            )
                        }
                    }
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
                        1 -> onboardingFullscreenAdState
                        2 -> onboardingPageThreeAdState
                        else -> NativeAdState.Disabled("not_this_page")
                    },
                    onNext = onNext
                )
            }
        }
    }
}

private fun Int.toOnboardingPageIndex(showFullNativePage: Boolean): Int =
    if (showFullNativePage && this > FULLSCREEN_NATIVE_PAGER_INDEX) this - 1 else this

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int,
    pageCount: Int,
    selectedPage: Int,
    nativeAdState: NativeAdState,
    onNext: () -> Unit
) {
    val pageHasNativePlacement = pageIndex in 0..2
    val shouldReserveNativeSpace =
        pageHasNativePlacement && (nativeAdState is NativeAdState.Loading || nativeAdState is NativeAdState.Loaded)
    val visualHeight = when {
        shouldReserveNativeSpace -> 205.dp
        pageHasNativePlacement -> 320.dp
        else -> 505.dp
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(if (shouldReserveNativeSpace) 18.dp else 34.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(visualHeight),
            contentAlignment = Alignment.Center
        ) {
            when (page.visual) {
                OnboardingVisual.DramaPhone -> DramaPhoneVisual(painterResource(R.drawable.onboarding1image))
                OnboardingVisual.Collections -> CollectionsVisual()
                OnboardingVisual.RomancePhone -> DramaPhoneVisual(painterResource(R.drawable.onboarding3image))
                OnboardingVisual.Welcome -> Unit
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OnboardingTitle(page)
        Spacer(modifier = Modifier.height(16.dp))
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
                .padding(horizontal = 10.dp)
        )
        Spacer(modifier = Modifier.weight(if (shouldReserveNativeSpace) 0.72f else 1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        if (pageHasNativePlacement && nativeAdState !is NativeAdState.Disabled) {
            Spacer(modifier = Modifier.height(12.dp))
            OnboardingNativeAd(
                state = nativeAdState,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
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
    ErainNativeAdHost(
        placementName = "onboarding_page_native",
        state = state,
        modifier = modifier,
        height = 320.dp,
        showFailureMessage = true
    )
}

//@Composable
//private fun OnboardingFullscreenNativeAd(
//    state: NativeAdState,
//    onContinue: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xF2111113))
//            .padding(horizontal = 18.dp, vertical = 26.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize(),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(modifier = Modifier.height(22.dp))
//            Text(
//                text = "Sponsored",
//                color = Color(0xFFB9B0B5),
//                fontSize = 13.sp,
//                fontWeight = FontWeight.SemiBold,
//                letterSpacing = 0.sp
//            )
//            Spacer(modifier = Modifier.height(14.dp))
//            ErainNativeAdHost(
//                placementName = "onboarding_fullscreen_native",
//                state = state,
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth(),
//                height = 560.dp,
//                showFailureMessage = true
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            OnboardingActionButton(
//                label = R.string.next_btn,
//                onClick = onContinue
//            )
//            Spacer(modifier = Modifier.height(18.dp))
//        }
//    }
//}


@Composable
private fun OnboardingFullscreenNativeAd(
    state: NativeAdState,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2111113))
            .padding(horizontal = 18.dp, vertical = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Sponsored",
                color = Color(0xFFB9B0B5),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

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
                    // Disabled/failed states shouldn't normally reach this composable
                    // since showFullNativePage gates on Loading/Loaded, but guard anyway.
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OnboardingActionButton(
                label = R.string.next_btn,
                onClick = onContinue
            )
            Spacer(modifier = Modifier.height(18.dp))
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
    Row(
        modifier = modifier.padding(start = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .height(5.dp)
                    .width(if (selectedPage == index) 27.dp else 5.dp)
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
    Box(
        modifier = Modifier
            .height(44.dp)
            .width(112.dp)
            .background( brush = Brush.horizontalGradient(
                listOf(Color(0xFF86011D),
                    Color(0xFF140105))
            ),
                shape = RoundedCornerShape(24.dp))
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
private fun DramaPhoneVisual(painter: Painter) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(26.dp))
        )
    }
}

@Composable
private fun CollectionsVisual() {
    Box(modifier = Modifier) {
        Image(
            painter = painterResource(R.drawable.onboarding2image),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(26.dp))
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
    val shouldReserveNativeSpace = nativeAdState is NativeAdState.Loading || nativeAdState is NativeAdState.Loaded
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.welocome_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth
        )

        // Frosted glass card — truly centered on screen
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 82.dp)
                .background(Color(0x1AFFFFFF), RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.applogo),
                    contentDescription = "app logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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

        // Indicator + button — pinned to the bottom, independent of the card's position
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = if (shouldReserveNativeSpace) 356.dp else 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageIndicator(
                pageCount = pageCount,
                selectedPage = selectedPage,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF86011D),
                                Color(0xFF140105))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onNext)
                    .padding(horizontal = 26.dp),
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
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 20.dp)
            )
        }
    }
}
