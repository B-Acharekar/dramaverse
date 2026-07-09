package app.dramaverse.stream.screen

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dramaverse.stream.R
import kotlinx.coroutines.delay

@Composable
fun CustomSplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        DramaVerseGlowBackground()
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
                .padding(horizontal = 24.dp, vertical = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name_display),
                color = Color(0xFFFFA8AE),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                color = Color(0xFFE8E2E7),
                fontSize = 17.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.splash_description),
                color = Color(0xFFB6959B),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(38.dp))
            LoadingDots()
            Spacer(modifier = Modifier.weight(1.35f))
        }
    }
}

@Composable
fun DramaVerseGlowBackground() {
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
