package app.dramaverse.stream.screen

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.data.CheckInReward
import app.dramaverse.stream.data.DailyRewardTask
import app.dramaverse.stream.data.RewardFeed
import app.dramaverse.stream.data.SpinReward
import app.dramaverse.stream.data.fallbackRewardFeed
import app.dramaverse.stream.model.RewardViewModel
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

private val Background = Color(0xFF08080A)
private val Panel = Color(0xFF16151A)
private val StrokeColor = Color(0x29FFFFFF)
private val Gold = Color(0xFFF7C64B)
private val Pink = Color(0xFFFF3F68)
private val SoftPink = Color(0xFFFFB7C2)
private val Track = Color(0xFF5E5664)
private val TextMuted = Color(0xFFC7B6BE)
private val TextDim = Color(0xFF8F828B)
private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun RewardScreen(
    backendBaseUrl: String,
    onHome: () -> Unit,
    onShorts: () -> Unit,
    onLibrary: () -> Unit,
    viewModel: RewardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSpinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(backendBaseUrl) {
        viewModel.loadRewards(backendBaseUrl)
    }

    Box(Modifier.fillMaxSize().background(Background)) {
        RewardBackdrop()
        val feed = uiState.feed
        if (uiState.isLoading && feed == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SoftPink)
            }
        } else {
            RewardContent(
                feed = feed ?: fallbackRewardFeed(),
                onRules = { viewModel.setRulesVisible(true) },
                onCheckIn = { viewModel.claimDailyCheckIn(backendBaseUrl) },
                onTaskClaim = { taskId -> viewModel.claimDailyTask(backendBaseUrl, taskId) },
                onTaskStart = onHome,
                onSpinOpen = { showSpinDialog = true }
            )
        }
        BottomNavigationBar(
            selected = "Rewards",
            onHome = onHome,
            onShorts = onShorts,
            onLibrary = onLibrary,
            onRewards = {},
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (uiState.showRules) {
        RulesDialog(
            rules = uiState.feed?.rules ?: fallbackRewardFeed().rules,
            onDismiss = { viewModel.setRulesVisible(false) }
        )
    }
    if (showSpinDialog) {
        val feed = uiState.feed ?: fallbackRewardFeed()
        SpinWheelDialog(
            spin = feed.spin,
            pointerIndex = uiState.spinPointerIndex,
            spinTurns = uiState.spinTurns,
            onDismiss = { showSpinDialog = false },
            onSpin = { viewModel.spin(backendBaseUrl) }
        )
    }
}

@Composable
private fun RewardContent(
    feed: RewardFeed,
    onRules: () -> Unit,
    onCheckIn: () -> Unit,
    onTaskClaim: (String) -> Unit,
    onTaskStart: () -> Unit,
    onSpinOpen: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 32.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { RewardHeader(onRules) }
        item { BalanceCard(feed.coins) }
        item { DailyCheckInSection(feed.checkInRewards, feed.canCheckIn, onCheckIn) }
        item { DailyTaskSection(feed.dailyTasks, onTaskClaim, onTaskStart) }
        item { WeeklySpinSection(feed.spin, onSpinOpen) }
    }
}

@Composable
private fun RewardBackdrop() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF19203A), Color(0xFF251327), Color(0x9908080A), Color.Transparent)
                )
            )
    )
}

@Composable
private fun RewardHeader(onRules: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f).padding(end = 14.dp)) {
            Text("Rewards", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Spacer(Modifier.height(5.dp))
            Text(
                "Earn coins from check-ins, watch time, and the weekly spin.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x99141318))
                .border(1.dp, StrokeColor, CircleShape)
                .clickable(onClick = onRules),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun BalanceCard(coins: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Brush.linearGradient(listOf(Color(0xFF241D28), Color(0xFF151319))))
            .border(1.dp, Color(0x45F7C64B), CardShape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("CURRENT BALANCE", color = SoftPink, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(coins.toString(), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Spacer(Modifier.width(9.dp))
                CoinIcon(34)
                Spacer(Modifier.width(7.dp))
                Text("Coins", color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            }
        }
    }
}

@Composable
private fun DailyCheckInSection(
    rewards: List<CheckInReward>,
    canCheckIn: Boolean,
    onCheckIn: () -> Unit
) {
    val activeDay = rewards.firstOrNull { it.current }?.day
    SectionHeader("Daily Check-in", if (canCheckIn && activeDay != null) "Claim day $activeDay" else "Claimed today")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rewards.chunked(4).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    CheckInTile(
                        item = item,
                        canClaim = canCheckIn && item.current,
                        onCheckIn = onCheckIn,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CheckInTile(
    item: CheckInReward,
    canClaim: Boolean,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = when {
        item.status == "claimed" || item.claimed -> CheckInState.Claimed
        item.status == "today" || canClaim -> CheckInState.Today
        else -> CheckInState.Upcoming
    }
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(CardShape)
            .background(
                when (state) {
                    CheckInState.Today -> Color(0xFF2A2114)
                    CheckInState.Claimed -> Color(0xFF231821)
                    CheckInState.Upcoming -> Panel
                }
            )
            .border(
                1.dp,
                when (state) {
                    CheckInState.Today -> Gold
                    CheckInState.Claimed -> SoftPink
                    CheckInState.Upcoming -> StrokeColor
                },
                CardShape
            )
            .clickable(enabled = canClaim, onClick = onCheckIn),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Day ${item.day}", color = if (state == CheckInState.Upcoming) TextMuted else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        Spacer(Modifier.height(6.dp))
        if (state == CheckInState.Claimed) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = SoftPink, modifier = Modifier.size(22.dp))
        } else {
            CoinIcon(22)
        }
        Spacer(Modifier.height(5.dp))
        Text("+${item.reward}", color = if (state == CheckInState.Upcoming) TextDim else Gold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            when (state) {
                CheckInState.Today -> "Today"
                CheckInState.Claimed -> "Claimed"
                CheckInState.Upcoming -> if (item.day == 7) "Bonus" else "Locked"
            },
            color = if (state == CheckInState.Upcoming) TextDim else TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun DailyTaskSection(
    tasks: List<DailyRewardTask>,
    onTaskClaim: (String) -> Unit,
    onTaskStart: () -> Unit
) {
    SectionHeader("Daily Task", "Rotates today")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tasks.forEach { task ->
            DailyTaskRow(task, onTaskClaim, onTaskStart)
        }
    }
}

@Composable
private fun DailyTaskRow(
    task: DailyRewardTask,
    onTaskClaim: (String) -> Unit,
    onTaskStart: () -> Unit
) {
    val progress = if (task.targetMinutes > 0) {
        (task.progressMinutes.toFloat() / task.targetMinutes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Panel)
            .border(1.dp, if (task.completed && !task.claimed) Color(0x66F7C64B) else StrokeColor, CardShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x22F7C64B)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(task.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp, modifier = Modifier.weight(1f))
                Text("+${task.reward}", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Track)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(10.dp)
                        .background(Brush.horizontalGradient(listOf(Gold, Pink)), RoundedCornerShape(8.dp))
                )
            }
            Spacer(Modifier.height(5.dp))
            Text("${task.progressMinutes}/${task.targetMinutes} min", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
        }
        Spacer(Modifier.width(12.dp))
        TaskClaimButton(task, onTaskClaim, onTaskStart)
    }
}

@Composable
private fun TaskClaimButton(
    task: DailyRewardTask,
    onTaskClaim: (String) -> Unit,
    onTaskStart: () -> Unit
) {
    val claimable = task.completed && !task.claimed
    val clickable = !task.claimed
    Box(
        modifier = Modifier
            .width(78.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (claimable) Pink else if (task.claimed) Color(0xFF26232A) else Color(0x33F7C64B))
            .border(1.dp, if (!task.claimed && !claimable) Color(0x66F7C64B) else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(enabled = clickable) { if (claimable) onTaskClaim(task.id) else onTaskStart() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (task.claimed) "Done" else if (claimable) "Claim" else "Go",
            color = if (task.claimed) TextDim else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun WeeklySpinSection(spin: SpinReward, onSpinOpen: () -> Unit) {
    SectionHeader("Weekly Spin", if (spin.available) "Ready" else weeklySpinCountdown())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF241229), Color(0xFF19151F), Color(0xFF101014))
                )
            )
            .border(1.dp, if (spin.available) Color(0x77F7C64B) else StrokeColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onSpinOpen)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFFFD36D), Color(0xFFFF456D), Color(0xFF6338D8))))
                .border(1.dp, Color(0x55FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Spin the Wheel", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                if (spin.available) "Open the wheel and win coins for this week." else "You used this week's spin. Next chance ${weeklySpinCountdown().lowercase()}.",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            if (spin.lastReward != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (spin.lastReward == 0) "Last spin: Better luck next time" else "Last spin: +${spin.lastReward} coins",
                    color = Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (spin.available) "OPEN" else "VIEW",
            color = if (spin.available) Color(0xFF17100D) else TextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (spin.available) Gold else Color(0xFF27242B))
                .padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SpinWheelDialog(
    spin: SpinReward,
    pointerIndex: Int,
    spinTurns: Int,
    onDismiss: () -> Unit,
    onSpin: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFFDFBFF))
                .padding(top = 18.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(40.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Spin the Wheel", color = Color(0xFF3A148C), fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                    Text("and win coins", color = Color(0xFF766B7C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x11000000))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFF4C4152), modifier = Modifier.size(21.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            SpinWheel(spin.segments, pointerIndex, spinTurns, spin.lastReward, large = true)
            Spacer(Modifier.height(18.dp))
            Text(
                spinResultText(spin),
                color = if (spin.lastReward == 0) Color(0xFF6E6074) else Color(0xFF1B1720),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                if (spin.available) "One spin available this week" else weeklySpinCountdown(),
                color = Color(0xFF8A7B91),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (spin.available) Brush.horizontalGradient(listOf(Color(0xFFFF4D73), Color(0xFFD70842))) else Brush.horizontalGradient(listOf(Color(0xFFE9E5ED), Color(0xFFE9E5ED))))
                    .clickable(enabled = spin.available, onClick = onSpin),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (spin.available) "SPIN NOW" else "USED THIS WEEK",
                    color = if (spin.available) Color.White else Color(0xFF86798D),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun SpinWheel(segments: List<Int>, pointerIndex: Int, spinTurns: Int, lastReward: Int?, large: Boolean = false) {
    val colors = listOf(Color(0xFFFFC94D), Color(0xFF6B36E8), Color(0xFF58C2FF), Color(0xFF8A4DFF), Color(0xFFFF5F85))
    val safeSegments = segments.ifEmpty { listOf(0, 10, 15, 20, 30, 40, 60, 100) }
    val sweep = 360f / safeSegments.size.coerceAtLeast(1)
    val selected = pointerIndex.coerceIn(0, (safeSegments.size - 1).coerceAtLeast(0))
    val rotation by animateFloatAsState(
        targetValue = spinTurns * 720f - selected * sweep,
        animationSpec = tween(durationMillis = 1100),
        label = "spinWheelRotation"
    )
    Box(Modifier.size(if (large) 292.dp else 230.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = if (large) 8.dp.toPx() else 16.dp.toPx()
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = if (large) 22.sp.toPx() else 12.sp.toPx()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            safeSegments.forEachIndexed { index, reward ->
                val start = -90f + rotation + index * sweep
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2)
                )
                val angle = Math.toRadians((start + sweep / 2f).toDouble())
                val radius = size.minDimension * 0.34f
                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.rotate(
                    (start + sweep / 2f + 90f),
                    center.x + cos(angle).toFloat() * radius,
                    center.y + sin(angle).toFloat() * radius
                )
                drawContext.canvas.nativeCanvas.drawText(
                    if (reward == 0) "MISS" else reward.toString(),
                    center.x + cos(angle).toFloat() * radius,
                    center.y + sin(angle).toFloat() * radius + 4.dp.toPx(),
                    labelPaint
                )
                drawContext.canvas.nativeCanvas.restore()
            }
            drawCircle(color = Color(0xFF5E31D6), radius = if (large) 38.dp.toPx() else 44.dp.toPx(), center = center)
            drawCircle(color = Color(0x55FFFFFF), radius = size.minDimension / 2f - inset, style = Stroke(width = if (large) 3.dp.toPx() else 5.dp.toPx()))
            drawPath(
                path = Path().apply {
                    moveTo(center.x, 4.dp.toPx())
                    lineTo(center.x - 18.dp.toPx(), 44.dp.toPx())
                    lineTo(center.x + 18.dp.toPx(), 44.dp.toPx())
                    close()
                },
                color = Pink
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("WEEKLY", color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            val centerReward = lastReward ?: safeSegments.getOrElse(selected) { 0 }
            Text(
                if (centerReward == 0) "MISS" else "+$centerReward",
                color = Color.White,
                fontSize = if (centerReward == 0) 14.sp else 24.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun RulesDialog(rules: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reward Rules", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), letterSpacing = 0.sp)
                Icon(Icons.Filled.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(22.dp).clickable(onClick = onDismiss))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rules.forEach { rule ->
                    Row(verticalAlignment = Alignment.Top) {
                        CoinIcon(14)
                        Spacer(Modifier.width(9.dp))
                        Text(rule, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun weeklySpinCountdown(): String {
    val value by produceState(initialValue = nextWeeklySpinText()) {
        while (true) {
            this.value = nextWeeklySpinText()
            delay(60_000)
        }
    }
    return value
}

private fun nextWeeklySpinText(): String {
    val now = ZonedDateTime.now()
    var reset = now.with(DayOfWeek.MONDAY).toLocalDate().plusWeeks(1).atStartOfDay(now.zone)
    if (!reset.isAfter(now)) {
        reset = reset.plusWeeks(1)
    }
    val duration = Duration.between(now, reset)
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
    return when {
        days > 0 -> "Resets in ${days}d ${hours}h"
        hours > 0 -> "Resets in ${hours}h ${minutes}m"
        else -> "Resets in ${minutes.coerceAtLeast(1)}m"
    }
}

private fun spinResultText(spin: SpinReward): String {
    val reward = spin.lastReward
    return when {
        spin.available -> "Rewards include coins and a better-luck-next-time slot."
        reward == null -> "Come back when your weekly spin is ready."
        reward == 0 -> "Better luck next time"
        else -> "You won +$reward coins"
    }
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp, modifier = Modifier.weight(1f))
        Text(
            action,
            color = SoftPink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x2216151A))
                .border(1.dp, Color(0x33FFB7C2), RoundedCornerShape(12.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

private enum class CheckInState {
    Today,
    Claimed,
    Upcoming
}

@Composable
private fun CoinIcon(size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF17100D), modifier = Modifier.size((size * 0.62f).dp))
    }
}
