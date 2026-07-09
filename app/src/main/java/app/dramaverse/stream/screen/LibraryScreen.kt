package app.dramaverse.stream.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.data.ContinueWatchingItem
import app.dramaverse.stream.data.DramaItem
import app.dramaverse.stream.data.LibraryFeed
import app.dramaverse.stream.model.LibraryViewModel

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
                onOpenShorts = onOpenShorts
            )
        }
        BottomNavigationBar(
            selected = "Library",
            onHome = onHome,
            onShorts = onShorts,
            onLibrary = {},
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun LibraryContent(
    feed: LibraryFeed,
    errorMessage: String?,
    onOpenShorts: (Int?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 48.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item { LibrarySearchBar() }
        item { LibraryChips() }
        if (errorMessage != null) {
            item { Text(errorMessage, color = SoftPink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }
        item {
            LibraryFilmRail(
                title = "Watch List",
                subtitle = "Saved and bookmarked films",
                items = feed.watchList,
                emptyText = "Saved films will appear here.",
                onOpenShorts = onOpenShorts
            )
        }
        item {
            WatchHistorySection(
                items = feed.watchHistory,
                onOpenShorts = onOpenShorts
            )
        }
        item {
            LibraryFilmRail(
                title = "Followed Films",
                subtitle = "Films you follow",
                items = feed.followedFilms,
                emptyText = "Followed films will appear here.",
                onOpenShorts = onOpenShorts
            )
        }
        item {
            LibraryGridSection(
                title = "Recommended for You",
                items = feed.recommended,
                onOpenShorts = onOpenShorts
            )
        }
        item {
            LibraryGridSection(
                title = "Similar Films",
                items = feed.similarFilms,
                onOpenShorts = onOpenShorts
            )
        }
    }
}

@Composable
private fun LibrarySearchBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF17161A))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFB89BA5), modifier = Modifier.size(25.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            "Find your next obsession...",
            color = Color(0xFF8F767F),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.Mic, contentDescription = null, tint = Color(0xFFB89BA5), modifier = Modifier.size(25.dp))
    }
}

@Composable
private fun LibraryChips() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(listOf("All", "Romance", "CEO", "Mafia")) { chip ->
            val selected = chip == "All"
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected) Color(0xFFFFA6B3) else Color(0xFF1E1D21))
                    .padding(horizontal = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    chip,
                    color = if (selected) Color(0xFF4A1A26) else Color(0xFFE6BDC6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp
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
    onOpenShorts: (Int?) -> Unit
) {
    LibraryHeader(title, subtitle)
    if (items.isEmpty()) {
        EmptyLibraryBlock(emptyText)
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { film ->
            CompactLibraryCard(film = film, onOpenShorts = onOpenShorts)
        }
    }
}

@Composable
private fun WatchHistorySection(
    items: List<ContinueWatchingItem>,
    onOpenShorts: (Int?) -> Unit
) {
    LibraryHeader("Watch History", "Continue from your latest episode")
    if (items.isEmpty()) {
        EmptyLibraryBlock("Films watched for 10 seconds or more will appear here.")
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
    LibraryHeader(title, null)
    if (items.isEmpty()) {
        EmptyLibraryBlock("No films yet.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.height(((items.take(6).size + 1) / 2 * 250).dp)
    ) {
        items(items.take(6)) { film ->
            LargeLibraryCard(film = film, onOpenShorts = onOpenShorts)
        }
    }
}

@Composable
private fun LibraryHeader(title: String, subtitle: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            if (subtitle != null) {
                Text(subtitle, color = Color(0xFFBBA3AB), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            }
        }
        Text("Refresh", color = SoftPink, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun CompactLibraryCard(film: DramaItem, onOpenShorts: (Int?) -> Unit) {
    Column(
        modifier = Modifier
            .width(132.dp)
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
                "Ep ${item.episodeNumber}/${film.episodeTotal.coerceAtLeast(item.episodeNumber)}",
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
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Panel)) {
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
