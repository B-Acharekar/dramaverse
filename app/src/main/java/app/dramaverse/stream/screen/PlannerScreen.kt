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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.data.DramaItem
import app.dramaverse.stream.data.PlannerItem
import app.dramaverse.stream.model.PlannerViewModel

private val Background = Color(0xFF09090B)
private val Panel = Color(0xFF161519)
private val Pink = Color(0xFFFF3E68)
private val Gold = Color(0xFFF6C54F)
private val SoftPink = Color(0xFFFFB5C1)

@Composable
fun PlannerScreen(
    backendBaseUrl: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onShorts: () -> Unit,
    onLibrary: () -> Unit,
    onRewards: () -> Unit,
    viewModel: PlannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(backendBaseUrl) {
        viewModel.loadPlanner(backendBaseUrl)
    }

    Box(Modifier.fillMaxSize().background(Background)) {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SoftPink)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { PlannerHeader(onBack) }
                item {
                    ScheduleBuilder(
                        selectedFilm = uiState.selectedFilm,
                        hasSavedFilms = uiState.suggestions.isNotEmpty(),
                        selectedEpisode = uiState.selectedEpisode,
                        onEpisodeMinus = { viewModel.changeEpisode(-1) },
                        onEpisodePlus = { viewModel.changeEpisode(1) },
                        onSchedule = { viewModel.scheduleTomorrow(backendBaseUrl) }
                    )
                }
                item { SuggestionRail(uiState.suggestions, uiState.selectedFilm, viewModel::selectFilm, onLibrary) }
                item { PlannedList(uiState.items) }
            }
        }
        BottomNavigationBar(
            selected = "Planner",
            onHome = onHome,
            onShorts = onShorts,
            onLibrary = onLibrary,
            onRewards = onRewards,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PlannerHeader(onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(104.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF050506), Color(0xEE09090B), Color.Transparent)))
            .padding(start = 18.dp, end = 18.dp, top = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x6618171C)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Planner", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Text("Schedule saved dramas and reminders.", color = Color(0xFFCDB5BC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun ScheduleBuilder(
    selectedFilm: DramaItem?,
    hasSavedFilms: Boolean,
    selectedEpisode: Int,
    onEpisodeMinus: () -> Unit,
    onEpisodePlus: () -> Unit,
    onSchedule: () -> Unit
) {
    Row(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1A151B), Color(0xFF111115))))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF231D24))) {
            if (selectedFilm != null) NetworkDramaImage(selectedFilm.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, selectedFilm.title)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(selectedFilm?.title ?: if (hasSavedFilms) "Choose a saved drama" else "Save dramas first", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            Text(
                if (selectedFilm != null) "Tomorrow at 8:00 PM" else "Use + on Home or Shorts to build this list.",
                color = if (selectedFilm != null) Gold else Color(0xFFCDB5BC),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            )
            if (selectedFilm != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton(Icons.Filled.Remove, onEpisodeMinus)
                    Text("Ep $selectedEpisode", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp), letterSpacing = 0.sp)
                    StepButton(Icons.Filled.Add, onEpisodePlus)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Box(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selectedFilm != null) Brush.horizontalGradient(listOf(Color(0xFFFF4D74), Color(0xFFD70B43))) else Brush.horizontalGradient(listOf(Color(0xFF26232A), Color(0xFF26232A))))
            .clickable(enabled = selectedFilm != null, onClick = onSchedule),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (selectedFilm != null) "Schedule Reminder" else "Select Saved Drama", color = if (selectedFilm != null) Color.White else Color(0xFF8F828B), fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun SuggestionRail(items: List<DramaItem>, selectedFilm: DramaItem?, onSelect: (DramaItem) -> Unit, onLibrary: () -> Unit) {
    SectionTitle("Saved Watchlist")
    if (items.isEmpty()) {
        EmptySavedWatchlist(onLibrary)
        return
    }
    LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { film ->
            val selected = film.id == selectedFilm?.id && film.title == selectedFilm.title
            Column(
                Modifier
                    .width(136.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF121116))
                    .border(1.dp, if (selected) Gold else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                    .clickable { onSelect(film) }
            ) {
                Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF241E26))) {
                    NetworkDramaImage(film.imageUrl, Modifier.fillMaxSize(), ContentScale.Crop, film.title)
                }
                Text(film.title, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp, modifier = Modifier.padding(9.dp))
            }
        }
    }
}

@Composable
private fun EmptySavedWatchlist(onLibrary: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onLibrary)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("No saved dramas yet", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Text("Save films from Home or Shorts, then schedule them here.", color = Color(0xFFCDB5BC), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun PlannedList(items: List<PlannerItem>) {
    SectionTitle("Upcoming Watchlist")
    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (items.isEmpty()) {
            EmptyPlanner()
        } else {
            items.forEach { item -> PlannedRow(item) }
        }
    }
}

@Composable
private fun PlannedRow(item: PlannerItem) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Schedule, contentDescription = null, tint = Gold, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            Text("Episode ${item.episode ?: 1} - ${item.scheduledAt}", color = Color(0xFFBDA3AB), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun EmptyPlanner() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Pink, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(12.dp))
        Text("Scheduled dramas will appear here.", color = Color(0xFFCDB5BC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF26232A)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 18.dp), letterSpacing = 0.sp)
}
