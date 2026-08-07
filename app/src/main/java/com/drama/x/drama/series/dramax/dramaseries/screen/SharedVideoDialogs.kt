package com.drama.x.drama.series.dramax.dramaseries.screen

/**
 * Shared dialog/sheet composables used by both ShortsScreen and EpisodeScreen.
 * All were previously private in ShortsScreen — moved here as internal so both
 * screens can reference them without duplication.
 */

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
private fun SharedEpisodeCell(
    episode: Int,
    isPlaying: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .aspectRatio(0.92f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) Color(0x33F5C65B) else Color(0xFF211D25))
            .then(
                if (isPlaying) Modifier.border(
                    1.5.dp,
                    SharedGold,
                    RoundedCornerShape(14.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            if (isLocked) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFF6B6470),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = episode.toString(),
            color = if (isPlaying) SharedGold else if (isLocked) Color(0xFF9D8FA0) else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(10.dp))

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
            // Auto Unlock feature temporarily hidden
            // Spacer(Modifier.height(20.dp))
            // SharedPlaybackToggleRow(stringResource(R.string.auto_unlock_episodes), stringResource(R.string.auto_unlock_episodes_desc), autoUnlock, onAutoUnlockChange)
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
    val SharedReportReasons = listOf(
        stringResource(R.string.report_reason_episode_error),
        stringResource(R.string.report_reason_paid_film_too_long),
        stringResource(R.string.report_reason_low_quality),
        stringResource(R.string.report_reason_subtitle_missing),
        stringResource(R.string.report_reason_inaccurate_subtitle),
        stringResource(R.string.report_reason_other)
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxWidth(0.86f).clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF16121A)).border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.report_issue), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.report_issue_desc), color = Color(0xFF8F8791), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
            Text(stringResource(R.string.please_select_reason), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                SharedSizeOption(stringResource(R.string.subtitle_small),  15.sp, pendingSize == SharedSubtitleSize.SMALL)  { pendingSize = SharedSubtitleSize.SMALL;  onSizeChange(SharedSubtitleSize.SMALL) }
                SharedSizeOption(stringResource(R.string.subtitle_medium), 19.sp, pendingSize == SharedSubtitleSize.MEDIUM) { pendingSize = SharedSubtitleSize.MEDIUM; onSizeChange(SharedSubtitleSize.MEDIUM) }
                SharedSizeOption(stringResource(R.string.subtitle_large),  24.sp, pendingSize == SharedSubtitleSize.LARGE)  { pendingSize = SharedSubtitleSize.LARGE;  onSizeChange(SharedSubtitleSize.LARGE) }
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
            Text(stringResource(R.string.share), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
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
                Text(stringResource(R.string.loading_share_apps), color = Color(0xFF8F8791), fontSize = 13.sp)
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

// ── Shared Video Top Bar (used by both ShortsScreen and EpisodeScreen) ──────
@Composable
internal fun SharedVideoTopBar(
    item: ShortsItem,
    showActions: Boolean,
    onBack: () -> Unit,
    onFeedbackClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onBack)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFFE5E1E4), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.weight(1f))
        if (showActions) {
            SharedHeaderCircleAction(
                icon = Icons.Filled.Feedback,
                onClick = onFeedbackClick
            )
            Spacer(Modifier.width(8.dp))
            SharedHeaderCircleAction(
                icon = Icons.Filled.MoreVert,
                onClick = onOptionsClick
            )
        }
    }
}

@Composable
private fun SharedHeaderCircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x66131315))
            .border(1.dp, Color(0x14FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFE5E1E4), modifier = Modifier.size(20.dp))
    }
}

// ── Shared Video Caption/Info Section (bottom info with seekbar for episodes) ──
@Composable
internal fun SharedVideoCaption(
    item: ShortsItem,
    positionMs: Long,
    durationMs: Long,
    isLocked: Boolean,
    isEpisodeMode: Boolean,
    bottomReservedPadding: Dp,
    onSeekTo: (Long) -> Unit,
    onWatchNowClick: () -> Unit
) {
    val description = item.film.description.ifBlank {stringResource(R.string.watch_great_content) }
    var descriptionExpanded by remember(item.film.id, item.episodeNumber) { mutableStateOf(false) }
    val showDescriptionToggle = description.length > 70
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = if (isLocked) 16.dp else 90.dp, bottom = bottomReservedPadding + 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Episode ${item.episodeNumber} / ${item.film.episodeTotal}",
                color = SharedGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33F5C65B))
                    .border(1.dp, Color(0x4DF5C65B), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.trending), color = Color(0xFFE5BDBE), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            item.film.title,
            color = Color(0xFFE5E1E4),
            fontSize = 20.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            color = Color(0xCCE5E1E4),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            maxLines = if (descriptionExpanded) 4 else 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        if (showDescriptionToggle) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (descriptionExpanded) stringResource(R.string.view_less) else stringResource(R.string.view_more),
                color = SharedGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
                modifier = Modifier.clickable { descriptionExpanded = !descriptionExpanded }
            )
        }
        Spacer(Modifier.height(10.dp))
        if (isEpisodeMode) {
            SharedThinSeekBar(
                progress = if (durationMs > 0L) positionMs.toFloat() / durationMs.toFloat() else 0f,
                onSeekFraction = { fraction ->
                    if (durationMs > 0L) onSeekTo((durationMs * fraction).toLong())
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatSharedPlaybackTime(positionMs),
                    color = Color(0x99E5BDBE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                Text(
                    formatSharedPlaybackTime(durationMs),
                    color = Color(0x99E5BDBE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(170.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SharedPink)
                    .clickable(onClick = onWatchNowClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.watch_now),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SharedThinSeekBar(
    progress: Float,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableStateOf(1) }
    Box(
        modifier = modifier
            .height(24.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(widthPx) {
                detectTapGestures { offset ->
                    onSeekFraction((offset.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(widthPx) {
                detectDragGestures { change, _ ->
                    onSeekFraction((change.position.x / widthPx).coerceIn(0f, 1f))
                    change.consume()
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x66FFFFFF))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SharedPink)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .wrapContentWidth(Alignment.End)
                .offset(x = (-6).dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(SharedPink)
        )
    }
}

private fun formatSharedPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds) else String.format("%d:%02d", minutes, seconds)
}

// ── Shared imports needed ───────────────────────────────────────────────────
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ── Shared Video Sidebar Actions (used by both ShortsScreen and EpisodeScreen) ──
@Composable
internal fun SharedVideoSidebar(
    liked: Boolean,
    likeCount: Int,
    bookmarked: Boolean,
    saveCount: Int,
    ccEnabled: Boolean,
    playbackSpeed: Float,
    isEpisodeMode: Boolean,
    modifier: Modifier = Modifier,
    onLikeClick: (Boolean) -> Unit,
    onBookmarkClick: (Boolean) -> Unit,
    onShareClick: () -> Unit,
    onEpisodesClick: () -> Unit,
    onCcClick: () -> Unit,
    onSpeedClick: () -> Unit,
    bottomReservedPadding: Dp = 0.dp
) {
    Column(
        modifier = modifier
            .padding(end = 12.dp, bottom = bottomReservedPadding + 102.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Like Button
        SharedSideAction(
            icon = if (liked) Icons.Filled.FavoriteBorder else Icons.Filled.FavoriteBorder,
            label = formatSharedCount(likeCount + if (liked) 1 else 0),
            tint = if (liked) SharedPink else Color(0xFFFFAAB6),
            onClick = { onLikeClick(!liked) }
        )

        // Bookmark Button
        SharedSideAction(
            icon = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            label = formatSharedCount(saveCount + if (bookmarked) 1 else 0),
            tint = if (bookmarked) SharedGold else Color.White,
            onClick = { onBookmarkClick(!bookmarked) }
        )

        // Share Button
        SharedSideAction(
            icon = Icons.Filled.Share,
            label = stringResource(R.string.share_label),
            tint = Color.White,
            onClick = onShareClick
        )

        // Episodes Button (only in episode mode)
        if (isEpisodeMode) {
            SharedSideAction(
                icon = Icons.Filled.VideoLibrary,
                label = stringResource(R.string.episodes_label),
                tint = Color.White,
                onClick = onEpisodesClick
            )
        }

        // Subtitles Button
        SharedSideAction(
            icon = Icons.Filled.ClosedCaption,
            label = "CC",
            tint = if (ccEnabled) SharedGold else Color.White,
            onClick = onCcClick
        )

        // Speed Button (only in episode mode)
        if (isEpisodeMode) {
            SharedSideTextAction(
                value = formatSharedSpeed(playbackSpeed),
                label = stringResource(R.string.speed_label),
                onClick = onSpeedClick
            )
        }
    }
}

@Composable
private fun SharedSideAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1E))
                .border(1.dp, Color(0xFF3A3640), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(label, color = Color(0xFFB7A8B3), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SharedSideTextAction(
    value: String,
    label: String,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1E))
                .border(1.dp, Color(0xFF3A3640), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Text(label, color = Color(0xFFB7A8B3), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Helper functions for sidebar ────────────────────────────────────────────
private fun formatSharedCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
    count >= 1_000 -> String.format("%.1fk", count / 1_000f)
    else -> count.toString()
}

private fun formatSharedSpeed(speed: Float): String = when (speed) {
    0.75f -> "0.75x"
    1f -> "1x"
    1.25f -> "1.25x"
    1.5f -> "1.5x"
    1.75f -> "1.75x"
    2f -> "2x"
    else -> "${speed}x"
}
