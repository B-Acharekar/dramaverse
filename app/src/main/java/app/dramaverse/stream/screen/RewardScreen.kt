package app.dramaverse.stream.screen

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.data.RewardAchievement
import app.dramaverse.stream.data.RewardFeed
import app.dramaverse.stream.data.RewardPackage
import app.dramaverse.stream.model.RewardViewModel

private val Background = Color(0xFF08080A)
private val Panel = Color(0xFF17171A)
private val Gold = Color(0xFFF6C54F)
private val Pink = Color(0xFFFF3E68)
private val SoftPink = Color(0xFFFFB5C1)

@Composable
fun RewardScreen(
    backendBaseUrl: String,
    onHome: () -> Unit,
    onShorts: () -> Unit,
    onLibrary: () -> Unit,
    viewModel: RewardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(backendBaseUrl) {
        viewModel.loadRewards(backendBaseUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        val feed = uiState.feed
        if (uiState.isLoading && feed == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SoftPink)
            }
        } else {
            RewardContent(
                feed = feed ?: fallbackRewardFeed(),
                selectedPlanIndex = uiState.selectedPlanIndex,
                onPlanSelected = viewModel::selectPlan,
                onClaimDaily = { viewModel.claimDailyCheckIn(backendBaseUrl) },
                onSpin = { viewModel.spin(backendBaseUrl) }
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
}

@Composable
private fun RewardContent(
    feed: RewardFeed,
    selectedPlanIndex: Int,
    onPlanSelected: (Int) -> Unit,
    onClaimDaily: () -> Unit,
    onSpin: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BalanceHero(feed.coins) }
        item { VipClub(feed.subscriptionPackages, selectedPlanIndex, onPlanSelected) }
        item { DailyCheckIn(feed.checkInDay, onClaimDaily) }
        item { LuckySpin(feed.spinAvailable, onSpin) }
        item { WatchEarn(feed.watchMinutesToday) }
        item { CoinPacks(feed.coinPackages) }
        item { Achievements(feed.achievements) }
    }
}

@Composable
private fun BalanceHero(coins: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1B171C), Color(0xFF221A25))))
            .border(1.dp, Color(0x33FFB5C1), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("YOUR BALANCE", color = SoftPink, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(coins.toString(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Spacer(Modifier.width(8.dp))
                CoinIcon(28)
            }
        }
        Text(
            "REDEEM",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFF4D74), Color(0xFFD70B43))))
                .padding(horizontal = 20.dp, vertical = 11.dp),
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun VipClub(plans: List<RewardPackage>, selectedPlanIndex: Int, onPlanSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF6D2B9B), Color(0xFFF6C54F)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Color(0xFF111114), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Join the VIP Club", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        Text("Unlimited watching, no ads, early access, and HD streaming.", color = Color(0xFFCDB1B9), fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, letterSpacing = 0.sp)
        Spacer(Modifier.height(16.dp))
        BenefitRow(Icons.Filled.PlayCircle, "Unlimited watching", "Binge every episode without hidden gates.")
        BenefitRow(Icons.Filled.CheckCircle, "No ads", "Pure cinematic storytelling with zero interruptions.")
        BenefitRow(Icons.Filled.Hd, "HD Streaming", "Sharp visuals tuned for mobile displays.")
        Spacer(Modifier.height(16.dp))
        plans.take(3).forEachIndexed { index, plan ->
            PlanRow(plan, selected = index == selectedPlanIndex, onClick = { onPlanSelected(index) })
            if (index != plans.take(3).lastIndex) Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(16.dp))
        ActionButton("Subscribe Now")
    }
}

@Composable
private fun BenefitRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Text(body, color = Color(0xFFCDB1B9), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun PlanRow(plan: RewardPackage, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0x26F6C54F) else Color(0xFF111114))
            .border(1.dp, if (selected) Gold else Color(0x25FFFFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(plan.title.uppercase(), color = SoftPink, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            Text(plan.price, color = if (selected) Gold else Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        }
        if (plan.bestValue) {
            Text("POPULAR", color = Color(0xFF261B06), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Gold).padding(horizontal = 10.dp, vertical = 5.dp), letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun DailyCheckIn(currentDay: Int, onClaimDaily: () -> Unit) {
    SectionTitle("Daily Check-in", "Day $currentDay of 7")
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.height(168.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items((1..7).toList()) { day ->
            val active = day == currentDay
            val complete = day < currentDay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) Color(0x332E2511) else if (complete) Color(0xFF3A1721) else Panel)
                    .border(1.dp, if (active) Gold else if (complete) Pink else Color(0x20FFFFFF), RoundedCornerShape(12.dp))
                    .clickable(enabled = active, onClick = onClaimDaily),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Day $day", color = if (active) Gold else Color(0xFFBDA3AB), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                Spacer(Modifier.height(6.dp))
                Icon(if (day == 7) Icons.Filled.EmojiEvents else Icons.Filled.Star, contentDescription = null, tint = if (complete) SoftPink else if (active) Gold else Color(0xFF6C6065), modifier = Modifier.size(22.dp))
                Text(if (day == 7) "Bonus" else "+${day * 5}", color = if (active) Gold else Color(0xFF9B858E), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
        }
    }
}

@Composable
private fun LuckySpin(spinAvailable: Int, onSpin: () -> Unit) {
    SectionTitle("Lucky Spin", "$spinAvailable Free Daily")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(180.dp).clip(CircleShape).background(Color(0xFF202024)).border(12.dp, Color(0xFF28282C), CircleShape), contentAlignment = Alignment.Center) {
            CoinIcon(42)
        }
        Spacer(Modifier.height(16.dp))
        ActionButton("SPIN NOW", onSpin)
    }
}

@Composable
private fun WatchEarn(minutes: Int) {
    SectionTitle("Watch & Earn", null)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = SoftPink, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Daily Watch Time", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Text("Watch 30 mins to earn 50 Coins", color = Color(0xFFB69FA7), fontSize = 12.sp, letterSpacing = 0.sp)
            }
            Text("${minutes.coerceAtMost(30)}/30m", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF29272B))) {
            Box(Modifier.fillMaxWidth((minutes / 30f).coerceIn(0f, 1f)).height(8.dp).background(SoftPink, RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
private fun CoinPacks(packages: List<RewardPackage>) {
    SectionTitle("Refill Your Coins", "Instant unlocks")
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(430.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(packages.take(4)) { pack ->
            CoinPackCard(pack)
        }
    }
    packages.firstOrNull { it.bestValue }?.let { pack ->
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).border(1.dp, Color(0x66FFB5C1), RoundedCornerShape(18.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CoinIcon(58)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(pack.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Text("Best value", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
            Text(pack.price, color = Color(0xFF5A1626), fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(SoftPink).padding(horizontal = 18.dp, vertical = 16.dp), letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun CoinPackCard(pack: RewardPackage) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CoinIcon(48)
        Spacer(Modifier.height(16.dp))
        Text((pack.amount.takeIf { it > 0 } ?: pack.title.filter(Char::isDigit).toIntOrNull() ?: 50).toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        Text("Coins", color = SoftPink, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        Spacer(Modifier.height(14.dp))
        Text(pack.price, color = SoftPink, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF222126)).padding(vertical = 11.dp), textAlign = TextAlign.Center, letterSpacing = 0.sp)
    }
}

@Composable
private fun Achievements(achievements: List<RewardAchievement>) {
    SectionTitle("Achievements", "View All")
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(342.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(achievements) { achievement ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Panel)
                    .border(1.dp, if (achievement.unlocked) Color(0x66F6C54F) else Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = if (achievement.unlocked) Gold else Color(0xFF5A555B), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(achievement.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
                Text(achievement.subtitle, color = Color(0xFF9F858E), fontSize = 11.sp, textAlign = TextAlign.Center, letterSpacing = 0.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = SoftPink, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit = {}) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFF4D74), Color(0xFFD70B43))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
    }
}

@Composable
private fun CoinIcon(size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF1A1310), modifier = Modifier.size((size * 0.62f).dp))
    }
}

private fun fallbackRewardFeed(): RewardFeed = RewardFeed(
    coins = 1240,
    vip = false,
    checkInDay = 4,
    spinAvailable = 1,
    watchMinutesToday = 22,
    coinPackages = listOf(
        RewardPackage("50 Coins", 50, "$0.99"),
        RewardPackage("100 Coins", 100, "$1.99"),
        RewardPackage("250 Coins", 250, "$4.99"),
        RewardPackage("500 Coins", 500, "$8.99"),
        RewardPackage("1000 Coins Pack", 1000, "$14.99", true)
    ),
    subscriptionPackages = listOf(
        RewardPackage("Monthly", 0, "$9.99"),
        RewardPackage("Quarterly", 0, "$24.99", true),
        RewardPackage("Yearly", 0, "$79.99")
    ),
    achievements = listOf(
        RewardAchievement("Drama King", "Watch 100 episodes", true),
        RewardAchievement("Night Owl", "Watch after 12 AM", false),
        RewardAchievement("Top Fan", "Favorite 5 series", true),
        RewardAchievement("Socialite", "Invite 3 friends", false)
    )
)
