package app.dramaverse.stream.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.R
import app.dramaverse.stream.data.DramaItem
import app.dramaverse.stream.model.SearchViewModel

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
    var searchText by remember(query) { mutableStateOf(query) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(backendBaseUrl, query) {
        viewModel.search(backendBaseUrl, query)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchHeader(
                query = searchText,
                onQueryChange = { searchText = it },
                onBack = onBack,
                onSubmit = {
                    val next = searchText.trim()
                    if (next.isNotBlank()) {
                        focusManager.clearFocus()
                        onSearch(next)
                    }
                }
            )
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
            ) {
                Text(
                    stringResource(R.string.search_results_count, uiState.results.size),
                    color = Color(0xFFCDB5BC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    letterSpacing = 0.sp
                )
            }
            if (uiState.errorMessage != null) {
                Text(
                    stringResource(R.string.search_error),
                    color = Color(0xFFFFC0C9),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    letterSpacing = 0.sp
                )
            }
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFFC0C9))
                }
            } else if (uiState.results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_films_found), color = Color(0xFFCDB5BC), fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(uiState.results) { film ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 })
                        ) {
                            SearchFilmCard(film, onOpenShorts)
                        }
                    }
                }
            }
        }
        BottomNavigationBar(
            selected = "Search",
            onHome = onHome,
            onShorts = onShorts,
            onLibrary = onLibrary,
            onRewards = onRewards,
            onProfile = onProfile,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(102.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF050506), Color(0xE909090B), Color.Transparent)))
            .padding(start = 14.dp, end = 18.dp, top = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x6618171C))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xD017151A))
                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = Color(0xFFF2E3E7),
                modifier = Modifier
                    .size(19.dp)
                    .clickable(onClick = onSubmit)
            )
            Spacer(modifier = Modifier.width(9.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) {
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
private fun SearchFilmCard(film: DramaItem, onOpenShorts: (Int?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenShorts(film.id.takeIf { it != 0 }) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF151318))
                .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(14.dp))
        ) {
            NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC050507))))
            )
            Text(
                film.genre.uppercase().take(14),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xBBFF3E68))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                letterSpacing = 0.sp
            )
            Text(
                film.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 10.dp, bottom = 13.dp),
                letterSpacing = 0.sp
            )
        }
    }
}
