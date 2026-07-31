package com.drama.x.drama.series.dramax.dramaseries.screen

/**
 * Shared dialog/sheet composables used by both ShortsScreen and EpisodeScreen.
 * All were previously private in ShortsScreen — moved here as internal so both
 * screens can reference them without duplication.
 */

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.data.ShortsItem
import com.drama.x.drama.series.dramax.dramaseries.data.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Colour tokens (shared) ──────────────────────────────────────────────────
internal val SharedPink = Color(0xFFFF5168)
internal val SharedGold = Color(0xFFF5C65B)

internal const val SHARED_EPISODES_PER_PAGE = 30
internal const val SHARED_EPISODES_PER_ROW  = 5
internal const val SHARED_FREE_PREVIEW      = 3

internal val SharedReportReasons = listOf(
    "Episode error",
    "Paid film too long",
    "Low Quality",
    "Subtitle missing",
    "Inaccurate Subtitle",
    "Other"
)

// ── SubtitleSize enum (shared) ──────────────────────────────────────────────
internal enum class SharedSubtitleSize { SMALL, MEDIUM, LARGE }

// ── Unlock Episode Dialog ───────────────────────────────────────────────────
@Composable
internal fun SharedUnlockEpisodeDialog(
    posterUrl: String,
    episodeNumber: Int,
    dailyUnlocksUsed: Int,
    dailyUnlockLimit: Int,
    isLoading: Boolean,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF16121A))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFF8F8791),
                    modifier = Modifier.size(22.dp).clip(CircleShape)
                        .background(Color(0x33000000))
                        .clickable(enabled = !isLoading, onClick = onDismiss).padding(3.dp))
            }
            Box(modifier = Modifier.width(150.dp).aspectRatio(0.72f).clip(RoundedCornerShape(14.dp))) {
                SharedThumbnail(posterUrl, Modifier.fillMaxWidth().aspectRatio(0.72f))
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFE12E3E))
                    .padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                    Text("EP $episodeNumber", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.unlock_episode_title, episodeNumber), color = Color.White,
                fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.unlock_episode_desc), color = Color(0xFF9D8A91),
                fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.daily_progress, dailyUnlocksUsed, dailyUnlockLimit),
                color = Color(0xFF7A7178), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50))
                .background(if (isLoading) Color(0x553A3035) else SharedPink)
                .clickable(enabled = !isLoading, onClick = onWatchAd), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White,
                            modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0x33FFFFFF)))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.watch_ad), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.unlock_permanent_note), color = Color(0xFF7A7178),
                fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

// ── Daily Limit Reached Dialog ──────────────────────────────────────────────
@Composable
internal fun SharedDailyLimitDialog(
    dailyUnlockLimit: Int,
    onBrowseEpisodes: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxWidth(0.82f).clip(RoundedCornerShape(26.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF3A1216), Color(0xFF16121A)), radius = 340f))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFF8F8791),
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0x33000000))
                        .clickable(onClick = onDismiss).padding(3.dp))
            }
            Spacer(Modifier.height(6.dp))
            Icon(Icons.Filled.HourglassEmpty, contentDescription = null, tint = Color(0xFFF08A3C), modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.daily_limit_title), color = Color.White,
                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.daily_limit_desc, dailyUnlockLimit), color = Color(0xFFCDB8BF),
                fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50))
                .background(SharedPink).clickable(onClick = onBrowseEpisodes), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.browse_free_episodes), color = Color.White,
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── Episode Options Sheet (grid picker) ────────────────────────────────────
@Composable
internal fun SharedEpisodeOptionsSheet(
    currentEpisode: Int,
    totalEpisodes: Int,
    unlockedThrough: Int = currentEpisode,
    onEpisodeSelected: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    val safeTotal = totalEpisodes.coerceAtLeast(1)
    var page by remember { mutableStateOf((currentEpisode - 1) / SHARED_EPISODES_PER_PAGE) }
    val pageStart = page * SHARED_EPISODES_PER_PAGE + 1
    val pageEnd   = (pageStart + SHARED_EPISODES_PER_PAGE - 1).coerceAtMost(safeTotal)
    val episodes  = remember(page, safeTotal) { (pageStart..pageEnd).toList() }
    val hasNext   = pageEnd < safeTotal
    val hasPrev   = page > 0

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxWidth(0.86f).clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF16121A)).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.episodes), color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("$currentEpisode/$safeTotal", color = Color(0xFF9D8A91), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(18.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(SHARED_EPISODES_PER_ROW),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(episodes, key = { it }) { ep ->
                    SharedEpisodeCell(episode = ep, isPlaying = ep == currentEpisode,
                        isLocked = ep > unlockedThrough, onClick = { onEpisodeSelected(ep); onDismiss() })
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                if (hasPrev) {
                    SharedPagerPill("Episodes ${(page-1)*SHARED_EPISODES_PER_PAGE+1} - ${page*SHARED_EPISODES_PER_PAGE}", true) { page-- }
                    if (hasNext) Spacer(Modifier.width(10.dp))
                }
                if (hasNext) {
                    SharedPagerPill("Episodes ${pageEnd+1} - ${(pageEnd+SHARED_EPISODES_PER_PAGE).coerceAtMost(safeTotal)}", false) { page++ }
                }
            }
        }
    }
}

@Composable
private fun SharedEpisodeCell(episode: Int, isPlaying: Boolean, isLocked: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.aspectRatio(0.92f).clip(RoundedCornerShape(14.dp))
        .background(if (isPlaying) Color(0x33F5C65B) else Color(0xFF211D25))
        .then(if (isPlaying) Modifier.border(1.5.dp, SharedGold, RoundedCornerShape(14.dp)) else Modifier)
        .clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            if (isLocked) Icon(Icons.Filled.Lock, contentDescription = null,
                tint = Color(0xFF6B6470), modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(episode.toString(),
            color = if (isPlaying) SharedGold else if (isLocked) Color(0xFF9D8FA0) else Color.White,
            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        if (isPlaying) Text("PLAYING", color = SharedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
        else Spacer(Modifier.height(10.dp))
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SharedPagerPill(text: String, leading: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF262129))
        .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (leading) { Icon(Icons.Filled.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)) }
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (!leading) { Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.ChevronRight, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
    }
}

// ── Playback Options (3-dot menu) ───────────────────────────────────────────
@Composable
internal fun SharedPlaybackOptionsSheet(
    autoNext: Boolean,
    autoUnlock: Boolean,
    onAutoNextChange: (Boolean) -> Unit,
    onAutoUnlockChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxWidth(0.86f).clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF16121A)).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp)).padding(20.dp)) {
            Text(stringResource(R.string.playback_options), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            SharedPlaybackToggleRow(stringResource(R.string.auto_next_episode), stringResource(R.string.auto_next_episode_desc), autoNext, onAutoNextChange)
            Spacer(Modifier.height(20.dp))
            SharedPlaybackToggleRow(stringResource(R.string.auto_unlock_episodes), stringResource(R.string.auto_unlock_episodes_desc), autoUnlock, onAutoUnlockChange)
        }
    }
}

@Composable
private fun SharedPlaybackToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(description, color = Color(0xFF8F8791), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
        }
        Spacer(Modifier.width(12.dp))
        SharedPillSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SharedPillSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(checked = checked, onCheckedChange = onCheckedChange)
}

// ── Report an Issue (Feedback Form) ────────────────────────────────────────
@Composable
internal fun SharedFeedbackFormSheet(
    filmTitle: String,
    episodeNumber: Int,
    thumbnailUrl: String,
    selectedReason: String,
    onReasonSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxWidth(0.86f).clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF16121A)).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Report an Issue", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Help us improve your viewing experience", color = Color(0xFF8F8791), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFF8F8791),
                    modifier = Modifier.size(22.dp).clickable(onClick = onDismiss))
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SharedThumbnail(thumbnailUrl, Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(filmTitle, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.episode_title, episodeNumber), color = Color(0xFFB7ABB2),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("Please select a reason", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Column {
                SharedReportReasons.forEach { reason ->
                    val selected = reason == selectedReason
                    Row(modifier = Modifier.fillMaxWidth().clickable { onReasonSelected(reason) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(reason, color = if (selected) SharedPink else Color.White, fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        SharedReasonRadio(selected = selected)
                    }
                    if (reason != SharedReportReasons.last()) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50))
                .background(if (selectedReason.isNotBlank()) SharedPink else Color(0x553A3035))
                .clickable(enabled = selectedReason.isNotBlank(), onClick = onSubmit),
                contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.submit),
                    color = if (selectedReason.isNotBlank()) Color.White else Color(0xFF8F7D84),
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun SharedReasonRadio(selected: Boolean) {
    Box(modifier = Modifier.size(24.dp).clip(CircleShape)
        .background(if (selected) SharedPink else Color.Transparent)
        .border(width = if (selected) 0.dp else 1.5.dp,
            color = if (selected) Color.Transparent else Color(0xFF4A4550), shape = CircleShape),
        contentAlignment = Alignment.Center) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

// ── Subtitle Options Sheet ──────────────────────────────────────────────────
@Composable
internal fun SharedSubtitleOptionsSheet(
    tracks: List<SubtitleTrack>,
    selectedUrl: String,
    subtitleSize: SharedSubtitleSize = SharedSubtitleSize.SMALL,
    onSelect: (SubtitleTrack) -> Unit,
    onSizeChange: (SharedSubtitleSize) -> Unit = {},
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var pendingUrl  by remember(selectedUrl)   { mutableStateOf(selectedUrl) }
    var pendingSize by remember(subtitleSize)  { mutableStateOf(subtitleSize) }
    val listState   = rememberLazyListState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxWidth(0.86f).clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF16121A)).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp))
            .padding(horizontal = 20.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.subtitles), color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFF8F8791),
                    modifier = Modifier.size(22.dp).clickable(onClick = onDismiss))
            }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SharedSizeOption("Small",  15.sp, pendingSize == SharedSubtitleSize.SMALL)  { pendingSize = SharedSubtitleSize.SMALL;  onSizeChange(SharedSubtitleSize.SMALL) }
                SharedSizeOption("Medium", 19.sp, pendingSize == SharedSubtitleSize.MEDIUM) { pendingSize = SharedSubtitleSize.MEDIUM; onSizeChange(SharedSubtitleSize.MEDIUM) }
                SharedSizeOption("Large",  24.sp, pendingSize == SharedSubtitleSize.LARGE)  { pendingSize = SharedSubtitleSize.LARGE;  onSizeChange(SharedSubtitleSize.LARGE) }
            }
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tracks, key = { it.url }) { track ->
                        val sel = track.url == pendingUrl
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (sel) Color(0xFF2A2530) else Color.Transparent)
                            .clickable { pendingUrl = track.url; onSelect(track) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(track.label, color = if (sel) SharedGold else Color.White, fontSize = 15.sp,
                                fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            SharedRadioDot(selected = sel)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.width(3.dp).height(300.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x33FFFFFF))) {
                    val itemCount = tracks.size.coerceAtLeast(1)
                    val visibleFrac = (5f / itemCount).coerceIn(0.15f, 1f)
                    val scrollFrac  = if (itemCount <= 1) 0f else listState.firstVisibleItemIndex / (itemCount - 1).toFloat()
                    Box(modifier = Modifier.fillMaxWidth().height((300f * visibleFrac).dp)
                        .padding(top = (300f * (1f - visibleFrac) * scrollFrac).dp)
                        .clip(RoundedCornerShape(2.dp)).background(SharedGold))
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50))
                .background(SharedPink).clickable(onClick = onSave), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.save), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun SharedSizeOption(label: String, glyphSize: androidx.compose.ui.unit.TextUnit, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp))
            .then(if (selected) Modifier.border(1.5.dp, SharedGold, RoundedCornerShape(14.dp)) else Modifier),
            contentAlignment = Alignment.Center) {
            Box {
                Text("A", color = SharedGold, fontSize = glyphSize, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 10.dp, bottom = 6.dp))
                Text("A", color = SharedGold, fontSize = (glyphSize.value * 1.35f).sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 10.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SharedRadioDot(selected: Boolean) {
    Box(modifier = Modifier.size(20.dp).clip(CircleShape)
        .background(if (selected) SharedGold else Color.Transparent)
        .border(width = if (selected) 0.dp else 1.5.dp,
            color = if (selected) Color.Transparent else Color(0xFF4A4550), shape = CircleShape),
        contentAlignment = Alignment.Center) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF1A1A1A), modifier = Modifier.size(12.dp))
    }
}

// ── Share Sheet ─────────────────────────────────────────────────────────────
@Composable
internal fun SharedShareSheet(shareText: String, onDismiss: () -> Unit) {
    val context   = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope     = rememberCoroutineScope()
    val shareApps by produceState<List<SharedShareApp>>(emptyList(), shareText) {
        value = withContext(Dispatchers.IO) { context.resolveSharedShareApps(shareText) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxWidth(0.86f).clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF16121A)).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp)).padding(20.dp)) {
            Text("Share", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF211D25)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(shareText, color = Color(0xFFE5D2D7), fontSize = 13.sp, lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color(0xFFCDB8BF),
                    modifier = Modifier.size(20.dp).clickable {
                        scope.launch {
                            clipboard.setClipEntry(androidx.compose.ui.platform.ClipEntry(
                                android.content.ClipData.newPlainText("share_text", shareText)))
                        }
                    })
            }
            Spacer(Modifier.height(20.dp))
            if (shareApps.isEmpty()) {
                Text("Loading share apps…", color = Color(0xFF8F8791), fontSize = 13.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(shareApps, key = { it.packageName + it.activityName }) { app ->
                        SharedShareAppIcon(app) { context.shareViaSharedComponent(shareText, app); onDismiss() }
                    }
                }
            }
        }
    }
}

internal data class SharedShareApp(val label: String, val packageName: String, val activityName: String, val icon: Bitmap?)

@Composable
private fun SharedShareAppIcon(app: SharedShareApp, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0xFF2E2A31)).clickable(onClick = onClick),
            contentAlignment = Alignment.Center) {
            if (app.icon != null) Image(app.icon.asImageBitmap(), null, modifier = Modifier.size(54.dp).clip(CircleShape))
        }
        Spacer(Modifier.height(6.dp))
        Text(app.label, color = Color(0xFFE5D2D7), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

internal fun Context.resolveSharedShareApps(shareText: String): List<SharedShareApp> {
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
    val infos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
        packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    else @Suppress("DEPRECATION") packageManager.queryIntentActivities(intent, 0)
    return infos.distinctBy { it.activityInfo.packageName + it.activityInfo.name }.mapNotNull { ri ->
        runCatching {
            val d: Drawable = ri.loadIcon(packageManager)
            SharedShareApp(ri.loadLabel(packageManager).toString(), ri.activityInfo.packageName,
                ri.activityInfo.name, d.toSharedBitmap())
        }.getOrNull()
    }
}

internal fun Context.shareViaSharedComponent(shareText: String, app: SharedShareApp) {
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText)
        setClassName(app.packageName, app.activityName); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try { startActivity(intent) } catch (_: android.content.ActivityNotFoundException) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText) }, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun Drawable.toSharedBitmap(): Bitmap? = runCatching {
    val bmp = Bitmap.createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp); setBounds(0, 0, c.width, c.height); draw(c); bmp
}.getOrNull()

// ── Shared thumbnail loader (bitmap from URL) ───────────────────────────────
@Composable
internal fun SharedThumbnail(imageUrl: String, modifier: Modifier) {
    val bitmap by produceState<Bitmap?>(null, imageUrl) {
        value = if (imageUrl.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching { java.net.URL(imageUrl).openStream().use { android.graphics.BitmapFactory.decodeStream(it) } }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    } else {
        Box(modifier = modifier.background(
            Brush.linearGradient(listOf(Color(0xFF25121C), Color(0xFF101C2A), Color(0xFF050507)))))
    }
}

// ── Build share text ────────────────────────────────────────────────────────
internal fun buildSharedShareText(item: ShortsItem, context: Context): String = buildString {
    append("Watch "); append(item.film.title.ifBlank { "this DramaX short" })
    append(" Episode "); append(item.episodeNumber); append(" on DramaX.\n\n")
    append("Get the app: https://play.google.com/store/apps/details?id="); append(context.packageName)
}
