package com.drama.x.drama.series.dramax.dramaseries.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drama.x.drama.series.dramax.dramaseries.data.ContinueWatchingItem
import com.drama.x.drama.series.dramax.dramaseries.data.DramaItem
import com.drama.x.drama.series.dramax.dramaseries.data.HomeFeed
import com.drama.x.drama.series.dramax.dramaseries.model.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL
import com.drama.x.drama.series.dramax.dramaseries.R



private val HomeBackground = Color(0xFF09090B)
private val Panel = Color(0xFF151318)
private val Pink = Color(0xFFFF3E68)
private val SoftPink = Color(0xFFFFC0C9)
private val Gold = Color(0xFFF5C65B)
private val CardPanel = Color(0xFF1A1A1A)

private enum class HomeTab(val label: String) {
    Popular("Popular"),
    New("New"),
    Ranking("Ranking"),
    Categories("Categories")
}

private enum class CategorySheet {
    Filters,
    Sort
}

private enum class AudienceFilter(val label: String) {
    All("All"),
    Male("Male"),
    Female("Female")
}

private enum class CategoryFilter(val label: String) {
    All("All"),
    Modern("Modern"),
    Historical("Historical"),
    Fantasy("Fantasy"),
    Romance("Romance")
}

private enum class CategorySort(val label: String) {
    Newest("Newest"),
    Popular("Popular"),
    Rating("Rating"),
    Trending("Trending")
}

@Composable
fun HomeScreen(
    backendBaseUrl: String,
    onOpenShorts: (Int?) -> Unit,
    onLibrary: () -> Unit,
    onSearch: (String) -> Unit,
    onRewards: () -> Unit,
    onNotifications: () -> Unit,
    onProfile:() -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(HomeTab.Popular) }
    var activeCategorySheet by remember { mutableStateOf<CategorySheet?>(null) }
    var selectedAudienceFilter by remember { mutableStateOf(AudienceFilter.All) }
    var selectedCategoryFilter by remember { mutableStateOf(CategoryFilter.All) }
    var selectedCategorySort by remember { mutableStateOf(CategorySort.Newest) }

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
                selectedTab = selectedTab,
                savedFilmIds = uiState.savedFilmIds,
                savedFilms = uiState.savedFilms,
                onSearchClick = { onSearch("hot") },
                onNotifications = onNotifications,
                onOpenShorts = onOpenShorts,
                onLibrary = onLibrary,
                onTabSelected = { selectedTab = it },
                audienceFilter = selectedAudienceFilter,
                categoryFilter = selectedCategoryFilter,
                categorySort = selectedCategorySort,
                onOpenCategorySheet = { activeCategorySheet = it },
                onToggleWatchList = { film, enabled ->
                    viewModel.setReminder(backendBaseUrl, film, enabled)
                }
            )
        }
        BottomNavigationBar(
            selected = "Home",
            onHome = {},
            onShorts = { onOpenShorts(null) },
            onLibrary = onLibrary,
            onRewards = onRewards,
            onProfile = onProfile,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        when (activeCategorySheet) {
            CategorySheet.Filters -> CategoryFilterSheet(
                selectedAudience = selectedAudienceFilter,
                selectedCategory = selectedCategoryFilter,
                onDismiss = { activeCategorySheet = null },
                onApply = { audience, category ->
                    selectedAudienceFilter = audience
                    selectedCategoryFilter = category
                    activeCategorySheet = null
                }
            )

            CategorySheet.Sort -> CategorySortSheet(
                selectedSort = selectedCategorySort,
                onDismiss = { activeCategorySheet = null },
                onApply = { sort ->
                    selectedCategorySort = sort
                    activeCategorySheet = null
                }
            )

            null -> Unit
        }
    }
}

@Composable
private fun HomeContent(
    feed: HomeFeed,
    selectedTab: HomeTab,
    savedFilmIds: Set<Int>,
    savedFilms: List<DramaItem>,
    onSearchClick: () -> Unit,
    onNotifications: () -> Unit,
    onOpenShorts: (Int?) -> Unit,
    onLibrary: () -> Unit,
    onTabSelected: (HomeTab) -> Unit,
    audienceFilter: AudienceFilter,
    categoryFilter: CategoryFilter,
    categorySort: CategorySort,
    onOpenCategorySheet: (CategorySheet) -> Unit,
    onToggleWatchList: (DramaItem, Boolean) -> Unit
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerHeight = 104.dp + topInset
    val heroItems = feed.heroItems()
    val heroKeys = heroItems.map { it.uniqueKey() }.toSet()
    val popularItems = feed.trending
        .filterNot { it.uniqueKey() in heroKeys }
        .ifEmpty {
            feed.moreLikeThis.filterNot { it.uniqueKey() in heroKeys }
        }
    val allItems = (heroItems + feed.trending + feed.moreLikeThis + listOf(feed.topRated))
        .distinctBy { it.uniqueKey() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = headerHeight, bottom = 96.dp)
    ) {
        when (selectedTab) {
            HomeTab.Popular -> popularTab(
                heroItems = heroItems,
                popularItems = popularItems,
                feed = feed,
                savedFilmIds = savedFilmIds,
                savedFilms = savedFilms,
                onOpenShorts = onOpenShorts,
                onLibrary = onLibrary,
                onToggleWatchList = onToggleWatchList
            )

            HomeTab.New -> newTab(
                items = allItems,
                onOpenShorts = onOpenShorts
            )

            HomeTab.Ranking -> rankingTab(
                items = allItems,
                onOpenShorts = onOpenShorts
            )

            HomeTab.Categories -> categoriesTab(
                items = allItems,
                hotTags = feed.hotTags,
                audienceFilter = audienceFilter,
                categoryFilter = categoryFilter,
                categorySort = categorySort,
                onOpenSheet = onOpenCategorySheet,
                onOpenShorts = onOpenShorts
            )
        }
    }

    HomeTopBar(
        selectedTab = selectedTab,
        topInset = topInset,
        onTabSelected = onTabSelected,
        onSearchClick = onSearchClick,
        onNotifications = onNotifications
    )
}

private fun LazyListScope.popularTab(
    heroItems: List<DramaItem>,
    popularItems: List<DramaItem>,
    feed: HomeFeed,
    savedFilmIds: Set<Int>,
    savedFilms: List<DramaItem>,
    onOpenShorts: (Int?) -> Unit,
    onLibrary: () -> Unit,
    onToggleWatchList: (DramaItem, Boolean) -> Unit
) {
    item {
        HeroCarousel(
            items = heroItems,
            savedFilmIds = savedFilmIds,
            onOpenShorts = onOpenShorts,
            onToggleWatchList = onToggleWatchList
        )
    }
    item {
        SectionHeader(title = "Featured Highlights")
        CompactPosterGrid(items = popularItems.take(9), columns = 3, onOpenShorts = onOpenShorts)
    }
    item { ContinueWatching(feed.continueWatching, onOpenShorts) }
    item {
        SectionHeader(title = "My Favorites", action = stringResource(R.string.see_all), onAction = onLibrary)
        FavoriteGrid(items = savedFilms.take(4), onOpenShorts = onOpenShorts)
    }
    item { Spacer(modifier = Modifier.height(14.dp)) }
}

private fun LazyListScope.newTab(
    items: List<DramaItem>,
    onOpenShorts: (Int?) -> Unit
) {
    item {
        AccentTitle("Fresh on DramaX")
        TallPosterGrid(items = items, onOpenShorts = onOpenShorts, badges = listOf("HOT", "NEW", "NEW", "NEW", "TRENDING"))
        Spacer(Modifier.height(12.dp))
    }
}

private fun LazyListScope.rankingTab(
    items: List<DramaItem>,
    onOpenShorts: (Int?) -> Unit
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Weekly Top 20", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Spacer(Modifier.weight(1f))
            Text("Updated 3h ago", color = Color(0xFF9CA3AF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
        }
    }
    items(items.take(3).withIndex().toList()) { (index, item) ->
        RankingHeroRow(rank = index + 1, item = item, onOpenShorts = onOpenShorts)
    }
    items(items.drop(3).take(17).withIndex().toList()) { (index, item) ->
        RankingListRow(rank = index + 4, item = item, onOpenShorts = onOpenShorts)
    }
    item { Spacer(Modifier.height(12.dp)) }
}

private fun LazyListScope.categoriesTab(
    items: List<DramaItem>,
    hotTags: List<String>,
    audienceFilter: AudienceFilter,
    categoryFilter: CategoryFilter,
    categorySort: CategorySort,
    onOpenSheet: (CategorySheet) -> Unit,
    onOpenShorts: (Int?) -> Unit
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolPill(Icons.Filled.FilterList, "FILTERS", onClick = { onOpenSheet(CategorySheet.Filters) })
            Spacer(Modifier.weight(1f))
            ToolPill(Icons.AutoMirrored.Filled.Sort, "SORT BY", onClick = { onOpenSheet(CategorySheet.Sort) })
        }
        val categoryItems = items
            .distinctBy { it.uniqueKey() }
            .filterByCategoryControls(audienceFilter, categoryFilter)
            .sortForCategory(categorySort)
            .take(12)
        CompactPosterGrid(items = categoryItems, columns = 3, onOpenShorts = onOpenShorts)
    }
}

@Composable
private fun HeroCarousel(
    items: List<DramaItem>,
    savedFilmIds: Set<Int>,
    onOpenShorts: (Int?) -> Unit,
    onToggleWatchList: (DramaItem, Boolean) -> Unit
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
            .height(374.dp)
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
    }
}

@Composable
private fun HeroSection(
    item: DramaItem,
    selectedIndex: Int,
    itemCount: Int,
    saved: Boolean,
    onOpenShorts: (Int?) -> Unit,
    onToggleWatchList: (DramaItem, Boolean) -> Unit
) {
    val filmId = item.id.takeIf { it != 0 }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(374.dp)
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
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.description,
                color = Color(0xFFE0C9D0),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
                modifier = Modifier.fillMaxWidth(0.88f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WatchButton(width = 135, height = 35, onClick = { onOpenShorts(filmId) })
                Spacer(modifier = Modifier.width(12.dp))
                PlusButton(
                    saved = saved,
                    size = 35,
                    onClick = {
                        // Mirrors Shorts bookmark behavior: Save toggles the watchlist state immediately.
                        filmId?.let { onToggleWatchList(item, !saved) }
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
    selectedTab: HomeTab,
    topInset: androidx.compose.ui.unit.Dp,
    onTabSelected: (HomeTab) -> Unit,
    onSearchClick: () -> Unit = {},
    onNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp + topInset)
            .background(Color(0xF20B0B0D))
    ) {
        DramaXTopAppBar(
            topInset = topInset,
            onSearchClick = onSearchClick,
            onNotificationsClick = onNotifications
        )
        LazyRow(
            modifier = Modifier.height(40.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(HomeTab.values().toList()) { tab ->
                HomeTabChip(
                    label = tab.label,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

private val fallbackHotTags = listOf(
    "Billionaire",
    "Revenge",
    "CEO",
    "Family",
    "Romance",
    "Thriller"
)

private fun List<DramaItem>.filterByCategoryControls(
    audience: AudienceFilter,
    category: CategoryFilter
): List<DramaItem> {
    return filter { item ->
        item.matchesAudience(audience) && item.matchesCategory(category)
    }
}

private fun List<DramaItem>.sortForCategory(sort: CategorySort): List<DramaItem> {
    return when (sort) {
        CategorySort.Newest -> this
        CategorySort.Popular -> sortedWith(
            compareByDescending<DramaItem> { it.likeCount }
                .thenByDescending { it.rating.toFloatOrNull() ?: 0f }
                .thenByDescending { it.episodeTotal }
        )
        CategorySort.Rating -> sortedByDescending { it.rating.toFloatOrNull() ?: 0f }
        CategorySort.Trending -> sortedWith(
            compareByDescending<DramaItem> { it.isPremium }
                .thenByDescending { it.likeCount }
                .thenByDescending { it.rating.toFloatOrNull() ?: 0f }
        )
    }
}

private fun DramaItem.matchesAudience(audience: AudienceFilter): Boolean {
    if (audience == AudienceFilter.All) return true
    val haystack = searchableCategoryText()
    val maleKeywords = listOf("action", "thriller", "revenge", "war", "crime", "ceo", "billionaire", "fantasy")
    val femaleKeywords = listOf("romance", "love", "family", "heir", "bride", "wife", "autumn", "drama")
    return when (audience) {
        AudienceFilter.All -> true
        AudienceFilter.Male -> maleKeywords.any { it in haystack }
        AudienceFilter.Female -> femaleKeywords.any { it in haystack }
    }
}

private fun DramaItem.matchesCategory(category: CategoryFilter): Boolean {
    if (category == CategoryFilter.All) return true
    val haystack = searchableCategoryText()
    val keywords = when (category) {
        CategoryFilter.All -> emptyList()
        CategoryFilter.Modern -> listOf("modern", "ceo", "billionaire", "city", "office", "finance", "tech")
        CategoryFilter.Historical -> listOf("historical", "king", "queen", "empire", "legacy", "dynasty", "ancient")
        CategoryFilter.Fantasy -> listOf("fantasy", "magic", "crimson", "shadow", "void", "supernatural")
        CategoryFilter.Romance -> listOf("romance", "love", "vow", "bride", "wife", "autumn")
    }
    return keywords.any { it in haystack }
}

private fun DramaItem.searchableCategoryText(): String {
    return "$title $genre $description".lowercase()
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
private fun HomeTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFFFF5168) else Color(0xFF231F21))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFFE4BEBC),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
fun DramaXTopAppBar(
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp + topInset)
            .background(Color(0xFF0C0808))
            .padding(top = topInset)
            .border(width = 1.dp, color = Color(0x1AFFFFFF))
            .padding(start = 36.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderAssetLogo()
        Spacer(modifier = Modifier.width(1.dp))
        Text(
            text = "ramaX",
            color = SoftPink,
            fontSize = 16.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        AppHeaderIcon(Icons.Filled.Search, onSearchClick, Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(16.dp))
        AppHeaderIcon(Icons.Filled.Notifications, onNotificationsClick, Modifier.size(width = 16.dp, height = 20.dp))
    }
}

@Composable
private fun HeaderAssetLogo() {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("header-icon.png").use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(35.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Image(
            painter = painterResource(R.drawable.icon_2),
            contentDescription = null,
            modifier = Modifier.size(35.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun AppHeaderIcon(icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFE5BDBE), modifier = modifier)
    }
}

@Composable
private fun AccentTitle(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(Color(0xFFFF535A), RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color(0xFFE2E2E2), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    }
}

@Composable
private fun CompactPosterGrid(
    items: List<DramaItem>,
    columns: Int,
    onOpenShorts: (Int?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { item ->
                    CompactPosterCard(
                        item = item,
                        onOpenShorts = onOpenShorts,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CompactPosterCard(
    item: DramaItem,
    onOpenShorts: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.562f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1F2937))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x6BFF0000),
                        Color(0x6BF4BE4E)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onOpenShorts(item.id.takeIf { it != 0 }) }
    ) {
        NetworkDramaImage(item.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, item.title)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9000000))))
        )
        if (item.isPremium || item.rating.toFloatOrNull()?.let { it >= 4.8f } == true) {
            CornerBadge("HOT", Pink, icon = R.drawable.fire, modifier = Modifier.align(Alignment.TopStart).padding(5.dp))
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(7.dp)
        ) {
            Text(
                item.title,
                color = Color.White,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Text(
                "${item.genre} - ${item.episodeTotal} Eps",
                color = Color(0xFFE2C0BE),
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun TallPosterGrid(
    items: List<DramaItem>,
    onOpenShorts: (Int?) -> Unit,
    badges: List<String?>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.chunked(2).forEachIndexed { rowIndex, rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowItems.forEachIndexed { columnIndex, item ->
                    val itemIndex = rowIndex * 2 + columnIndex
                    TallPosterCard(
                        item = item,
                        badge = badges.getOrNull(itemIndex),
                        onOpenShorts = onOpenShorts,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TallPosterCard(
    item: DramaItem,
    badge: String?,
    onOpenShorts: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.663f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF282A2B))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x6BFF0000),
                        Color(0x6BF4BE4E)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onOpenShorts(item.id.takeIf { it != 0 }) }
    ) {
        NetworkDramaImage(item.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, item.title)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE000000))))
        )
        if (badge != null) {
            val badgeColor: Color
            val badgeIcon: Int

            when (badge) {
                "NEW" -> {
                    badgeColor = Color(0xFFEAB308)
                    badgeIcon = R.drawable.sticker
                }

                "TRENDING" -> {
                    badgeColor = Color(0xFF54D84A)
                    badgeIcon = R.drawable.trend
                }

                "HOT" -> {
                    badgeColor = Pink
                    badgeIcon = R.drawable.fire
                }

                else -> {
                    badgeColor = Pink
                    badgeIcon = R.drawable.ic_star
                }
            }

            CornerBadge(
                text = badge,
                color = badgeColor,
                icon = badgeIcon,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text("${item.rating} - ${item.genre}", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            }
            Text(item.title, color = Color(0xFFE2E2E2), fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun FavoriteGrid(items: List<DramaItem>, onOpenShorts: (Int?) -> Unit) {
    if (items.isEmpty()) {
        EmptyFavorites(onOpenShorts)
        return
    }
    TallPosterGrid(
        items = items,
        onOpenShorts = onOpenShorts,
        badges = listOf("HOT", null, "NEW", "NEW")
    )
}

@Composable
private fun EmptyFavorites(onOpenShorts: (Int?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF151318))
            .border(1.dp, Color(0xFF2B252B), RoundedCornerShape(12.dp))
            .clickable { onOpenShorts(null) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("No favorites yet", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Text("Bookmark a drama to add it here.", color = Color(0xFFC7B6BC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun RankingHeroRow(rank: Int, item: DramaItem, onOpenShorts: (Int?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(112.dp)
            .clickable { onOpenShorts(item.id.takeIf { it != 0 }) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (textBrush, strokeColor) = when (rank) {
            1 -> Pair(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x66FF4D6D),
                        Color(0xFFFF4D6D)
                    )
                ),
                Color(0xFFFF0000)
            )

            2 -> Pair(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6B7280),
                        Color(0xFF6B7280)
                    )
                ),
                Color(0xFFFFFFFF)
            )

            else -> Pair(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF9A3412),
                        Color(0xFF9A3412)
                    )
                ),
                Color(0xFFFF8000)
            )
        }

        Box(
            modifier = Modifier.width(54.dp),
            contentAlignment = Alignment.Center
        ) {
            // Border
            Text(
                text = rank.toString(),
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                style = TextStyle(
                    color = strokeColor,
                    drawStyle = Stroke(width = 1f)
                )
            )

            // Gradient fill
            Text(
                text = rank.toString(),
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                style = TextStyle(
                    brush = textBrush
                )
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(CardPanel)
                .border(1.dp, brush = Brush.linearGradient(
                    colors = listOf(Color(0x6BFF0000),
                        Color(0x6BF4BE4E)
                    )
                ), RoundedCornerShape(12.dp))
        ) {
            NetworkDramaImage(item.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, item.title)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))))
            CornerBadge("TOP $rank", Color(0xFF374151), icon = null, modifier =  Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 40.dp))
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            ) {
                Text(item.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
                Text("${item.genre} - ${item.episodeTotal} Eps", color = Color(0xFFD1D5DB), fontSize = 10.sp, maxLines = 1, letterSpacing = 0.sp)
            }
            Text(item.rating, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp), letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun RankingListRow(rank: Int, item: DramaItem, onOpenShorts: (Int?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CardPanel)
            .clickable { onOpenShorts(item.id.takeIf { it != 0 }) }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(rank.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(34.dp), letterSpacing = 0.sp)
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Panel)
        ) {
            NetworkDramaImage(item.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, item.title)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            Text("${item.genre} - Ep ${item.episodeTotal}", color = Color(0xFF9CA3AF), fontSize = 10.sp, maxLines = 1, letterSpacing = 0.sp)
        }
        Text(item.rating, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
    }
}

@Composable
private fun ToolPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF1F1F1F))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
    }
}

@Composable
fun CornerBadge(
    text: String,
    color: Color,
    @DrawableRes icon: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Image(
                painter = painterResource(it),
                contentDescription = null,
                modifier = Modifier.size(10.dp)
            )
        }

        Spacer(modifier = Modifier.width(3.dp))

        Text(
            text = text,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun CategoryFilterSheet(
    selectedAudience: AudienceFilter,
    selectedCategory: CategoryFilter,
    onDismiss: () -> Unit,
    onApply: (AudienceFilter, CategoryFilter) -> Unit
) {
    var draftAudience by remember(selectedAudience) { mutableStateOf(selectedAudience) }
    var draftCategory by remember(selectedCategory) { mutableStateOf(selectedCategory) }

    CategoryModalScaffold(
        title = "Filters",
        onDismiss = onDismiss,
        actionLabel = "Apply Filters",
        onApply = { onApply(draftAudience, draftCategory) }
    ) {
        SheetCaption("AUDIENCE")
        SheetChipRow(AudienceFilter.values().toList(), draftAudience, onSelected = { draftAudience = it }) { it.label }
        Spacer(Modifier.height(18.dp))
        SheetCaption("CATEGORY")
        SheetChipRows(CategoryFilter.values().toList(), draftCategory, onSelected = { draftCategory = it }) { it.label }
    }
}

@Composable
private fun CategorySortSheet(
    selectedSort: CategorySort,
    onDismiss: () -> Unit,
    onApply: (CategorySort) -> Unit
) {
    var draftSort by remember(selectedSort) { mutableStateOf(selectedSort) }

    CategoryModalScaffold(
        title = "Sort By",
        onDismiss = onDismiss,
        actionLabel = "Apply Sort",
        onApply = { onApply(draftSort) }
    ) {
        CategorySort.values().forEach { sort ->
            SortOptionRow(
                label = sort.label,
                selected = draftSort == sort,
                onClick = { draftSort = sort }
            )
        }
    }
}

@Composable
private fun CategoryModalScaffold(
    title: String,
    onDismiss: () -> Unit,
    actionLabel: String,
    onApply: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB8000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF130D10))
                .border(1.dp, Color(0xFF5A3740), RoundedCornerShape(18.dp))
                .clickable(enabled = true, onClick = {})
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "x",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    letterSpacing = 0.sp
                )
            }
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFF2354A))
                    .clickable(onClick = onApply),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(actionLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("v", color = Color(0xFFF2354A), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                }
            }
        }
    }
}

@Composable
private fun SheetCaption(label: String) {
    Text(label, color = Color(0xFFE9C9CE), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun <T> SheetChipRow(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            FilterChoiceChip(
                label = label(option),
                selected = selected == option,
                onClick = { onSelected(option) }
            )
        }
    }
}

@Composable
private fun <T> SheetChipRows(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(3).forEach { rowOptions ->
            SheetChipRow(rowOptions, selected, onSelected, label)
        }
    }
}

@Composable
private fun FilterChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0xFFF2354A) else Color(0xFF202323))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
    }
}

@Composable
private fun SortOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF21191D) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (label) {
                "Newest" -> Icons.Filled.Star
                "Popular" -> Icons.Filled.Bookmark
                "Rating" -> Icons.Filled.Star
                else -> Icons.AutoMirrored.Filled.Sort
            },
            contentDescription = null,
            tint = if (selected) Pink else Color(0xFFE9C9CE),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color(0xFFEEDFE4), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.dp, if (selected) Pink else Color(0xFF4A444A), CircleShape)
                .background(if (selected) Pink else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text("v", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
        }
    }
}

@Composable
private fun ContinueWatching(items: List<ContinueWatchingItem>, onOpenShorts: (Int?) -> Unit) {
    if(!items.isEmpty()){
        SectionHeader(title = stringResource(R.string.continue_watching), action = stringResource(R.string.see_all))
    }

//    if (items.isEmpty()) {
//        EmptyContinueWatching(onOpenShorts)
//        return
//    }
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
private fun ActionCards(onRewards: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        SmallActionCard(stringResource(R.string.vip_short), stringResource(R.string.join_vip_club), stringResource(R.string.unlock_all_episodes), Modifier.fillMaxWidth(), onRewards)
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
    onProfile:()->Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(Color(0xF20B0B0E))
            .border(1.dp, Color(0xFF211B22))
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(Icons.Filled.Home, stringResource(R.string.nav_home), selected == "Home", onHome)
        NavItem(Icons.Filled.Explore, stringResource(R.string.nav_shorts), selected == "Shorts", onShorts)
        NavItem(Icons.Filled.VideoLibrary, stringResource(R.string.nav_library), selected == "Library", onLibrary)
        NavItem(Icons.Filled.Person, stringResource(R.string.nav_profile), selected == "Profile", onProfile)
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
private fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        Spacer(modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                color = SoftPink,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                modifier = if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier
            )
        }
    }
}

@Composable
private fun WatchButton(fullWidth: Boolean = false, width: Int = 140, height: Int = 50, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier.width(width.dp))
            .height(height.dp)
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
private fun PlusButton(saved: Boolean, size: Int = 50, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (saved) Color(0x33F5C65B) else Color(0xCC17171B))
            .border(1.dp, if (saved) Gold else Color(0xFF343139), RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (saved) {
            Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Gold, modifier = Modifier.size((size * 0.44f).dp))
        } else {
            Text("+", color = Color.White, fontSize = (size * 0.5f).sp, fontWeight = FontWeight.Light, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun EmptyContinueWatching(onOpenShorts: (Int?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF20161B), Color(0xFF131316))))
            .border(1.dp, Color(0xFF2B252B), RoundedCornerShape(12.dp))
            .clickable { onOpenShorts(null) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFF5168)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF250B12), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Watch something", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Text("Start a drama and it will appear here.", color = Color(0xFFC7B6BC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        }
        Text("START", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
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
                .background(Brush.linearGradient(colors = listOf(
                    Color(0x6BFF0000),
                    Color(0x6BF4BE4E)
                ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite))
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
