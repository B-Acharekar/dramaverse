package app.dramaverse.stream.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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
            OnboardingPageContent(
                page = uiState.pages[pageIndex],
                pageIndex = pageIndex,
                pageCount = uiState.pages.size,
                selectedPage = pagerState.currentPage,
                onNext = {
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
            )
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
                OnboardingVisual.DramaPhone -> DramaPhoneVisual(romance = false)
                OnboardingVisual.Collections -> CollectionsVisual()
                OnboardingVisual.RomancePhone -> DramaPhoneVisual(romance = true)
                OnboardingVisual.Rewards -> RewardsVisual()
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OnboardingTitle(page)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
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
                label = if (pageIndex == pageCount - 1) "Start" else "Next",
                onClick = onNext
            )
        }
        Spacer(modifier = Modifier.weight(0.66f))
    }
}

@Composable
private fun OnboardingTitle(page: OnboardingPage) {
    if (page.accentTitle.isBlank()) {
        Text(
            text = page.title,
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
                text = page.title,
                color = Color.White,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
            Text(
                text = page.accentTitle,
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
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .width(112.dp)
            .border(2.dp, Color(0xFFFF365E), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
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
private fun DramaPhoneVisual(romance: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.66f)
            .aspectRatio(0.52f)
            .background(Color(0xFF222225), RoundedCornerShape(32.dp))
            .padding(7.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(26.dp))
        ) {
            drawPhoneScene(romance)
        }
        PhoneSideActions(modifier = Modifier.align(Alignment.CenterEnd))
        PhoneBottomMeta(modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun CollectionsVisual() {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.67f)
            .aspectRatio(0.52f)
            .background(Color(0xFF343438), RoundedCornerShape(31.dp))
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(25.dp))
        ) {
            drawPosterGrid()
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x10111113), Color(0xDD111113)),
                    startY = size.height * 0.56f,
                    endY = size.height
                )
            )
        }
    }
}

@Composable
private fun RewardsVisual() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(505.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(235.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xAAFFBF3F), Color(0x332E2518), Color.Transparent)
                ),
                radius = size.minDimension * 0.52f,
                center = center
            )
            drawCircle(
                color = Color(0x33FFF0C1),
                radius = size.minDimension * 0.25f,
                center = center
            )
            drawCircle(
                color = Color(0xFFFFC552),
                radius = size.minDimension * 0.13f,
                center = center
            )
            drawStar(center, size.minDimension * 0.08f, Color(0xFF201915))
            drawRewardChip(Offset(size.width * 0.69f, size.height * 0.23f), rotationHint = true)
            drawRewardChip(Offset(size.width * 0.16f, size.height * 0.56f), rotationHint = false)
        }
    }
}

@Composable
private fun PhoneSideActions(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(end = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Text("<3", color = Color.White, fontSize = 13.sp, lineHeight = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        Text("[]", color = Color.White, fontSize = 12.sp, lineHeight = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        Text("=", color = Color.White, fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
    }
}

@Composable
private fun PhoneBottomMeta(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(start = 9.dp, bottom = 13.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MiniPill("TRENDING", Color(0xFFFF4D6D), Color(0xFF2D151A))
            MiniPill("EPISODE 1", Color.White, Color(0x552B2B30))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(4.dp)
                .width(72.dp)
                .background(Color(0x44FFFFFF), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.64f)
                    .background(Color(0xFFFF6076), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun MiniPill(text: String, color: Color, background: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 5.sp,
        lineHeight = 7.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

private fun DrawScope.drawPhoneScene(romance: Boolean) {
    val base = if (romance) Color(0xFF342336) else Color(0xFF16283D)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF070A12), base, Color(0xFF0B0C11))
        )
    )
    repeat(18) { index ->
        val x = size.width * ((index * 37 % 100) / 100f)
        val top = size.height * ((index * 13 % 45) / 100f)
        val buildingWidth = size.width * (0.06f + (index % 4) * 0.012f)
        val buildingHeight = size.height * (0.22f + (index % 5) * 0.035f)
        drawRoundRect(
            color = Color(0xAA121722),
            topLeft = Offset(x, top),
            size = Size(buildingWidth, buildingHeight),
            cornerRadius = CornerRadius(2f, 2f)
        )
        repeat(4) { light ->
            drawCircle(
                color = if (romance) Color(0xFFFF7DAC) else Color(0xFF60B9FF),
                radius = 1.8f,
                center = Offset(x + buildingWidth * 0.45f, top + 10f + light * 17f)
            )
        }
    }
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(Color(0x99FF4067), Color(0x55683DFF), Color.Transparent)
        ),
        topLeft = Offset(size.width * 0.12f, size.height * 0.44f),
        size = Size(size.width * 0.76f, size.height * 0.22f),
        cornerRadius = CornerRadius(size.width * 0.18f, size.width * 0.18f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xAAFF5B7C), Color(0x22FF5B7C), Color.Transparent)
        ),
        radius = size.width * 0.34f,
        center = Offset(size.width * 0.50f, size.height * 0.48f)
    )
    drawLeadFigure(
        x = size.width * 0.44f,
        ground = size.height * 0.70f,
        body = Color(0xFF141721),
        skin = Color(0xFFE8A681),
        facingRight = true
    )
    drawLeadFigure(
        x = size.width * 0.58f,
        ground = size.height * 0.71f,
        body = if (romance) Color(0xFF4C3030) else Color(0xFF22232C),
        skin = Color(0xFFF2B088),
        facingRight = false
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color(0x00111113), Color(0x440B0C11), Color(0xAA07080B)),
            startY = size.height * 0.48f,
            endY = size.height * 0.82f
        ),
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(0f, 0f)
    )
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, Color(0xAA09090B)),
            startY = size.height * 0.62f,
            endY = size.height
        )
    )
}

private fun DrawScope.drawLeadFigure(
    x: Float,
    ground: Float,
    body: Color,
    skin: Color,
    facingRight: Boolean
) {
    val headCenter = Offset(x, ground - size.height * 0.26f)
    drawCircle(
        color = Color(0xFF1B1417),
        radius = size.width * 0.052f,
        center = Offset(headCenter.x + if (facingRight) -5f else 5f, headCenter.y - 7f)
    )
    drawOvalLikeHead(headCenter, skin)
    drawRoundRect(
        color = body,
        topLeft = Offset(x - size.width * 0.050f, ground - size.height * 0.205f),
        size = Size(size.width * 0.10f, size.height * 0.23f),
        cornerRadius = CornerRadius(16f, 16f)
    )
    drawRoundRect(
        color = body.copy(alpha = 0.85f),
        topLeft = Offset(x + if (facingRight) size.width * 0.018f else -size.width * 0.050f, ground - size.height * 0.165f),
        size = Size(size.width * 0.035f, size.height * 0.18f),
        cornerRadius = CornerRadius(10f, 10f)
    )
}

private fun DrawScope.drawOvalLikeHead(center: Offset, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - size.width * 0.035f, center.y - size.width * 0.045f),
        size = Size(size.width * 0.070f, size.width * 0.086f),
        cornerRadius = CornerRadius(size.width * 0.035f, size.width * 0.043f)
    )
}

private fun DrawScope.drawPosterGrid() {
    val titles = listOf("HROMATIC", "ATLANTIC\nPROMISE", "ASPHALT\nSHADOW", "PRECISION", "ROOTS & RUIN", "BINARY\nGHOST", "BLOODED\nSTEEL", "STATE\nSECRETS", "WHISPER")
    val palette = listOf(
        Color(0xFF16354B), Color(0xFFB58445), Color(0xFF2D4242),
        Color(0xFF1E6574), Color(0xFF78613C), Color(0xFF223A4A),
        Color(0xFF2A2725), Color(0xFF473530), Color(0xFF172727)
    )
    val columns = 3
    val rows = 3
    val cellWidth = size.width / columns
    val cellHeight = size.height / rows
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val index = row * columns + column
            val left = column * cellWidth
            val top = row * cellHeight
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(palette[index].copy(alpha = 0.95f), Color(0xFF08090B)),
                    startY = top,
                    endY = top + cellHeight
                ),
                topLeft = Offset(left, top),
                size = Size(cellWidth - 1f, cellHeight - 1f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = cellWidth * 0.22f,
                center = Offset(left + cellWidth * 0.53f, top + cellHeight * 0.34f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                titles[index].replace('\n', ' '),
                left + cellWidth * 0.08f,
                top + cellHeight * 0.74f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 16f
                    isFakeBoldText = true
                    alpha = 210
                }
            )
        }
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    repeat(10) { index ->
        val angle = Math.toRadians((index * 36.0) - 90.0)
        val r = if (index % 2 == 0) radius else radius * 0.45f
        val point = Offset(
            x = center.x + (kotlin.math.cos(angle) * r).toFloat(),
            y = center.y + (kotlin.math.sin(angle) * r).toFloat()
        )
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawRewardChip(anchor: Offset, rotationHint: Boolean) {
    drawRoundRect(
        color = Color(0x223E3321),
        topLeft = Offset(anchor.x - 22f, anchor.y - 15f),
        size = Size(44f, 30f),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(1.3f)
    )
    drawCircle(
        color = Color(0xFFFFC552),
        radius = 6f,
        center = Offset(anchor.x + if (rotationHint) 2f else -3f, anchor.y)
    )
    drawCircle(
        color = Color(0xFF17120E),
        radius = 3f,
        center = Offset(anchor.x + if (rotationHint) 2f else -3f, anchor.y)
    )
}
