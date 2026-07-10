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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.data.RewardAchievement
import app.dramaverse.stream.data.RewardFeed
import app.dramaverse.stream.data.RewardPackage
import app.dramaverse.stream.data.fallbackRewardFeed
import app.dramaverse.stream.model.RewardViewModel

private val Background = Color(0xFF07070A)
private val Ink = Color(0xFF0D0D12)
private val Panel = Color(0xFF17151B)
private val Stroke = Color(0x26FFFFFF)
private val Gold = Color(0xFFF8C84F)
private val Pink = Color(0xFFFF3F6E)
private val SoftPink = Color(0xFFFFB9C5)
private val Muted = Color(0xFFC4B4BD)
private val Dim = Color(0xFF8F8089)

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
        RewardBackdrop()
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
                onGetAccess = { viewModel.trackSubscriptionIntent(backendBaseUrl) },
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
    onGetAccess: () -> Unit,
    onClaimDaily: () -> Unit,
    onSpin: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { RewardsHeader(feed.coins) }
        item { PremiumAccessPanel(feed.subscriptionPackages, selectedPlanIndex, onPlanSelected, onGetAccess) }
        item { CoinWallet(feed.coins, feed.coinPackages) }
        item { DailyRewards(feed.checkInDay, onClaimDaily) }
        item { MissionDeck(feed.watchMinutesToday, feed.spinAvailable, onSpin) }
        item { AchievementStrip(feed.achievements) }
    }
}

@Composable
private fun RewardBackdrop() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(420.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A2446),
                        Color(0xFF2A1635),
                        Color(0x77100B18),
                        Color.Transparent
                    )
                )
            )
    ) {
        StarDot(34.dp, 28.dp, 2.dp, Color(0xAAFFFFFF))
        StarDot(102.dp, 74.dp, 1.5.dp, Color(0x80FFFFFF))
        StarDot(238.dp, 38.dp, 2.dp, Color(0x90F8C84F))
        StarDot(318.dp, 96.dp, 1.5.dp, Color(0x8AFFFFFF))
        StarDot(64.dp, 178.dp, 1.5.dp, Color(0x8AFFFFFF))
        StarDot(280.dp, 210.dp, 2.dp, Color(0x80FFB9C5))
        StarDot(170.dp, 264.dp, 1.5.dp, Color(0x80FFFFFF))
    }
}

@Composable
private fun StarDot(x: Dp, y: Dp, size: Dp, color: Color) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun RewardsHeader(coins: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Rewards", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Text("VIP access, coins, and daily perks", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x99131118))
                .border(1.dp, Color(0x34F8C84F), RoundedCornerShape(22.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinIcon(21)
            Spacer(Modifier.width(7.dp))
            Text(coins.toString(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun PremiumAccessPanel(
    packages: List<RewardPackage>,
    selectedPlanIndex: Int,
    onPlanSelected: (Int) -> Unit,
    onGetAccess: () -> Unit
) {
    val plans = pricingPlans(packages)
    val selected = selectedPlanIndex.coerceIn(0, plans.lastIndex)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF21C263F),
                        Color(0xF323172F),
                        Color(0xF20B0B10)
                    )
                )
            )
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(28.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF8E3CCB), Gold))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Ink, modifier = Modifier.size(29.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Pricing", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Unlock every\nDramaVerse story",
            color = Color.White,
            fontSize = 30.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Premium episodes, no ads, early drops, and reward boosts in one pass.",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
            modifier = Modifier.fillMaxWidth(0.92f)
        )
        Spacer(Modifier.height(22.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xB713111B))
                .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(22.dp))
        ) {
            plans.forEachIndexed { index, plan ->
                PaywallPlanRow(
                    plan = plan,
                    selected = index == selected,
                    onClick = { onPlanSelected(index) }
                )
                if (index != plans.lastIndex) DividerLine()
            }
        }
        Spacer(Modifier.height(20.dp))
        IncludedList()
        Spacer(Modifier.height(22.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White)
                .clickable(onClick = onGetAccess),
            contentAlignment = Alignment.Center
        ) {
            Text("Get Full Access", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            LegalLink("Terms of Use")
            LegalDot()
            LegalLink("Privacy Policy")
            LegalDot()
            LegalLink("Restore")
        }
    }
}

@Composable
private fun PaywallPlanRow(plan: PricingPlan, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick)
            .background(if (selected) Color(0x18111114) else Color.Transparent)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(31.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (selected) Color.White else Color.Transparent)
                .border(2.dp, if (selected) Color.White else Color(0xFFC9C7D2), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(plan.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
            Text("${plan.price} / ${plan.period}", color = Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        }
        if (plan.bestValue) {
            Text(
                "BEST VALUE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Pink)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun IncludedList() {
    Column(Modifier.fillMaxWidth()) {
        Text("What's included", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        Spacer(Modifier.height(12.dp))
        IncludedRow("Unlimited access to all premium episodes")
        IncludedRow("No ads during every drama session")
        IncludedRow("Early access to finales and new drops")
        IncludedRow("HD streaming plus reward multipliers")
    }
}

@Composable
private fun IncludedRow(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, color = Color(0xFFEDE8F0), fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    }
}

@Composable
private fun CoinWallet(coins: Int, packages: List<RewardPackage>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Panel)
            .border(1.dp, Stroke, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Coin Wallet", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Text("Unlock episodes instantly", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x33F8C84F))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinIcon(22)
                Spacer(Modifier.width(8.dp))
                Text(coins.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(packages.take(5)) { pack ->
                CoinPackPill(pack)
            }
        }
    }
}

@Composable
private fun CoinPackPill(pack: RewardPackage) {
    Column(
        modifier = Modifier
            .width(124.dp)
            .height(132.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF201D24))
            .border(1.dp, if (pack.bestValue) Color(0x80F8C84F) else Stroke, RoundedCornerShape(18.dp))
            .padding(13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CoinIcon(34)
        Spacer(Modifier.height(10.dp))
        Text(coinAmountLabel(pack), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        Text(pack.price.withDollarSign(), color = SoftPink, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
    }
}

@Composable
private fun DailyRewards(currentDay: Int, onClaimDaily: () -> Unit) {
    PremiumSectionHeader("Daily Check-in", "Day $currentDay of 7")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..7).forEach { day ->
            val active = day == currentDay
            val complete = day < currentDay
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(if (day == 7) 96.dp else 82.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when {
                            active -> Color(0x332D2510)
                            complete -> Color(0xFF3A1722)
                            else -> Color(0xFF151419)
                        }
                    )
                    .border(
                        1.dp,
                        when {
                            active -> Gold
                            complete -> Color(0x88FFB9C5)
                            else -> Stroke
                        },
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = active, onClick = onClaimDaily),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("D$day", color = if (active) Gold else Muted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                Spacer(Modifier.height(6.dp))
                Icon(
                    imageVector = if (day == 7) Icons.Filled.EmojiEvents else if (complete) Icons.Filled.CheckCircle else Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (active) Gold else if (complete) SoftPink else Color(0xFF5F5962),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(if (day == 7) "Bonus" else "+${day * 5}", color = if (active) Gold else Dim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
        }
    }
}

@Composable
private fun MissionDeck(watchMinutes: Int, spinAvailable: Int, onSpin: () -> Unit) {
    PremiumSectionHeader("Earn More", "Missions")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MissionCard(
            icon = Icons.Filled.PlayCircle,
            title = "Daily Watch Time",
            body = "Watch 30 mins to earn 50 Coins",
            stat = "${watchMinutes.coerceAtMost(30)}/30m"
        ) {
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A272E))) {
                Box(
                    Modifier
                        .fillMaxWidth((watchMinutes / 30f).coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(SoftPink, RoundedCornerShape(8.dp))
                )
            }
        }
        MissionCard(
            icon = Icons.Filled.Star,
            title = "Lucky Spin",
            body = "Spin once daily for bonus coins",
            stat = "$spinAvailable free"
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFF4D74), Color(0xFFD70B43))))
                    .clickable(onClick = onSpin),
                contentAlignment = Alignment.Center
            ) {
                Text("SPIN NOW", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
        }
    }
}

@Composable
private fun MissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    stat: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.dp, Stroke, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x26FFB9C5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = SoftPink, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Text(body, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            }
            Text(stat, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun AchievementStrip(achievements: List<RewardAchievement>) {
    PremiumSectionHeader("Achievements", "View All")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(achievements) { achievement ->
            AchievementTile(achievement)
        }
    }
}

@Composable
private fun AchievementTile(achievement: RewardAchievement) {
    Column(
        modifier = Modifier
            .width(156.dp)
            .height(148.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.dp, if (achievement.unlocked) Color(0x66F8C84F) else Stroke, RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(if (achievement.unlocked) Color(0x26F8C84F) else Color(0xFF1F1D22)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = if (achievement.unlocked) Gold else Color(0xFF5D5860), modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(achievement.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
        Text(achievement.subtitle, color = Dim, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
    }
}

@Composable
private fun PremiumSectionHeader(title: String, action: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, color = SoftPink, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x16FFFFFF)))
}

@Composable
private fun LegalLink(text: String) {
    Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
}

@Composable
private fun LegalDot() {
    Text("  -  ", color = Color(0x88FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
}

@Composable
private fun CoinIcon(size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF17100D), modifier = Modifier.size((size * 0.62f).dp))
    }
}

private data class PricingPlan(
    val title: String,
    val price: String,
    val period: String,
    val bestValue: Boolean = false
)

private fun pricingPlans(packages: List<RewardPackage>): List<PricingPlan> {
    val byTitle = packages.associateBy { it.title.lowercase() }
    return listOf(
        PricingPlan(
            title = "Monthly",
            price = byTitle.firstPrice("monthly") ?: "$10.99",
            period = "month"
        ),
        PricingPlan(
            title = "Yearly",
            price = byTitle.firstPrice("yearly", "annual") ?: "$100.99",
            period = "year",
            bestValue = true
        ),
        PricingPlan(
            title = "Weekly",
            price = byTitle.firstPrice("weekly") ?: "$4.99",
            period = "week"
        )
    )
}

private fun Map<String, RewardPackage>.firstPrice(vararg titleParts: String): String? {
    val match = entries.firstOrNull { (title, _) ->
        titleParts.any { part -> title.contains(part) }
    }?.value ?: return null
    return match.price.withDollarSign()
}

private fun String.withDollarSign(): String {
    val clean = trim()
    if (clean.isBlank()) return "$0.99"
    return when {
        clean.startsWith("$") -> clean
        clean.firstOrNull()?.isDigit() == true -> "$$clean"
        else -> clean
    }
}

private fun coinAmountLabel(pack: RewardPackage): String {
    val amount = pack.amount.takeIf { it > 0 } ?: pack.title.filter(Char::isDigit).toIntOrNull() ?: 50
    return "$amount Coins"
}
