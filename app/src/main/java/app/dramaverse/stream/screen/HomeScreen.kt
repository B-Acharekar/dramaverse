package app.dramaverse.stream.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.data.ContinueWatchingItem
import app.dramaverse.stream.data.DramaItem
import app.dramaverse.stream.data.HomeFeed
import app.dramaverse.stream.model.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL

private val HomeBackground = Color(0xFF09090B)
private val Panel = Color(0xFF151318)
private val Pink = Color(0xFFFF3E68)
private val SoftPink = Color(0xFFFFC0C9)
private val Gold = Color(0xFFF5C65B)

@Composable
fun HomeScreen(
    backendBaseUrl: String,
    onOpenShorts: (Int?) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(backendBaseUrl) {
        viewModel.loadHome(backendBaseUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        val feed = uiState.feed
        if (feed == null) {
            HomeSkeleton()
        } else {
            HomeContent(
                feed = feed,
                selectedMood = uiState.selectedMood,
                isMoodLoading = uiState.isMoodLoading,
                onMoodSelected = { mood -> viewModel.selectMood(backendBaseUrl, mood) },
                onSearchClick = { viewModel.openHotSearch(backendBaseUrl) },
                onOpenShorts = onOpenShorts
            )
        }
        BottomNavigationBar(
            selected = "Home",
            onHome = {},
            onShorts = { onOpenShorts(null) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HomeContent(
    feed: HomeFeed,
    selectedMood: String?,
    isMoodLoading: Boolean,
    onMoodSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onOpenShorts: (Int?) -> Unit
) {
    val heroItems = feed.heroItems()
    val heroKeys = heroItems.map { it.uniqueKey() }.toSet()
    val trendingItems = feed.trending
        .filterNot { it.uniqueKey() in heroKeys }
        .ifEmpty {
            feed.moreLikeThis.filterNot { it.uniqueKey() in heroKeys }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 92.dp)
    ) {
        item {
            HeroCarousel(
                items = heroItems,
                selectedMood = selectedMood,
                isMoodLoading = isMoodLoading,
                onMoodSelected = onMoodSelected,
                onSearchClick = onSearchClick,
                onOpenShorts = onOpenShorts
            )
        }
        if (feed.continueWatching.isNotEmpty()) {
            item { ContinueWatching(feed.continueWatching, onOpenShorts) }
        }
        item { PosterRail(title = "Trending Now", items = trendingItems, showTrend = true, onOpenShorts = onOpenShorts) }
        item { TopRatedCard(feed.topRated, onOpenShorts) }
        item { ActionCards() }
        item { Spacer(modifier = Modifier.height(18.dp)) }
    }
}

@Composable
private fun HeroCarousel(
    items: List<DramaItem>,
    selectedMood: String?,
    isMoodLoading: Boolean,
    onMoodSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onOpenShorts: (Int?) -> Unit
) {
    val pageCount = 10_000
    val startPage = pageCount / 2
    val pagerState = rememberPagerState(initialPage = startPage) { pageCount }

    LaunchedEffect(items) {
        while (true) {
            delay(5200)
            pagerState.animateScrollToPage(
                page = pagerState.currentPage + 1,
                animationSpec = tween(durationMillis = 1150, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(620.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            HeroSection(
                item = items[page.floorMod(items.size)],
                selectedIndex = pagerState.currentPage.floorMod(items.size),
                itemCount = items.size,
                onOpenShorts = onOpenShorts
            )
        }
        HomeTopBar(
            selectedMood = selectedMood,
            isMoodLoading = isMoodLoading,
            onMoodSelected = onMoodSelected,
            onSearchClick = onSearchClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun HeroSection(
    item: DramaItem,
    selectedIndex: Int,
    itemCount: Int,
    onOpenShorts: (Int?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(620.dp)
    ) {
        NetworkDramaImage(item.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, item.title)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x66000000),
                            Color(0x16000000),
                            Color(0x33000000),
                            Color(0xF509090B)
                        ),
                        startY = 0f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0x88000000), Color.Transparent, Color(0x33000000))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp)
        ) {
            TagPill("FEATURED", Gold, Color(0x663B2F13))
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.description,
                color = Color(0xFFE0C9D0),
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
                modifier = Modifier.fillMaxWidth(0.88f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WatchButton(width = 152, onClick = { onOpenShorts(item.id.takeIf { it != 0 }) })
                Spacer(modifier = Modifier.width(12.dp))
                PlusButton()
                Spacer(modifier = Modifier.weight(1f))
                HeroIndicators(selectedIndex = selectedIndex, count = itemCount)
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@Composable
private fun HeroIndicators(selectedIndex: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count.coerceAtMost(4)) { index ->
            Box(
                Modifier
                    .width(if (selectedIndex == index) 36.dp else 5.dp)
                    .height(4.dp)
                    .background(
                        if (selectedIndex == index) SoftPink else Color(0xFF57525A),
                        RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    selectedMood: String? = null,
    isMoodLoading: Boolean = false,
    onMoodSelected: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF5000000),
                        Color(0xC9000000),
                        Color(0x72000000),
                        Color(0x22000000),
                        Color.Transparent
                    )
                )
            )
            .padding(top = 12.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x6618171C))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable(onClick = onSearchClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFF2E3E7), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(18.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x6618171C))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color(0xFFE8D5DA), modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFF0B18B), Color(0xFF351B1F))))
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                MoodChip(
                    label = "Hot",
                    selected = selectedMood == "hot",
                    loading = isMoodLoading && selectedMood == "hot",
                    onClick = onSearchClick
                )
            }
            items(moodLabels) { label ->
                MoodChip(
                    label = label.display,
                    selected = selectedMood == label.query,
                    loading = isMoodLoading && selectedMood == label.query,
                    onClick = { onMoodSelected(label.query) }
                )
            }
        }
    }
}

private data class MoodLabel(
    val display: String,
    val query: String
)

private val moodLabels = listOf(
    MoodLabel("Happy", "happy"),
    MoodLabel("Emotional", "emotional"),
    MoodLabel("Action", "action"),
    MoodLabel("Romance", "romance"),
    MoodLabel("Horror", "horror"),
    MoodLabel("Mind-blowing", "mind blowing"),
    MoodLabel("Relax", "relax")
)

@Composable
private fun MoodChip(
    label: String,
    selected: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFFF4D9DF) else Color(0x8A17151A))
            .border(
                1.dp,
                if (selected) Color(0xFFFFE4EA) else Color(0x33FFFFFF),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (loading) "$label..." else label,
            color = if (selected) Color(0xFF180C10) else Color(0xFFEEDFE4),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun ContinueWatching(
    items: List<ContinueWatchingItem>,
    onOpenShorts: (Int?) -> Unit
) {
    SectionHeader(title = "Continue Watching", action = "SEE ALL")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item -> ContinueCard(item, onOpenShorts) }
    }
}

@Composable
private fun ContinueCard(
    item: ContinueWatchingItem,
    onOpenShorts: (Int?) -> Unit
) {
    val film = item.film
    Column(
        modifier = Modifier
            .width(218.dp)
            .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(122.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Panel)
        ) {
            NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xA8000000))))
            )
            Text(
                "Ep ${item.episodeNumber}/${film.episodeTotal.coerceAtLeast(item.episodeNumber)}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .background(Color(0x99000000), RoundedCornerShape(10.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                letterSpacing = 0.sp
            )
            Text(
                formatContinueTime(item.progressSeconds),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(7.dp)
                    .background(Color(0x99000000), RoundedCornerShape(10.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                letterSpacing = 0.sp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0x66FFFFFF))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(item.progressFraction.takeIf { it > 0f } ?: 0.04f)
                    .height(4.dp)
                    .background(Pink, RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(film.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
        Text(
            "Ep ${item.episodeNumber}/${film.episodeTotal.coerceAtLeast(item.episodeNumber)}  •  ${formatContinueTime(item.progressSeconds)} / ${formatContinueTime(item.durationSeconds)}",
            color = Color(0xFFC7B6BC),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun PosterRail(
    title: String,
    items: List<DramaItem>,
    showTrend: Boolean,
    onOpenShorts: (Int?) -> Unit
) {
    SectionHeader(title = title, action = if (showTrend) "TRENDING" else null)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item -> PosterCard(item, onOpenShorts) }
    }
}

@Composable
private fun PosterCard(item: DramaItem, onOpenShorts: (Int?) -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable { onOpenShorts(item.id.takeIf { it != 0 }) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(12.dp))
                .background(Panel)
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
        ) {
            NetworkDramaImage(item.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, item.title)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB0000000))))
            )
            if (item.isPremium) {
                PremiumBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                )
            }
            Text(
                "${item.episodeTotal} Eps",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(7.dp)
                    .background(Color(0x99000000), RoundedCornerShape(10.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                letterSpacing = 0.sp
            )
        }
        Spacer(modifier = Modifier.height(9.dp))
        Text("* ${item.rating}", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
        Text(item.title, color = Color.White, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
    }
}

@Composable
private fun TopRatedCard(item: DramaItem, onOpenShorts: (Int?) -> Unit) {
    SectionHeader("Top Rated This Week")
    Box(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .height(236.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .clickable { onOpenShorts(item.id.takeIf { it != 0 }) }
    ) {
        NetworkDramaImage(item.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, item.title)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x22000000), Color(0x1A000000), Color(0xF409090B)),
                        startY = 50f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text("WEEKLY TOP #1", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            Spacer(Modifier.height(7.dp))
            Text(item.title, color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            Spacer(Modifier.height(7.dp))
            Text("* ${item.rating}    ${item.genre}    ${item.episodeTotal} Eps", color = Color(0xFFEBD3AF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun ActionCards() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallActionCard("VIP", "Join VIP Club", "Unlock all episodes\ntoday", Modifier.weight(1f))
        SmallActionCard("50", "Daily Rewards", "Claim your 50 coins", Modifier.weight(1f))
    }
}

@Composable
private fun SmallActionCard(icon: String, title: String, body: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A1A22), Color(0xFF171318))))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(icon, color = SoftPink, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        Text(body, color = Color(0xFFCDB5BC), fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    }
}

@Composable
fun BottomNavigationBar(
    selected: String,
    onHome: () -> Unit,
    onShorts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(Color(0xF20B0B0E))
            .border(1.dp, Color(0xFF211B22))
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(Icons.Filled.Home, "Home", selected == "Home", onHome)
        NavItem(Icons.Filled.Explore, "Shorts", selected == "Shorts", onShorts)
        NavItem(Icons.Filled.VideoLibrary, "Library", selected == "Library", {})
        NavItem(Icons.Filled.CardGiftcard, "Rewards", selected == "Rewards", {})
        NavItem(Icons.Filled.Person, "Profile", selected == "Profile", {})
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) Gold else Color(0xFF9B858E)
    val background = if (selected) Color(0x1FF5C65B) else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (selected) 25.dp else 22.dp))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        Spacer(modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, color = SoftPink, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun WatchButton(fullWidth: Boolean = false, width: Int = 140, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier.width(width.dp))
            .height(50.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFF416E), Color(0xFFE9164D))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF2A0D16), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Watch Now", color = Color(0xFF2A0D16), fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun PremiumBadge(modifier: Modifier = Modifier) {
    Text(
        text = "PREMIUM",
        color = Gold,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x33F5C65B))
            .border(1.dp, Color(0xAAF5C65B), RoundedCornerShape(16.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
private fun PlusButton(size: Int = 44) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xCC17171B))
            .border(1.dp, Color(0xFF343139), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light, letterSpacing = 0.sp)
    }
}

@Composable
private fun TagPill(text: String, color: Color, background: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun NetworkDramaImage(
    imageUrl: String,
    modifier: Modifier,
    contentScale: ContentScale,
    seed: String
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, imageUrl) {
        value = if (imageUrl.isBlank()) null else loadBitmap(imageUrl)
    }

    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        GeneratedPoster(seed = seed, modifier = modifier)
    }
}

@Composable
private fun GeneratedPoster(seed: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = posterColors(seed),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA050507))))
        )
    }
}

@Composable
private fun HomeSkeleton() {
    HomeLoaderTemplate()
}

@Composable
private fun HomeLoaderTemplate() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground),
        contentPadding = PaddingValues(bottom = 92.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(620.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF21171E), Color(0xFF101014))))
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(76.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xAA0D0D10), Color.Transparent)))
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    SkeletonBlock(width = 22, height = 22)
                    Spacer(modifier = Modifier.width(18.dp))
                    SkeletonBlock(width = 21, height = 21)
                    Spacer(modifier = Modifier.width(14.dp))
                    SkeletonBlock(width = 30, height = 30)
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    SkeletonBlock(width = 90, height = 18)
                    Spacer(modifier = Modifier.height(12.dp))
                    SkeletonBlock(width = 260, height = 28)
                    Spacer(modifier = Modifier.height(8.dp))
                    SkeletonBlock(width = 310, height = 16)
                    Spacer(modifier = Modifier.height(20.dp))
                    SkeletonBlock(width = 140, height = 48)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(18.dp)) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(4) {
                    Column(Modifier.width(150.dp)) {
                        SkeletonBlock(width = 150, height = 220)
                        Spacer(Modifier.height(8.dp))
                        SkeletonBlock(width = 72, height = 12)
                        Spacer(Modifier.height(5.dp))
                        SkeletonBlock(width = 110, height = 14)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(width: Int, height: Int) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF242027))
    )
}

private suspend fun loadBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        URL(imageUrl).openStream().use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}

private fun posterColors(seed: String): List<Color> {
    val hash = seed.fold(0) { acc, char -> acc + char.code }
    return when (hash % 5) {
        0 -> listOf(Color(0xFF40202B), Color(0xFF102438), Color(0xFF09090C))
        1 -> listOf(Color(0xFF233D52), Color(0xFF512634), Color(0xFF08090D))
        2 -> listOf(Color(0xFF4A321F), Color(0xFF1C243C), Color(0xFF09090B))
        3 -> listOf(Color(0xFF173E36), Color(0xFF3C263F), Color(0xFF08080B))
        else -> listOf(Color(0xFF1F233F), Color(0xFF4A1F2D), Color(0xFF07080B))
    }
}

private fun HomeFeed.heroItems(): List<DramaItem> {
    val merged = (listOf(hero) + trending + moreLikeThis)
        .distinctBy { it.uniqueKey() }
    val padded = if (merged.size >= 4) merged else merged + List(4 - merged.size) { hero }
    return padded.take(4)
}

private fun DramaItem.uniqueKey(): Any = id.takeIf { it != 0 } ?: title

private fun formatContinueTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
