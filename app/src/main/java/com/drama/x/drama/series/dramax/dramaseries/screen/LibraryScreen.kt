package com.drama.x.drama.series.dramax.dramaseries.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.ads.NativeAdState
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

private enum class MyListMode { Overview, History, Favorites }

@Composable
fun LibraryScreen(
    backendBaseUrl: String,
    onHome: () -> Unit,
    onShorts: () -> Unit,
    onOpenShorts: (Int?) -> Unit,
    onSearch: (String) -> Unit = {},
    onRewards: () -> Unit,
    onPlanner: () -> Unit,
    onProfile:() -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findLibraryActivity() }
    val bottomBannerVisible = shouldShowAppBottomBanner()
    val bottomBannerPadding = if (bottomBannerVisible) AppBottomBannerHeight else 0.dp
    var nativeMyListAdState by remember { mutableStateOf<NativeAdState>(NativeAdState.Idle) }

    LaunchedEffect(backendBaseUrl) {
        viewModel.loadLibrary(backendBaseUrl)
    }

    LaunchedEffect(activity) {
        activity?.let { AdsManager.loadNativeMyList(it) }
    }

    DisposableEffect(Unit) {
        val observer = Observer<NativeAdState> { nativeMyListAdState = it }
        AdsManager.nativeMyListAdLive.observeForever(observer)
        onDispose {
            AdsManager.nativeMyListAdLive.removeObserver(observer)
        }
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
                backendBaseUrl = backendBaseUrl,
                viewModel = viewModel,
                onSearch = onSearch,
                onOpenShorts = onOpenShorts,
                nativeMyListAdState = nativeMyListAdState,
                bottomBannerVisible = bottomBannerVisible,
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomBannerPadding)
        )
        if (bottomBannerVisible) {
            AppBottomBanner(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun LibraryContent(
    feed: LibraryFeed,
    errorMessage: String?,
    backendBaseUrl: String,
    viewModel: LibraryViewModel,
    onSearch: (String) -> Unit = {},
    onOpenShorts: (Int?) -> Unit,
    nativeMyListAdState: NativeAdState,
    bottomBannerVisible: Boolean,
    onPlanner: () -> Unit
) {
    var mode by remember { mutableStateOf(MyListMode.Overview) }
    val history = feed.watchHistory
    val favorites = feed.watchList
    // Checkbox selection state for History and Favorites modes
    var selectedHistoryIds by remember { mutableStateOf(emptySet<Int>()) }
    var selectedFavoriteIds by remember { mutableStateOf(emptySet<Int>()) }
    // Long-press to enter selection mode
    var isHistorySelectionMode by remember { mutableStateOf(false) }
    var isFavoritesSelectionMode by remember { mutableStateOf(false) }
    // Reset selection and mode whenever screen mode changes
    androidx.compose.runtime.LaunchedEffect(mode) {
        selectedHistoryIds = emptySet()
        selectedFavoriteIds = emptySet()
        isHistorySelectionMode = false
        isFavoritesSelectionMode = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 104.dp + if (bottomBannerVisible) AppBottomBannerHeight else 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (mode == MyListMode.Overview) {
            item { LibraryTopHeader(onSearchClick = onSearch) }
        }
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
        when (mode) {
            MyListMode.Overview -> {
                // Only show History watching section if there are items
                if (history.isNotEmpty()) {
                    item {
                        MyListSectionHeader(
                            title = stringResource(R.string.history_watching),
                            action = stringResource(R.string.see_all),
                            onAction = { mode = MyListMode.History }
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(history.take(8)) { item ->
                                MyListHistoryPreviewCard(item = item, onOpenShorts = onOpenShorts)
                            }
                        }
                    }
                }

                item {
                    MyListSectionHeader(
                        title = stringResource(R.string.my_favorites),
                        action = stringResource(R.string.see_all).takeIf { favorites.isNotEmpty() },
                        onAction = { mode = MyListMode.Favorites }
                    )
                }
                if (favorites.isEmpty()) {
                    item {
                        MyListEmptyMessage(
                            title = stringResource(R.string.no_favorites_yet),
                            body = stringResource(R.string.favorites_empty_message)
                        )
                    }
                } else {
                    favorites.forEachIndexed { index, film ->
                        if (index % 3 == 0) {
                            item {
                                MyListNativeAd(
                                    state = nativeMyListAdState,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                            }
                        }
                        item {
                            FavoriteListCard(film = film, onOpenShorts = onOpenShorts)
                        }
                    }
                }
            }

            MyListMode.History -> {
                item {
                    Spacer(modifier = Modifier.statusBarsPadding())
                }
                item {
                    val allSelected = history.isNotEmpty() && selectedHistoryIds.containsAll(history.map { it.film.id }.toSet())
                    HistoryAllHeader(
                        title = stringResource(R.string.history_watching),
                        meta = stringResource(R.string.items_watched,history.size),
                        allSelected = allSelected,
                        selectionMode = isHistorySelectionMode,
                        onBack = { mode = MyListMode.Overview },
                        onSelectAll = {
                            isHistorySelectionMode = true
                            selectedHistoryIds = if (allSelected) emptySet()
                            else history.map { it.film.id }.toSet()
                        },
                        onDelete = {
                            if (selectedHistoryIds.isNotEmpty()) {
                                viewModel.removeHistoryItems(backendBaseUrl, selectedHistoryIds)
                                selectedHistoryIds = emptySet()
                                isHistorySelectionMode = false
                            }
                        }
                    )
                }
                if (history.isEmpty()) {
                    item {
                        MyListEmptyMessage(
                            title = stringResource(R.string.nothing_watched_yet),
                            body = stringResource(R.string.nothing_watched_yet_desc)
                        )
                    }
                } else {
                    // Build rows of 3 items, inserting native ad every 3 rows (every 9 items)
                    val chunked = history.chunked(3)
                    chunked.forEachIndexed { rowIndex, rowItems ->
                        // Native ad every 3 rows (before row 0, 3, 6, ...)
                        if (rowIndex % 3 == 0) {
                            item {
                                MyListNativeAd(
                                    state = nativeMyListAdState,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                        item(key = "hist_row_$rowIndex") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { histItem ->
                                    val filmId = histItem.film.id
                                    val isChecked = filmId in selectedHistoryIds
                                    HistoryGridCard(
                                        item = histItem,
                                        isChecked = isChecked,
                                        selectionMode = isHistorySelectionMode,
                                        onLongPress = {
                                            isHistorySelectionMode = true
                                            selectedHistoryIds = selectedHistoryIds + filmId
                                        },
                                        onToggleCheck = {
                                            selectedHistoryIds = if (isChecked)
                                                selectedHistoryIds - filmId
                                            else
                                                selectedHistoryIds + filmId
                                        },
                                        onOpen = { if (!isHistorySelectionMode) onOpenShorts(filmId.takeIf { it != 0 }) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill empty slots in last row
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            MyListMode.Favorites -> {
                item {
                    Spacer(modifier = Modifier.statusBarsPadding())
                }
                item {
                    val allSelected = favorites.isNotEmpty() && selectedFavoriteIds.containsAll(favorites.map { it.id }.toSet())
                    HistoryAllHeader(
                        title = stringResource(R.string.my_favorites),
                        meta = stringResource(R.string.favorites_size,favorites.size),
                        allSelected = allSelected,
                        selectionMode = isFavoritesSelectionMode,
                        onBack = { mode = MyListMode.Overview },
                        onSelectAll = {
                            isFavoritesSelectionMode = true
                            selectedFavoriteIds = if (allSelected) emptySet()
                            else favorites.map { it.id }.toSet()
                        },
                        onDelete = {
                            if (selectedFavoriteIds.isNotEmpty()) {
                                viewModel.removeFavoriteItems(backendBaseUrl, selectedFavoriteIds)
                                selectedFavoriteIds = emptySet()
                                isFavoritesSelectionMode = false
                            }
                        }
                    )
                }
                if (favorites.isEmpty()) {
                    item {
                        MyListEmptyMessage(
                            title = stringResource(R.string.no_favorites_yet),
                            body = stringResource(R.string.favorites_empty_message)
                        )
                    }
                } else {
                    favorites.forEachIndexed { index, film ->
                        if (index % 3 == 0) {
                            item {
                                MyListNativeAd(
                                    state = nativeMyListAdState,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                            }
                        }
                        item(key = "fav_${film.id}") {
                            val isChecked = film.id in selectedFavoriteIds
                            FavoriteListCard(
                                film = film,
                                onOpenShorts = { if (!isFavoritesSelectionMode) onOpenShorts(it) },
                                isChecked = isChecked,
                                selectionMode = isFavoritesSelectionMode,
                                onLongPress = {
                                    isFavoritesSelectionMode = true
                                    selectedFavoriteIds = selectedFavoriteIds + film.id
                                },
                                onToggleCheck = {
                                    selectedFavoriteIds = if (isChecked)
                                        selectedFavoriteIds - film.id
                                    else
                                        selectedFavoriteIds + film.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyListNativeAd(
    state: NativeAdState,
    modifier: Modifier = Modifier
) {
    ErainNativeAdHost(
        placementName = "native_my_list",
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        height = 104.dp
    )
}

@Composable
private fun LibraryTopHeader(onSearchClick: (String) -> Unit = {},
                              modifier: Modifier = Modifier) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    DramaXTopAppBar(
        topInset = topInset,
        showActions = false,
        modifier = modifier
    )
}

@Composable
private fun MyListSectionHeader(title: String, action: String?, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        Spacer(modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
private fun MyListAllHeader(
    title: String,
    meta: String,
    action: String?,
    onBack: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onBack)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                modifier = Modifier.weight(1f)
            )
            if (onDelete != null) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete all",
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onDelete)
                )
            } else {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(meta, color = Color(0xFFCDB5BC), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            Spacer(modifier = Modifier.weight(1f))
            if (action != null) {
                Text(action, color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
        }
    }
}

@Composable
private fun HistoryAllHeader(
    title: String,
    meta: String,
    allSelected: Boolean,
    selectionMode: Boolean = false,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onBack)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                modifier = Modifier.weight(1f)
            )
            if (selectionMode) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete selected",
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onDelete)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(meta, color = Color(0xFFCDB5BC), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            Spacer(modifier = Modifier.weight(1f))
            if (selectionMode) {
                Text(
                    text = if (allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                    color = Gold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp,
                    modifier = Modifier.clickable(onClick = onSelectAll)
                )
            } else {
                Text(
                    text = stringResource(R.string.long_press_to_select),
                    color = Color(0xFF6B6B6F),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun HistoryGridCard(
    item: ContinueWatchingItem,
    isChecked: Boolean,
    selectionMode: Boolean = false,
    onLongPress: () -> Unit = {},
    onToggleCheck: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val film = item.film
    Column(
        modifier = modifier
            .pointerInput(selectionMode) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onOpen() }
                )
            }
    ) {
        // Poster box with checkbox overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.667f)  // ~2:3 portrait ratio
                .clip(RoundedCornerShape(8.dp))
                .background(Panel)
        ) {
            NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
            // Dark overlay
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xC009090B)))))
            // Episode progress pill (bottom-right)
            ProgressPill(
                text = stringResource(R.string.episode_progress, item.episodeNumber, film.episodeTotal.coerceAtLeast(item.episodeNumber)),
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
            )
            // Progress bar (bottom)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(item.progressFraction.takeIf { it > 0f } ?: 0.04f)
                    .height(3.dp)
                    .background(Pink)
            )
            // Checkbox tap zone - only shown in selection mode
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(32.dp)
                        .clickable(onClick = onToggleCheck),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isChecked) Pink else Color(0x44FFFFFF))
                            .border(1.dp, if (isChecked) Pink else Color(0x66FFFFFF), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            film.title,
            color = Color.White,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Text(
            "Ep ${item.episodeNumber} of ${film.episodeTotal.coerceAtLeast(item.episodeNumber)}",
            color = Color(0xFFCDB5BC),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun FavoriteGridCard(
    film: DramaItem,
    isChecked: Boolean,
    onToggleCheck: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onOpen)
    ) {
        // Poster box with checkbox overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.667f)
                .clip(RoundedCornerShape(8.dp))
                .background(Panel)
        ) {
            NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xC009090B)))))
            // Episode count pill (bottom-right)
            ProgressPill(
                text = "${film.episodeTotal.coerceAtLeast(1)} Eps",
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
            )
            // Checkbox tap zone (top-left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(32.dp)
                    .clickable(onClick = onToggleCheck),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isChecked) Pink else Color(0x44FFFFFF))
                        .border(1.dp, if (isChecked) Pink else Color(0x66FFFFFF), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            film.title,
            color = Color.White,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Text(
            "${film.episodeTotal.coerceAtLeast(1)} Episodes",
            color = Color(0xFFCDB5BC),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun MyListHistoryPreviewCard(item: ContinueWatchingItem, onOpenShorts: (Int?) -> Unit) {
    val film = item.film
    Column(
        modifier = Modifier
            .width(112.dp)
            .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Panel)
        ) {
            NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xC909090B)))))
            ProgressPill(
                text = stringResource(R.string.episode_progress, item.episodeNumber, film.episodeTotal.coerceAtLeast(item.episodeNumber)),
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(item.progressFraction.takeIf { it > 0f } ?: 0.04f)
                    .height(3.dp)
                    .background(Pink)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(film.title, color = Color.White, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
        Text("Ep ${item.episodeNumber} of ${film.episodeTotal.coerceAtLeast(item.episodeNumber)}", color = Color(0xFFCDB5BC), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    }
}

@Composable
private fun MyListHistoryGridCard(
    item: ContinueWatchingItem,
    onOpenShorts: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val film = item.film
    val context = LocalContext.current
    val progressPercent = ((item.progressFraction * 100).toInt()).coerceIn(0, 100)
    
    Row(
        modifier = modifier
            .wrapContentHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1F1F1F))
            .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            NetworkDramaImage(
                imageUrl = film.imageUrl,
                modifier = Modifier
                    .width(70.dp)
                    .height(92.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Panel),
                contentScale = ContentScale.Crop,
                seed = film.title
            )
            // Progress indicator at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(item.progressFraction.takeIf { it > 0f } ?: 0.04f)
                    .height(3.dp)
                    .background(Pink)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(film.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(film.genre.ifBlank { "Drama" }, color = Color(0xFFCDB5BC), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            Text(
                "Ep ${item.episodeNumber} of ${film.episodeTotal.coerceAtLeast(item.episodeNumber)} • $progressPercent%",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Pink)
                    .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Continue", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            Icons.Filled.Share,
            contentDescription = "Share",
            tint = Color(0xFFBCAFB4),
            modifier = Modifier
                .size(18.dp)
                .clickable {
                    shareFilm(context, film)
                }
        )
    }
}

@Composable
private fun FavoriteListCard(
    film: DramaItem,
    onOpenShorts: (Int?) -> Unit,
    isChecked: Boolean = false,
    selectionMode: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onToggleCheck: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 3.dp)
            .wrapContentHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1F1F1F))
            .then(
                if (onLongPress != null) {
                    Modifier.pointerInput(selectionMode) {
                        detectTapGestures(
                            onLongPress = { onLongPress() },
                            onTap = { onOpenShorts(film.id.takeIf { it != 0 }) }
                        )
                    }
                } else {
                    Modifier.clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
                }
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(92.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Panel)
        ) {
            NetworkDramaImage(
                imageUrl = film.imageUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                seed = film.title
            )
            if (selectionMode && onToggleCheck != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(28.dp)
                        .clickable(onClick = onToggleCheck),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isChecked) Pink else Color(0x66000000))
                            .border(1.dp, if (isChecked) Pink else Color(0x66FFFFFF), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(film.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(film.genre.ifBlank { "Drama" }, color = Color(0xFFCDB5BC), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            Text("${film.episodeTotal.coerceAtLeast(1)} Episodes", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Pink)
                    .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            Icons.Filled.Share,
            contentDescription = "Share",
            tint = Color(0xFFBCAFB4),
            modifier = Modifier
                .size(18.dp)
                .clickable {
                    shareFilm(context, film)
                }
        )
    }
}

@Composable
private fun ProgressPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color.White,
        fontSize = 7.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xB0000000))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
private fun MyListEmptyMessage(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, letterSpacing = 0.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(body, color = Color(0xFFCDB5BC), fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center, letterSpacing = 0.sp)
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

private fun shareFilm(context: Context, film: DramaItem) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, film.title)
        putExtra(android.content.Intent.EXTRA_TEXT, "Check out ${film.title} on DramaVerse!")
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
}

private tailrec fun Context.findLibraryActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findLibraryActivity()
    else -> null
}
