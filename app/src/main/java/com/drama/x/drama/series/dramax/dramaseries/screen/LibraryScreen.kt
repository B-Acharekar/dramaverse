package com.drama.x.drama.series.dramax.dramaseries.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.data.ContinueWatchingItem
import com.drama.x.drama.series.dramax.dramaseries.data.DramaItem
import com.drama.x.drama.series.dramax.dramaseries.data.LibraryFeed
import com.drama.x.drama.series.dramax.dramaseries.data.TopStar
import com.drama.x.drama.series.dramax.dramaseries.model.LibraryViewModel

private val HomeBackground = Color(0xFF09090B)
private val Panel = Color(0xFF151318)
private val Pink = Color(0xFFFF3E68)
private val SoftPink = Color(0xFFFFC0C9)
private val Gold = Color(0xFFF5C65B)

@Composable
fun LibraryScreen(
    backendBaseUrl: String,
    onHome: () -> Unit,
    onShorts: () -> Unit,
    onOpenShorts: (Int?) -> Unit,
    onSearch: (String) -> Unit,
    onRewards: () -> Unit,
    onPlanner: () -> Unit,
    onProfile:() -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(backendBaseUrl) {
        viewModel.loadLibrary(backendBaseUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        val feed = uiState.feed
        if (feed == null && uiState.isLoading) {
            LibrarySkeleton()
        } else {
            LibraryContent(
                feed = feed ?: LibraryFeed(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                errorMessage = uiState.errorMessage,
                onSearch = onSearch,
                onOpenShorts = onOpenShorts,
                onPlanner = onPlanner
            )
        }
        BottomNavigationBar(
            selected = "Library",
            onHome = onHome,
            onShorts = onShorts,
            onLibrary = {},
            onRewards = onRewards,
            onProfile = onProfile,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun LibraryContent(
    feed: LibraryFeed,
    errorMessage: String?,
    onSearch: (String) -> Unit,
    onOpenShorts: (Int?) -> Unit,
    onPlanner: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { LibraryTopHeader(onSearch) }
        if (errorMessage != null) {
            item {
                Text(
                    stringResource(R.string.library_load_error),
                    color = SoftPink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }
        }
        feed.watchHistory.firstOrNull()?.let { continueItem ->
            item { FeaturedContinueCard(continueItem, onOpenShorts) }
        }
        item {
            AnimatedLibrarySection {
                LibraryFilmRail(
                    title = stringResource(R.string.watch_list),
                    subtitle = stringResource(R.string.library_watch_list_subtitle),
                    items = feed.watchList,
                    emptyText = stringResource(R.string.library_watch_list_empty),
                    actionLabel = "Schedule",
                    onAction = onPlanner,
                    onOpenShorts = onOpenShorts
                )
            }
        }
        if (feed.watchHistory.size > 1) {
            item {
                AnimatedLibrarySection {
                    WatchHistorySection(
                        title = stringResource(R.string.continue_watching),
                        items = feed.watchHistory.drop(1),
                        onOpenShorts = onOpenShorts
                    )
                }
            }
        }
        item {
            AnimatedLibrarySection {
                LibraryGridSection(
                    title = stringResource(R.string.similar_films),
                    items = feed.similarFilms,
                    onOpenShorts = onOpenShorts
                )
            }
        }
        if (feed.topStars.isNotEmpty()) {
            item { AnimatedLibrarySection { TopStarsSection(feed.topStars, onOpenShorts) } }
        }
        item {
            AnimatedLibrarySection {
                LibraryGridSection(
                    title = stringResource(R.string.recommended_for_you),
                    items = feed.recommended,
                    onOpenShorts = onOpenShorts
                )
            }
        }
    }
}

@Composable
private fun LibraryTopHeader(onSearch: (String) -> Unit) {
    var searchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    fun submitSearch() {
        val query = searchText.trim()
        if (query.isNotBlank()) {
            focusManager.clearFocus()
            onSearch(query)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050506), Color(0xF309090B), Color(0x8809090B), Color.Transparent)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(start = 18.dp, end = 18.dp, top = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.nav_library), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Text(stringResource(R.string.library_subtitle), color = Color(0xFFCDB5BC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xD017151A))
                .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFF2E3E7), modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(9.dp))
            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (searchText.isBlank()) {
                            Text(stringResource(R.string.search_films_hint), color = Color(0xFF9B858E), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun AnimatedLibrarySection(content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })
    ) {
        Column { content() }
    }
}

@Composable
private fun FeaturedContinueCard(item: ContinueWatchingItem, onOpenShorts: (Int?) -> Unit) {
    val film = item.film
    Box(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .height(188.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(16.dp))
            .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
    ) {
        NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x22000000), Color(0x66000000), Color(0xE809090B))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Gold, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(R.string.episode_progress, item.episodeNumber, film.episodeTotal.coerceAtLeast(item.episodeNumber)),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                film.title,
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(9.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x55FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progressFraction.takeIf { it > 0f } ?: 0.05f)
                        .height(4.dp)
                        .background(Pink, RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun LibraryFilmRail(
    title: String,
    subtitle: String,
    items: List<DramaItem>,
    emptyText: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onOpenShorts: (Int?) -> Unit
) {
    Box(Modifier.padding(horizontal = 18.dp)) {
        Column {
            LibraryHeader(title, subtitle, actionLabel, onAction)
        }
    }
    if (items.isEmpty()) {
        Box(Modifier.padding(horizontal = 18.dp)) { EmptyLibraryBlock(emptyText) }
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { film ->
            CompactLibraryCard(film = film, onOpenShorts = onOpenShorts)
        }
    }
}

@Composable
private fun WatchHistorySection(
    title: String,
    items: List<ContinueWatchingItem>,
    onOpenShorts: (Int?) -> Unit
) {
    Box(Modifier.padding(horizontal = 18.dp)) {
        Column { LibraryHeader(title, stringResource(R.string.continue_watching_subtitle)) }
    }
    if (items.isEmpty()) {
        Box(Modifier.padding(horizontal = 18.dp)) {
            EmptyLibraryBlock(stringResource(R.string.continue_watching_empty))
        }
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            HistoryCard(item = item, onOpenShorts = onOpenShorts)
        }
    }
}

@Composable
private fun LibraryGridSection(
    title: String,
    items: List<DramaItem>,
    onOpenShorts: (Int?) -> Unit
) {
    Box(Modifier.padding(horizontal = 18.dp)) {
        Column { LibraryHeader(title, null) }
    }
    if (items.isEmpty()) {
        Box(Modifier.padding(horizontal = 18.dp)) { EmptyLibraryBlock(stringResource(R.string.no_films_yet)) }
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items.take(12)) { film ->
            CompactLibraryCard(film = film, onOpenShorts = onOpenShorts)
        }
    }
}

@Composable
private fun TopStarsSection(stars: List<TopStar>, onOpenShorts: (Int?) -> Unit) {
    Box(Modifier.padding(horizontal = 18.dp)) {
        Column { LibraryHeader(stringResource(R.string.top_stars), stringResource(R.string.top_stars_subtitle)) }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(stars) { star ->
            Column(
                modifier = Modifier
                    .width(82.dp)
                    .clickable { onOpenShorts(star.filmId.takeIf { it != 0 }) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(35.dp))
                        .background(Brush.radialGradient(listOf(Color(0xFFF0B18B), Color(0xFF351B1F)))),
                    contentAlignment = Alignment.Center
                ) {
                    if (star.imageUrl.isNotBlank()) {
                        NetworkDramaImage(star.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, star.name)
                    } else {
                        Text(star.name.take(1).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
                Text(star.name, color = Color(0xFFE8D5DA), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    title: String,
    subtitle: String?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            if (subtitle != null) {
                Text(subtitle, color = Color(0xFFBBA3AB), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                color = Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x18F5C65B))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun CompactLibraryCard(film: DramaItem, onOpenShorts: (Int?) -> Unit) {
    Column(
        modifier = Modifier
            .width(138.dp)
            .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
    ) {
        PosterBox(film, Modifier.fillMaxWidth().aspectRatio(0.72f))
        Spacer(modifier = Modifier.height(8.dp))
        Text(film.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
    }
}

@Composable
private fun HistoryCard(item: ContinueWatchingItem, onOpenShorts: (Int?) -> Unit) {
    val film = item.film
    Column(
        modifier = Modifier
            .width(210.dp)
            .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
    ) {
        Box {
            PosterBox(film, Modifier.fillMaxWidth().height(118.dp))
            Text(
                stringResource(R.string.episode_progress, item.episodeNumber, film.episodeTotal.coerceAtLeast(item.episodeNumber)),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                letterSpacing = 0.sp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(item.progressFraction.takeIf { it > 0f } ?: 0.04f)
                    .height(4.dp)
                    .background(Pink)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(film.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
    }
}

@Composable
private fun LargeLibraryCard(film: DramaItem, onOpenShorts: (Int?) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(238.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
    ) {
        NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD0050507))))
        )
        Text(
            film.genre.uppercase().take(14),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xBBFF3E68))
                .padding(horizontal = 9.dp, vertical = 4.dp),
            letterSpacing = 0.sp
        )
        Text(
            film.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 12.dp, bottom = 16.dp),
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun PosterBox(film: DramaItem, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
    ) {
        NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA050507))))
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(film.rating, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun EmptyLibraryBlock(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF151318))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Color(0xFF8E7880), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = Color(0xFFC7B6BC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    }
}

@Composable
private fun LibrarySkeleton() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 48.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFF17161A))) }
        items(5) {
            Box(Modifier.fillMaxWidth().height(138.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF151318)))
        }
    }
}
