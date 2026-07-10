package app.dramaverse.stream.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Bookmark
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.R
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
    onLibrary: () -> Unit,
    onSearch: (String) -> Unit,
    onRewards: () -> Unit,
    onPlanner: () -> Unit,
    onNotifications: () -> Unit,
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
                savedFilmIds = uiState.savedFilmIds,
                onMoodSelected = onSearch,
                onSearchClick = { onSearch("hot") },
                onNotifications = onNotifications,
                onOpenShorts = onOpenShorts,
                onPlanner = onPlanner,
                onRewards = onRewards,
                onToggleWatchList = { filmId, enabled ->
                    viewModel.setReminder(backendBaseUrl, filmId, enabled)
                }
            )
        }
        BottomNavigationBar(
            selected = "Home",
            onHome = {},
            onShorts = { onOpenShorts(null) },
            onLibrary = onLibrary,
            onRewards = onRewards,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HomeContent(
    feed: HomeFeed,
    selectedMood: String?,
    isMoodLoading: Boolean,
    savedFilmIds: Set<Int>,
    onMoodSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNotifications: () -> Unit,
    onOpenShorts: (Int?) -> Unit,
    onPlanner: () -> Unit,
    onRewards: () -> Unit,
    onToggleWatchList: (Int, Boolean) -> Unit
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
                hotTags = feed.hotTags,
                selectedMood = selectedMood,
                isMoodLoading = isMoodLoading,
                savedFilmIds = savedFilmIds,
                onMoodSelected = onMoodSelected,
                onSearchClick = onSearchClick,
                onNotifications = onNotifications,
                onOpenShorts = onOpenShorts,
                onPlanner = onPlanner,
                onToggleWatchList = onToggleWatchList
            )
        }
        if (feed.continueWatching.isNotEmpty()) {
            item { ContinueWatching(feed.continueWatching, onOpenShorts) }
        }
        item { PosterRail(title = stringResource(R.string.trending_now), items = trendingItems, showTrend = true, onOpenShorts = onOpenShorts) }
        item { TopRatedCard(feed.topRated, onOpenShorts) }
        item { ActionCards(onPlanner = onPlanner, onRewards = onRewards) }
        item { Spacer(modifier = Modifier.height(18.dp)) }
    }
}

@Composable
private fun HeroCarousel(
    items: List<DramaItem>,
    hotTags: List<String>,
    selectedMood: String?,
    isMoodLoading: Boolean,
    savedFilmIds: Set<Int>,
    onMoodSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNotifications: () -> Unit,
    onOpenShorts: (Int?) -> Unit,
    onPlanner: () -> Unit,
    onToggleWatchList: (Int, Boolean) -> Unit
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
                saved = items[page.floorMod(items.size)].id in savedFilmIds,
                onOpenShorts = onOpenShorts,
                onToggleWatchList = onToggleWatchList
            )
        }
            HomeTopBar(
                hotTags = hotTags,
                selectedMood = selectedMood,
                isMoodLoading = isMoodLoading,
                onMoodSelected = onMoodSelected,
                onSearchClick = onSearchClick,
                onNotifications = onNotifications,
                modifier = Modifier.align(Alignment.TopCenter)
            )
    }
}

@Composable
private fun HeroSection(
    item: DramaItem,
    selectedIndex: Int,
    itemCount: Int,
    saved: Boolean,
    onOpenShorts: (Int?) -> Unit,
    onToggleWatchList: (Int, Boolean) -> Unit
) {
    val filmId = item.id.takeIf { it != 0 }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(620.dp)
            .clickable { onOpenShorts(filmId) }
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
            TagPill(stringResource(R.string.featured), Gold, Color(0x663B2F13))
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
                WatchButton(width = 152, onClick = { onOpenShorts(filmId) })
                Spacer(modifier = Modifier.width(12.dp))
                SaveButton(
                    saved = saved,
                    onClick = {
                        // Mirrors Shorts bookmark behavior: Save toggles the watchlist state immediately.
                        filmId?.let { onToggleWatchList(it, !saved) }
                    }
                )
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
    hotTags: List<String> = emptyList(),
    selectedMood: String? = null,
    isMoodLoading: Boolean = false,
    onMoodSelected: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotifications: () -> Unit = {},
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
                .height(44.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark()
            Spacer(modifier = Modifier.weight(1f))
            HeaderIcon(Icons.Filled.Search, onSearchClick)
            Spacer(modifier = Modifier.width(12.dp))
            HeaderIcon(Icons.Filled.Notifications, onNotifications)
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                MoodChip(
                    label = stringResource(R.string.mood_hot),
                    selected = selectedMood == "hot",
                    loading = isMoodLoading && selectedMood == "hot",
                    onClick = onSearchClick
                )
            }
            items(hotTags.ifEmpty { fallbackHotTags }) { label ->
                MoodChip(
                    label = label,
                    selected = selectedMood.equals(label, ignoreCase = true),
                    loading = isMoodLoading && selectedMood.equals(label, ignoreCase = true),
                    onClick = { onMoodSelected(label) }
                )
            }
        }
    }
}

private val fallbackHotTags = listOf(
    "Trending",
    "New Release",
    "Billionaire",
    "Revenge",
    "CEO",
    "Family",
    "Mystery"
)

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.icon_2),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .padding(3.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text("DramaVerse", color = SoftPink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x6618171C))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFF2E3E7), modifier = Modifier.size(20.dp))
    }
}

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
private fun ContinueWatching(items: List<ContinueWatchingItem>, onOpenShorts: (Int?) -> Unit) {
    SectionHeader(title = stringResource(R.string.continue_watching), action = stringResource(R.string.see_all))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item -> ContinueCard(item, onOpenShorts) }
    }
}

@Composable
private fun ContinueCard(item: ContinueWatchingItem, onOpenShorts: (Int?) -> Unit) {
    val film = item.film
    Column(
        modifier = Modifier
            .width(210.dp)
            .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Panel)
        ) {
            NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(item.progressFraction.takeIf { it > 0f } ?: 0.04f)
                    .height(4.dp)
                    .background(Pink, RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(film.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
        Text(stringResource(R.string.episode_progress, item.episodeNumber, film.episodeTotal.coerceAtLeast(item.episodeNumber)), color = Color(0xFFC7B6BC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, letterSpacing = 0.sp)
    }
}

@Composable
private fun PosterRail(
    title: String,
    items: List<DramaItem>,
    showTrend: Boolean,
    onOpenShorts: (Int?) -> Unit
) {
    SectionHeader(title = title, action = if (showTrend) stringResource(R.string.trending_tag) else null)
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
                stringResource(R.string.episodes_count, item.episodeTotal),
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
        Text(stringResource(R.string.rating_value, item.rating), color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
        Text(item.title, color = Color.White, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
    }
}

@Composable
private fun TopRatedCard(item: DramaItem, onOpenShorts: (Int?) -> Unit) {
    SectionHeader(stringResource(R.string.top_rated_this_week))
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
            Text(stringResource(R.string.weekly_top_1), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            Spacer(Modifier.height(7.dp))
            Text(item.title, color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            Spacer(Modifier.height(7.dp))
            Text(stringResource(R.string.rating_genre_episodes, item.rating, item.genre, item.episodeTotal), color = Color(0xFFEBD3AF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun ActionCards(onPlanner: () -> Unit, onRewards: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallActionCard(
            stringResource(R.string.planner_action_short),
            stringResource(R.string.plan_saved_dramas),
            stringResource(R.string.schedule_watchlist_reminders),
            Modifier.weight(1.15f),
            onPlanner
        )
        SmallActionCard(stringResource(R.string.vip_short), stringResource(R.string.join_vip_club), stringResource(R.string.unlock_all_episodes), Modifier.weight(0.85f), onRewards)
    }
}

@Composable
private fun SmallActionCard(icon: String, title: String, body: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A1A22), Color(0xFF171318))))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
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
    onLibrary: () -> Unit,
    onRewards: () -> Unit = {},
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
        NavItem(Icons.Filled.Home, stringResource(R.string.nav_home), selected == "Home", onHome)
        NavItem(Icons.Filled.Explore, stringResource(R.string.nav_shorts), selected == "Shorts", onShorts)
        NavItem(Icons.Filled.VideoLibrary, stringResource(R.string.nav_library), selected == "Library", onLibrary)
        NavItem(Icons.Filled.CardGiftcard, stringResource(R.string.nav_rewards), selected == "Rewards", onRewards)
        NavItem(Icons.Filled.Person, stringResource(R.string.nav_profile), selected == "Profile", {})
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
            Text(stringResource(R.string.watch_now), color = Color(0xFF2A0D16), fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun PremiumBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.premium),
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
private fun SaveButton(saved: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .height(50.dp)
            .width(92.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (saved) Color(0x33F5C65B) else Color(0xCC17171B))
            .border(1.dp, if (saved) Gold else Color(0xFF343139), RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (saved) {
            Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(stringResource(R.string.saved), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        } else {
            Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Light, letterSpacing = 0.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(stringResource(R.string.save), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
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
fun NetworkDramaImage(
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

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
