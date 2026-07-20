package com.drama.x.drama.series.dramax.dramaseries.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drama.x.drama.series.dramax.dramaseries.R

@Composable
fun NoInternetScreen(
    onRetry: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "offline")
    val breathe by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offlineBreathe"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1700)),
        label = "offlinePulse"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1800)),
        label = "offlineShimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF061E78),
                        Color(0xFF09090B),
                        Color(0xFF171018),
                        Color(0xFF09090B)
                    )
                )
            )
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            OfflineGlyph(
                breathe = breathe,
                pulse = pulse,
                modifier = Modifier
                    .offset(y = ((breathe - 1f) * 10f).dp)
                    .size(152.dp)
            )
            Spacer(Modifier.height(30.dp))
            Text(
                text = stringResource(R.string.no_internet_title),
                color = Color.White,
                fontSize = 27.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.no_internet_description),
                color = Color(0xFFE4D1D8),
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF3E68),
                                Color(0xFFFF6F92),
                                Color(0xFFE9164D)
                            ),
                            startX = shimmer * 800f - 260f,
                            endX = shimmer * 800f
                        )
                    )
                    .border(1.dp, Color(0xFFFFC0C9).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onRetry),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.retry_now),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.retrying_connection),
                color = Color(0xBDE8D7DC),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun OfflineGlyph(
    breathe: Float,
    pulse: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val pink = Color(0xFFFF3E68)
        val softPink = Color(0xFFFFC0C9)
        val gold = Color(0xFFF5C65B)
        val panel = Color(0xFF151318)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val min = size.minDimension

        drawCircle(
            color = pink.copy(alpha = 0.10f + 0.10f * pulse),
            radius = min * (0.43f + pulse * 0.1f),
            center = center
        )
        drawCircle(
            color = gold.copy(alpha = 0.08f * (1f - pulse)),
            radius = min * (0.28f + pulse * 0.12f),
            center = center
        )

        drawRoundRect(
            color = panel,
            topLeft = Offset(size.width * 0.22f, size.height * 0.42f),
            size = Size(size.width * 0.56f, size.height * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(min * 0.08f, min * 0.08f)
        )
        drawRoundRect(
            color = Color(0xFF2A1720),
            topLeft = Offset(size.width * 0.27f, size.height * 0.49f),
            size = Size(size.width * 0.46f, size.height * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(min * 0.04f, min * 0.04f)
        )
        repeat(4) { index ->
            val x = size.width * (0.32f + index * 0.12f)
            drawCircle(softPink.copy(alpha = 0.8f), min * 0.018f, Offset(x, size.height * 0.72f))
        }

        val stroke = Stroke(width = min * 0.045f, cap = StrokeCap.Round)
        drawArc(
            color = gold.copy(alpha = 0.28f + pulse * 0.45f),
            startAngle = 218f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = Offset(size.width * 0.24f, size.height * 0.08f),
            size = Size(size.width * 0.52f * breathe, size.height * 0.52f * breathe),
            style = stroke
        )
        drawArc(
            color = softPink.copy(alpha = 0.5f + pulse * 0.35f),
            startAngle = 226f,
            sweepAngle = 88f,
            useCenter = false,
            topLeft = Offset(size.width * 0.34f, size.height * 0.2f),
            size = Size(size.width * 0.32f * breathe, size.height * 0.32f * breathe),
            style = stroke
        )
        drawCircle(pink, min * 0.036f, Offset(cx, size.height * 0.48f))

        rotate(degrees = -18f + pulse * 36f, pivot = Offset(cx, cy)) {
            drawLine(
                color = pink,
                start = Offset(cx - min * 0.26f, cy - min * 0.3f),
                end = Offset(cx + min * 0.26f, cy + min * 0.18f),
                strokeWidth = min * 0.035f,
                cap = StrokeCap.Round
            )
        }
        drawCircle(gold.copy(alpha = 0.9f), min * 0.018f, Offset(size.width * 0.2f, size.height * (0.32f + pulse * 0.05f)))
        drawCircle(softPink.copy(alpha = 0.85f), min * 0.015f, Offset(size.width * 0.82f, size.height * (0.34f - pulse * 0.05f)))
        drawOval(
            color = softPink.copy(alpha = 0.45f),
            topLeft = Offset(size.width * 0.34f, size.height * 0.76f),
            size = Size(size.width * 0.32f, size.height * 0.065f)
        )
    }
}
