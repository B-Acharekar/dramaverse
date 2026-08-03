package com.drama.x.drama.series.dramax.dramaseries.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.data.DramaItem
import com.drama.x.drama.series.dramax.dramaseries.model.SearchViewModel

private val SearchBackground = Color(0xFF131315)
private val SearchPanel = Color(0x99201F21)
private val SearchBorder = Color(0x14FFFFFF)
private val SearchPink = Color(0xFFFF5168)
private val SearchSoftPink = Color(0xFFFFB3B6)
private val SearchGold = Color(0xFFF4BE4E)

@Composable
fun SearchResultsScreen(
    backendBaseUrl: String,
    query: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onShorts: () -> Unit,
    onLibrary: () -> Unit,
    onOpenShorts: (Int?) -> Unit,
    onSearch: (String) -> Unit,
    onRewards: () -> Unit,
    onProfile: () -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDefaultSearch = query.isBlank() || query.equals("hot", ignoreCase = true)
    var searchText by remember(query) { mutableStateOf(if (isDefaultSearch) "" else query) }
    val focusManager = LocalFocusManager.current
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerHeight = 80.dp + topInset
    val bottomBannerVisible = shouldShowAppBottomBanner()
    val bottomBannerPadding = if (bottomBannerVisible) AppBottomBannerHeight else 0.dp

    LaunchedEffect(backendBaseUrl, query) {
        viewModel.search(backendBaseUrl, if (isDefaultSearch) "hot" else query)
    }

    fun submitSearch() {
        val next = searchText.trim()
        if (next.isNotBlank()) {
            focusManager.clearFocus()
            onSearch(next)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = headerHeight, bottom = 92.dp + bottomBannerPadding)
        ) {
            when {
                uiState.isLoading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SearchSoftPink)
                    }
                }

                isDefaultSearch -> defaultSearchContent(
                    hotItems = uiState.results.ifEmpty { uiState.recommendations },
                    onSearch = onSearch,
                    onOpenShorts = onOpenShorts
                )

                uiState.results.isEmpty() -> emptySearchContent(
                    query = searchText.ifBlank { query },
                    recommendations = uiState.recommendations,
                    onSearch = onSearch,
                    onOpenShorts = onOpenShorts
                )

                else -> filteredSearchContent(
                    query = searchText.ifBlank { query },
                    results = uiState.results,
                    onOpenShorts = onOpenShorts
                )
            }
        }

        SearchHeader(
            query = searchText,
            topInset = topInset,
            onQueryChange = { searchText = it },
            onClear = {
                if (searchText.isBlank()) {
                    onBack()
                } else {
                    searchText = ""
                }
            },
            onSubmit = ::submitSearch
        )

        BottomNavigationBar(
            selected = "Search",
            onHome = onHome,
            onShorts = onShorts,
            onLibrary = onLibrary,
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

private fun androidx.compose.foundation.lazy.LazyListScope.defaultSearchContent(
    hotItems: List<DramaItem>,
    onSearch: (String) -> Unit,
    onOpenShorts: (Int?) -> Unit
) {
    item {
        val chips = hotItems.searchChips(
            fallback = listOf(
                stringResource(R.string.fallback_genre_romance),
                stringResource(R.string.fallback_genre_drama),
                stringResource(R.string.fallback_genre_thriller)
            )
        )
        SearchSectionTitle(icon = Icons.AutoMirrored.Filled.TrendingUp, title = stringResource(R.string.trending_searches), iconTint = SearchGold)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chips) { chip ->
                SearchChip(chip, onClick = { onSearch(chip) })
            }
        }
        Spacer(Modifier.height(22.dp))
        SearchSectionTitle(icon = Icons.Filled.LocalFireDepartment, title = stringResource(R.string.hot_searches), iconTint = SearchPink)
    }
    items(hotItems.take(8)) { film ->
        SearchResultCard(film = film, query = "", badge = stringResource(R.string.badge_hot), onOpenShorts = onOpenShorts)
    }
    if (hotItems.isEmpty()) {
        item {
            SearchEmptyCatalogMessage()
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.emptySearchContent(
    query: String,
    recommendations: List<DramaItem>,
    onSearch: (String) -> Unit,
    onOpenShorts: (Int?) -> Unit
) {
    item {
        val chips = recommendations.searchChips(
            fallback = listOf(
                stringResource(R.string.fallback_genre_romance),
                stringResource(R.string.fallback_genre_drama),
                stringResource(R.string.fallback_genre_thriller)
            )
        ).take(4)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(166.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2C))
                    .border(1.dp, Color(0x14FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Masks, contentDescription = null, tint = Color(0x55E5E1E4), modifier = Modifier.size(82.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(4.dp, SearchSoftPink, CircleShape)
                        .align(Alignment.Center)
                )
                Text(stringResource(R.string.empty_state_mask_glyph), color = SearchSoftPink, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.no_results_found), color = Color(0xFFE5E1E4), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.no_results_found_desc, query),
                color = Color(0xFFE5BDBE),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(20.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(chips) { chip ->
                    SearchChip(chip, onClick = { onSearch(chip) })
                }
            }
        }
        RecommendedForYou(recommendations.take(2), onSearch, onOpenShorts)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.filteredSearchContent(
    query: String,
    results: List<DramaItem>,
    onOpenShorts: (Int?) -> Unit
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.results_found_for_query, results.size, query),
                color = Color(0xCCE5BDBE),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FilterList, contentDescription = null, tint = SearchSoftPink, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("FILTERS", color = SearchSoftPink, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
            }
        }
    }
    items(results) { film ->
        SearchResultCard(film = film, query = query, badge = if (film.isPremium) stringResource(R.string.badge_new) else stringResource(R.string.badge_hot), onOpenShorts = onOpenShorts)
    }
}

@Composable
private fun SearchHeader(
    query: String,
    topInset: androidx.compose.ui.unit.Dp,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp + topInset)
            .background(Color(0xFF0C0808))
            .border(1.dp, Color(0x18FFFFFF))
            .padding(top = topInset)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF201F21))
                .border(1.dp, Color(0xFF6B7280), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0x99E5BDBE), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color(0xFFE5E1E4), fontSize = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) {
                            Text(stringResource(R.string.search_films_hint), color = Color(0x66E5BDBE), fontSize = 16.sp, letterSpacing = 0.sp)
                        }
                        innerTextField()
                    }
                }
            )
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                tint = Color(0x99E5BDBE),
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onClear)
            )
        }
    }
}

@Composable
private fun SearchSectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, iconTint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, color = Color(0xFFE5E1E4), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    }
}

@Composable
private fun SearchChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x99201F21))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFFE5BDBE), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    }
}

@Composable
private fun SearchResultCard(film: DramaItem, query: String, badge: String, onOpenShorts: (Int?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(154.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SearchPanel)
            .border(1.dp, SearchBorder, RoundedCornerShape(12.dp))
            .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(128.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1F2937))
        ) {
            NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
            Text(
                badge,
                color = if (badge == "NEW") Color.Black else Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                modifier = Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (badge == "NEW") SearchGold else SearchPink)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                highlightedTitle(film.title, query),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                film.description.ifBlank { stringResource(R.string.fallback_description) },
                color = Color(0x99E5BDBE),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(Modifier.height(10.dp))
            GenreTag(film.genre)
        }
    }
}

@Composable
private fun RecommendedForYou(items: List<DramaItem>, onSearch: (String) -> Unit, onOpenShorts: (Int?) -> Unit) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.recommended_for_you), color = Color(0xFFE5E1E4), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.view_all), color = SearchSoftPink, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp, modifier = Modifier.clickable { onSearch("hot") })
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items.forEachIndexed { index, film ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF151318))
                        .border(1.dp, SearchPink, RoundedCornerShape(12.dp))
                        .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
                ) {
                    NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD0131315)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                        Text(if (index == 0) stringResource(R.string.badge_trending_first) else stringResource(R.string.badge_new_release), color = SearchGold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                        Text(film.title, color = Color(0xFFE5E1E4), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyCatalogMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.no_dramas_loaded_title),
            color = Color(0xFFE5E1E4),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.no_dramas_loaded_desc),
            color = Color(0x99E5BDBE),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun GenreTag(genre: String) {
    Text(
        genre.ifBlank { stringResource(R.string.genre_fallback) }.uppercase().take(18),
        color = Color(0xCCB8891A),
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
        modifier = Modifier
            .border(1.dp, Color(0x4DB8891A), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun highlightedTitle(title: String, query: String) = buildAnnotatedString {
    if (query.isBlank()) {
        withStyle(SpanStyle(color = Color(0xFFE5E1E4), fontWeight = FontWeight.Normal)) {
            append(title)
        }
        return@buildAnnotatedString
    }
    val index = title.lowercase().indexOf(query.lowercase())
    if (index < 0) {
        withStyle(SpanStyle(color = Color(0xFFE5E1E4), fontWeight = FontWeight.Normal)) {
            append(title)
        }
        return@buildAnnotatedString
    }
    withStyle(SpanStyle(color = Color(0xFFE5E1E4), fontWeight = FontWeight.Normal)) {
        append(title.take(index))
    }
    withStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.ExtraBold)) {
        append(title.substring(index, index + query.length))
    }
    withStyle(SpanStyle(color = Color(0xFFE5E1E4), fontWeight = FontWeight.Normal)) {
        append(title.drop(index + query.length))
    }
}

private fun List<DramaItem>.searchChips(fallback: List<String>): List<String> {
    return flatMap { item ->
        listOf(item.genre, item.title)
    }
        .map { it.trim() }
        .filter { it.length in 3..18 }
        .distinctBy { it.lowercase() }
        .take(8)
        .ifEmpty { fallback }
}
