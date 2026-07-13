package app.dramaverse.stream.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.model.OnboardingPage
import app.dramaverse.stream.model.OnboardingViewModel
import app.dramaverse.stream.model.OnboardingVisual
import kotlinx.coroutines.launch
import app.dramaverse.stream.R


@Composable
fun OnboardingScreen(
    onEntered: () -> Unit,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { uiState.pages.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        onEntered()
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectPage(pagerState.currentPage)
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
            val page = uiState.pages[pageIndex]
            val onNext: () -> Unit = {
                if (pagerState.currentPage == uiState.pages.lastIndex) {
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
                    pageIndex = pageIndex,
                    pageCount = uiState.pages.size,
                    selectedPage = pagerState.currentPage,
                    onNext = onNext
                )
            } else {
                OnboardingPageContent(
                    page = page,
                    pageIndex = pageIndex,
                    pageCount = uiState.pages.size,
                    selectedPage = pagerState.currentPage,
                    onNext = onNext
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int,
    pageCount: Int,
    selectedPage: Int,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.34f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(505.dp),
            contentAlignment = Alignment.Center
        ) {
            when (page.visual) {
                OnboardingVisual.DramaPhone -> DramaPhoneVisual(painterResource(R.drawable.onboarding1image))
                OnboardingVisual.Collections -> CollectionsVisual()
                OnboardingVisual.RomancePhone -> DramaPhoneVisual(painterResource(R.drawable.onboarding3image))
                OnboardingVisual.Welcome -> Unit // unreachable — Welcome is routed to WelcomePageContent in the pager
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
        Spacer(modifier = Modifier.height(32.dp))
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
        Spacer(modifier = Modifier.weight(0.66f))
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
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xEEFF506A), Color(0x99FF334D), Color.Transparent)
            ),
            radius = size.width * 0.68f,
            center = Offset(size.width * 1.08f, size.height * 0.07f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xCC7A15FF), Color(0xAA5B18F0), Color.Transparent)
            ),
            radius = size.width * 0.64f,
            center = Offset(size.width * -0.10f, size.height * 0.83f)
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0x00111113), Color(0xCC111113), Color(0xFF111113)),
                startY = size.height * 0.48f,
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

@Composable
private fun WelcomePageContent(
    page: OnboardingPage,
    pageIndex: Int,
    pageCount: Int,
    selectedPage: Int,
    onNext: () -> Unit
) {
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
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
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
                .padding(bottom = 24.dp),
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
    }
}