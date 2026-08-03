package com.drama.x.drama.series.dramax.dramaseries.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.data.ShortsItem
import com.drama.x.drama.series.dramax.dramaseries.data.SubtitleTrack
import com.drama.x.drama.series.dramax.dramaseries.model.EpisodeViewModel
import kotlinx.coroutines.launch
import com.drama.x.drama.series.dramax.dramaseries.R

// ── Design tokens ────────────────────────────────────────────────────────────
private val EpBgDark       = Color(0xFF131315)
private val EpScrimTop     = Color(0xFF0B0B0D)
private val EpPink         = Color(0xFFFF5168)
private val EpGold         = Color(0xFFF4BE4E)
private val EpGoldBg       = Color(0x33F4BE4E)
private val EpGoldBorder   = Color(0x4DF4BE4E)
private val EpTextPrimary  = Color(0xFFE5E1E4)
private val EpTextMuted    = Color(0xFFE5BDBE)
private val EpTextBody     = Color(0xCCE5E1E4)
private val EpBtnBg        = Color(0x66131315)
private val EpBtnBorder    = Color(0x14FFFFFF)
private val EpProgressBg   = Color(0x1AFFFFFF)
private val EpLockRed      = Color(0xFFFF6B6B)

// ── Root screen ──────────────────────────────────────────────────────────────
@Composable
fun EpisodeScreen(
    backendBaseUrl: String,
    filmId: Int?,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onRewards: () -> Unit,
    onProfile: () -> Unit,
    viewModel: EpisodeViewModel = viewModel()
) {
    BackHandler(onBack = onBack)

    val uiState        by viewModel.uiState.collectAsState()
    val context        = LocalContext.current
    val activity       = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()

    // playback state
    var isPlaying         by remember { mutableStateOf(false) }
    // side-bar interaction state
    var liked             by remember { mutableStateOf(false) }
    var bookmarked        by remember { mutableStateOf(false) }
    // popup visibility
    var showEpisodeList   by remember { mutableStateOf(false) }
    var showPlaybackOpts  by remember { mutableStateOf(false) }
    var showSubtitleOpts  by remember { mutableStateOf(false) }
    var showShareSheet    by remember { mutableStateOf(false) }
    var showFeedback      by remember { mutableStateOf(false) }
    var showDailyLimit    by remember { mutableStateOf(false) }
    // unlock dialog
    var unlockTarget      by remember { mutableStateOf<ShortsItem?>(null) }
    var isWatchingAd      by remember { mutableStateOf(false) }
    // playback options toggles
    var autoNext          by remember { mutableStateOf(false) }
    var autoUnlock        by remember { mutableStateOf(false) }
    // feedback form
    var feedbackReason    by remember { mutableStateOf("") }
    // subtitle state
    var selectedSubUrl    by remember { mutableStateOf("") }
    var subtitleSize      by remember { mutableStateOf(SharedSubtitleSize.SMALL) }

    val pagerState = rememberPagerState { uiState.episodes.size.coerceAtLeast(1) }

    LaunchedEffect(filmId, backendBaseUrl) {
        android.util.Log.d("EpisodeScreen", "LaunchedEffect: filmId=$filmId, backendBaseUrl=$backendBaseUrl")
        if (filmId != null) {
            viewModel.loadEpisodes(backendBaseUrl, filmId)
        } else {
            android.util.Log.w("EpisodeScreen", "filmId is null!")
        }
    }

    LaunchedEffect(pagerState.currentPage, uiState.episodes.size) {
        if (uiState.episodes.isNotEmpty()) {
            viewModel.ensurePlayback(pagerState.currentPage, backendBaseUrl)
        }
        isPlaying = true
    }

    val currentItem = uiState.episodes.getOrNull(pagerState.currentPage)

    Box(modifier = Modifier.fillMaxSize().background(EpBgDark)) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = EpPink)
            }
            uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(
                        "Error: ${uiState.errorMessage}",
                        color = Color(0xFFFF6B6B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            uiState.episodes.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    "No episodes found", color = EpTextPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                // ── Vertical pager ────────────────────────────────────
                VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize(), pageSpacing = 0.dp) { page ->
                    val item = uiState.episodes.getOrNull(page) ?: return@VerticalPager
                    EpisodePage(
                        item            = item,
                        isPlaying       = isPlaying && page == pagerState.currentPage,
                        isLocked        = viewModel.isEpisodeLocked(item.episodeNumber),
                        liked           = liked,
                        bookmarked      = bookmarked,
                        totalEpisodes   = uiState.episodes.size,
                        onTogglePlay    = { isPlaying = !isPlaying },
                        onLike          = { liked = !liked },
                        onBookmark      = { bookmarked = !bookmarked },
                        onShare         = { showShareSheet = true },
                        onEpisodes      = { showEpisodeList = true },
                        onSubtitle      = { showSubtitleOpts = true },
                        onSpeedCycle    = { /* handled inside EpisodePage */ },
                        onReport        = { showFeedback = true },
                        onMoreOptions   = { showPlaybackOpts = true },
                        onUnlockTap     = {
                            if (viewModel.canUnlockMore()) unlockTarget = item
                            else showDailyLimit = true
                        },
                        onProgressUpdate = { pos, dur ->
                            viewModel.saveWatchProgress(
                                backendBaseUrl, item.film.id, item.episodeNumber,
                                (pos / 1000).toInt(), (dur / 1000).toInt()
                            )
                        }
                    )
                }

                // ── Top bar ───────────────────────────────────────────
                SharedVideoTopBar(
                    item = currentItem ?: return@Box,
                    showActions = true,
                    onBack      = onBack,
                    onFeedbackClick = { showFeedback = true },
                    onOptionsClick  = { showPlaybackOpts = true }
                )

                // ── Episode list drawer ───────────────────────────────
                if (showEpisodeList) {
                    EpisodeListDrawer(
                        episodes         = uiState.episodes,
                        currentEpisode   = currentItem?.episodeNumber ?: 1,
                        unlockedEpisodes = uiState.unlockedEpisodes,
                        dailyUnlocksUsed = uiState.dailyUnlocksUsed,
                        dailyUnlockLimit = uiState.dailyUnlockLimit,
                        onSelectEpisode  = { epNum ->
                            val idx = uiState.episodes.indexOfFirst { it.episodeNumber == epNum }
                            if (idx >= 0) coroutineScope.launch { pagerState.scrollToPage(idx) }
                            showEpisodeList = false
                        },
                        onDismiss = { showEpisodeList = false }
                    )
                }
            }
        }

        // ── Dialogs (outside pager so they always show on top) ────────

        // Playback options (3-dot)
        if (showPlaybackOpts) {
            SharedPlaybackOptionsSheet(
                autoNext          = autoNext,
                autoUnlock        = autoUnlock,
                onAutoNextChange  = { autoNext = it },
                onAutoUnlockChange= { autoUnlock = it },
                onDismiss         = { showPlaybackOpts = false }
            )
        }

        // Episode grid picker
        if (showEpisodeList && uiState.episodes.isNotEmpty()) {
            // already shown as side drawer above — no duplicate needed
        }

        // Subtitle options
        if (showSubtitleOpts) {
            val tracks = currentItem?.subtitleTracks.orEmpty()
            SharedSubtitleOptionsSheet(
                tracks       = tracks,
                selectedUrl  = selectedSubUrl,
                subtitleSize = subtitleSize,
                onSelect     = { selectedSubUrl = it.url },
                onSizeChange = { subtitleSize = it },
                onSave       = { showSubtitleOpts = false },
                onDismiss    = { showSubtitleOpts = false }
            )
        }

        // Share sheet
        if (showShareSheet && currentItem != null) {
            SharedShareSheet(
                shareText = buildSharedShareText(currentItem, context),
                onDismiss = { showShareSheet = false }
            )
        }

        // Report an issue
        if (showFeedback && currentItem != null) {
            SharedFeedbackFormSheet(
                filmTitle        = currentItem.film.title,
                episodeNumber    = currentItem.episodeNumber,
                thumbnailUrl     = currentItem.film.imageUrl,
                selectedReason   = feedbackReason,
                onReasonSelected = { feedbackReason = it },
                onSubmit         = {
                    Toast.makeText(context, "Feedback sent", Toast.LENGTH_SHORT).show()
                    feedbackReason = ""
                    showFeedback = false
                },
                onDismiss = { showFeedback = false; feedbackReason = "" }
            )
        }

        // Unlock episode dialog
        val target = unlockTarget
        if (target != null) {
            SharedUnlockEpisodeDialog(
                posterUrl        = target.film.imageUrl,
                episodeNumber    = target.episodeNumber,
                dailyUnlocksUsed = uiState.dailyUnlocksUsed,
                dailyUnlockLimit = uiState.dailyUnlockLimit,
                isLoading        = isWatchingAd,
                onWatchAd        = {
                    isWatchingAd = true
                    isPlaying = false
                    activity?.let { act ->
                        AdsManager.loadAndShowRewardAll(
                            activity       = act,
                            onRewardEarned = {
                                viewModel.unlockEpisode(backendBaseUrl, target.film.id, target.episodeNumber)
                                isWatchingAd = false
                                unlockTarget = null
                                isPlaying = true
                            },
                            onFinished = {
                                isWatchingAd = false
                                if (!isPlaying) isPlaying = false
                            }
                        )
                    }
                },
                onDismiss = { if (!isWatchingAd) unlockTarget = null }
            )
        }

        // Daily limit dialog
        if (showDailyLimit) {
            SharedDailyLimitDialog(
                dailyUnlockLimit  = uiState.dailyUnlockLimit,
                onBrowseEpisodes  = { showDailyLimit = false; showEpisodeList = true },
                onDismiss         = { showDailyLimit = false }
            )
        }
    }
}

// ── Full-screen episode page ─────────────────────────────────────────────────
@Composable
private fun EpisodePage(
    item: ShortsItem,
    isPlaying: Boolean,
    isLocked: Boolean,
    liked: Boolean,
    bookmarked: Boolean,
    totalEpisodes: Int,
    onTogglePlay: () -> Unit,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onEpisodes: () -> Unit,
    onSubtitle: () -> Unit,
    onSpeedCycle: () -> Unit,
    onReport: () -> Unit,
    onMoreOptions: () -> Unit,
    onUnlockTap: () -> Unit,
    onProgressUpdate: (Long, Long) -> Unit
) {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var speed    by remember { mutableStateOf(1f) }
    var ccEnabled by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Video
        if (!isLocked && item.playUrl.isNotBlank()) {
            EpisodeVideoPlayer(
                playUrl  = item.playUrl,
                isPlaying = isPlaying,
                speed     = speed,
                onProgressUpdate = { pos, dur ->
                    position = pos; duration = dur
                    onProgressUpdate(pos, dur)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (!isLocked) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = EpTextPrimary)
            }
        }

        // Top scrim
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(
            Brush.verticalGradient(0f to EpScrimTop, 0.3f to EpScrimTop.copy(alpha = 0.4f), 1f to Color.Transparent)))

        // Bottom scrim
        Box(modifier = Modifier.fillMaxWidth().height(320.dp).align(Alignment.BottomCenter).background(
            Brush.verticalGradient(0f to Color.Transparent, 0.4f to EpScrimTop.copy(alpha = 0.8f), 1f to EpScrimTop)))

        // Tap to play/pause
        Box(modifier = Modifier.fillMaxSize().clickable(onClick = onTogglePlay))

        // Locked overlay
        if (isLocked) {
            EpLockedOverlay(item.episodeNumber, onUnlockTap)
        }

        // Right action bar
        SharedVideoSidebar(
            liked = liked,
            likeCount = 24500,
            bookmarked = bookmarked,
            saveCount = 1200,
            ccEnabled = ccEnabled,
            playbackSpeed = speed,
            isEpisodeMode = true,
            modifier = Modifier.align(Alignment.BottomEnd),
            onLikeClick = { onLike() },
            onBookmarkClick = { onBookmark() },
            onShareClick = onShare,
            onEpisodesClick = onEpisodes,
            onCcClick = onSubtitle,
            onSpeedClick = {
                speed = when (speed) { 0.75f -> 1f; 1f -> 1.25f; 1.25f -> 1.5f; 1.5f -> 2f; else -> 0.75f }
            },
            bottomReservedPadding = 32.dp
        )

        // Bottom info
        EpisodeBottomInfo(
            item          = item,
            totalEpisodes = totalEpisodes,
            position      = position,
            duration      = duration,
            modifier      = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth().padding(start = 16.dp, end = 72.dp, bottom = 32.dp)
        )
    }
}

// ── Bottom info (badge + title + desc + progress) ────────────────────────────
@Composable
private fun EpisodeBottomInfo(item: ShortsItem, totalEpisodes: Int, position: Long, duration: Long, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(EpGoldBg)
                .border(1.dp, EpGoldBorder, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("Episode ${item.episodeNumber}/$totalEpisodes", color = EpGold,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            }
            if (item.film.rating.isNotBlank()) {
                Text(item.film.rating, color = EpTextMuted, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            }
        }
        Text(item.film.title, color = EpTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (item.film.description.isNotBlank()) {
            Text(item.film.description, color = EpTextBody, fontSize = 14.sp,
                fontWeight = FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
        }
        Spacer(Modifier.height(12.dp))
        EpProgressBar(position, duration)
    }
}

@Composable
private fun EpProgressBar(position: Long, duration: Long) {
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(9999.dp)).background(EpProgressBg)) {
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                .clip(RoundedCornerShape(9999.dp)).background(EpPink))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(epFormatMs(position), color = EpTextMuted.copy(alpha = 0.6f),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            Text(epFormatMs(duration), color = EpTextMuted.copy(alpha = 0.6f),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
        }
    }
}

private fun epFormatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ── Top bar ──────────────────────────────────────────────────────────────────
@Composable
private fun EpisodeTopBar(onBack: () -> Unit, onEpisodes: () -> Unit, onMoreOpts: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart) {
        Box(modifier = Modifier.size(40.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = EpTextPrimary, modifier = Modifier.size(20.dp))
        }
        Row(modifier = Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EpTopBtn(onClick = onEpisodes) { Icon(Icons.Filled.VideoLibrary, "Episodes", tint = EpTextPrimary, modifier = Modifier.size(20.dp)) }
            EpTopBtn(onClick = onMoreOpts) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun EpTopBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(EpBtnBg)
        .border(1.dp, EpBtnBorder, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) { content() }
}

// ── Locked overlay ────────────────────────────────────────────────────────────
@Composable
private fun EpLockedOverlay(
    episodeNumber: Int,
    onUnlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = EpLockRed,
                modifier = Modifier.size(56.dp)
            )

            Text(
                text = stringResource(R.string.episode_locked, episodeNumber),
                color = EpTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(EpPink)
                    .clickable(onClick = onUnlock)
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.watch_ad_to_unlock),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Right side action button ──────────────────────────────────────────────────
@Composable
private fun EpSideAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(EpBtnBg)
            .border(1.dp, EpBtnBorder, CircleShape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(label, color = EpTextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun EpSpeedSideAction(speed: Float, onClick: () -> Unit) {
    val label = when (speed) { 0.75f -> "0.75x"; 1f -> "1x"; 1.25f -> "1.25x"; 1.5f -> "1.5x"; else -> "2x" }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(EpBtnBg)
            .border(1.dp, EpBtnBorder, CircleShape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Text("Speed", color = EpTextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
    }
}

// ── Episode list side drawer ──────────────────────────────────────────────────
@Composable
private fun EpisodeListDrawer(
    episodes: List<ShortsItem>, currentEpisode: Int, unlockedEpisodes: Set<Int>,
    dailyUnlocksUsed: Int, dailyUnlockLimit: Int,
    onSelectEpisode: (Int) -> Unit, onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0x80000000)).clickable(onClick = onDismiss))
        Column(
            modifier = Modifier.fillMaxWidth(0.82f).fillMaxHeight(0.9f).align(Alignment.BottomEnd)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Episodes  •  $dailyUnlocksUsed/$dailyUnlockLimit free today",
                color = EpTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp))
            LazyColumn {
                items(episodes) { item ->
                    val locked = item.episodeNumber !in unlockedEpisodes
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (item.episodeNumber == currentEpisode) Color(0xFF2A2A2A) else Color(0xFF242427))
                            .clickable(enabled = !locked) { onSelectEpisode(item.episodeNumber) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Episode ${item.episodeNumber}", color = EpTextPrimary, fontSize = 13.sp,
                            fontWeight = if (item.episodeNumber == currentEpisode) FontWeight.Bold else FontWeight.Normal)
                        if (locked) Icon(Icons.Filled.Lock, "Locked", tint = EpLockRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ── ExoPlayer video ───────────────────────────────────────────────────────────
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun EpisodeVideoPlayer(
    playUrl: String,
    isPlaying: Boolean,
    speed: Float,
    onProgressUpdate: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context   = LocalContext.current
    val exoPlayer = remember(playUrl) {
        ExoPlayer.Builder(context).setTrackSelector(DefaultTrackSelector(context)).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(playUrl)))
            prepare()
        }
    }
    LaunchedEffect(isPlaying)  { exoPlayer.playWhenReady = isPlaying }
    LaunchedEffect(speed)      { exoPlayer.setPlaybackSpeed(speed) }
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener); exoPlayer.release() }
    }
    AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false } }, modifier = modifier)
}

// ── Utility ───────────────────────────────────────────────────────────────────
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity       -> this
    is ContextWrapper -> baseContext.findActivity()
    else              -> null
}
