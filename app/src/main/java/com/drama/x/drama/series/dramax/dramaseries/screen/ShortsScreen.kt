package com.drama.x.drama.series.dramax.dramaseries.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.ads.NativeAdState
import com.drama.x.drama.series.dramax.dramaseries.data.ShortsItem
import com.drama.x.drama.series.dramax.dramaseries.data.SubtitleTrack
import com.drama.x.drama.series.dramax.dramaseries.model.ShortsViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private val ShortsBackground = Color(0xFF050507)
private val Gold = Color(0xFFF5C65B)
private val Pink = Color(0xFFFF5168)

private const val DAILY_UNLOCK_LIMIT = 7
private const val FREE_SHORTS_PREVIEW_EPISODES = 7

private data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

private sealed interface ShortsFeedPage {
    data class Video(val item: ShortsItem, val itemIndex: Int) : ShortsFeedPage
    data object NativeFullscreenAd : ShortsFeedPage
}

@Composable
fun ShortsScreen(
    backendBaseUrl: String,
    initialFilmId: Int?,
    initialEpisodeNumber: Int? = null,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onRewards: () -> Unit,
    onProfile:() -> Unit,
    viewModel: ShortsViewModel = viewModel()
) {

    BackHandler(onBack = onBack)

    val uiState by viewModel.uiState.collectAsState()
    val feedPages = remember(uiState.items) { uiState.items.withNativeAdPages() }
    val pagerState = rememberPagerState { feedPages.size.coerceAtLeast(1) }
    val currentFeedPage = feedPages.getOrNull(pagerState.currentPage)
    val currentVideoPage = currentFeedPage as? ShortsFeedPage.Video
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var showPlaybackOptions by remember { mutableStateOf(false) }
    var showFeedbackForm by remember { mutableStateOf(false) }
    var autoNext by remember { mutableStateOf(false) }
    var autoUnlock by remember { mutableStateOf(false) }
    var ccEnabled by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    val bottomBannerVisible = shouldShowAppBottomBanner()
    val bottomBannerPadding = if (bottomBannerVisible) AppBottomBannerHeight else 0.dp

    var dailyUnlocksUsed by remember { mutableStateOf(0) }
    var unlockedEpisodeKeys by remember { mutableStateOf(setOf<String>()) }
    var episodeModeKeys by remember { mutableStateOf(setOf<String>()) }
    var nativeShortVideoAdState by remember { mutableStateOf<NativeAdState>(NativeAdState.Idle) }

    LaunchedEffect(activity, uiState.items.size) {
        activity?.let { 
            // Start preloading native ads immediately instead of waiting for items to load
            AdsManager.loadNativeShortVideoFullscreen(it) 
        }
    }

    DisposableEffect(Unit) {
        val observer = androidx.lifecycle.Observer<NativeAdState> { nativeShortVideoAdState = it }
        AdsManager.nativeShortVideoFullscreenAdLive.observeForever(observer)
        onDispose {
            AdsManager.nativeShortVideoFullscreenAdLive.removeObserver(observer)
        }
    }

    LaunchedEffect(pagerState.currentPage, feedPages.size) {
        val currentItem = currentVideoPage?.item
        controlsVisible = true
        isPlaying = currentItem != null
        showPlaybackOptions = false
        showFeedbackForm = false
        ccEnabled = currentItem?.subtitleTracks?.isNotEmpty() == true
        if (currentVideoPage != null) {
            viewModel.ensurePlayback(currentVideoPage.itemIndex, backendBaseUrl)
            viewModel.loadMoreIfNeeded(currentVideoPage.itemIndex, backendBaseUrl)
        }
        
        // Prefetch adjacent videos for smooth transitions
        val nextPage = pagerState.currentPage + 1
        if (nextPage < feedPages.size) {
            val nextFeedPage = feedPages.getOrNull(nextPage)
            if (nextFeedPage is ShortsFeedPage.Video) {
                viewModel.ensurePlayback(nextFeedPage.itemIndex, backendBaseUrl)
            }
        }
        
        val prevPage = pagerState.currentPage - 1
        if (prevPage >= 0) {
            val prevFeedPage = feedPages.getOrNull(prevPage)
            if (prevFeedPage is ShortsFeedPage.Video) {
                viewModel.ensurePlayback(prevFeedPage.itemIndex, backendBaseUrl)
            }
        }
    }

    LaunchedEffect(backendBaseUrl, initialFilmId, initialEpisodeNumber) {
        viewModel.loadInitial(backendBaseUrl, initialFilmId, initialEpisodeNumber)
    }

    var initialPageScrolled by remember(initialFilmId, initialEpisodeNumber) { mutableStateOf(false) }
    LaunchedEffect(uiState.items, initialFilmId, initialEpisodeNumber) {
        if (!initialPageScrolled && initialFilmId != null && uiState.items.isNotEmpty()) {
            val targetEp = initialEpisodeNumber
                ?: viewModel.getLastWatchedEpisode(initialFilmId)
                ?: 1
            val pageIndex = feedPages.indexOfFirst { page ->
                page is ShortsFeedPage.Video && page.item.episodeNumber == targetEp
            }
            if (pageIndex > 0 && pageIndex < feedPages.size) {
                pagerState.scrollToPage(pageIndex)
            }
            initialPageScrolled = true
        }
    }
    
    // Track if any video is showing an ad to pause playback
    var isAnyVideoShowingAd by remember { mutableStateOf(false) }
    
    // Pause video when ad is showing
    LaunchedEffect(isAnyVideoShowingAd) {
        if (isAnyVideoShowingAd) {
            isPlaying = false
        }
    }

    // --- Immersive mode: hide system bars in episode mode ---
    val isEpisodeEntry = initialFilmId != null
    DisposableEffect(isEpisodeEntry) {
        val window = activity?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        if (isEpisodeEntry && insetsController != null) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            // Restore system bars when leaving ShortsScreen
            insetsController?.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ShortsBackground)
    ) {
        if (uiState.items.isEmpty()) {
            ShortsSkeleton()
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp
            ) { page ->
                when (val feedPage = feedPages[page]) {
                    ShortsFeedPage.NativeFullscreenAd -> ShortVideoNativeFullscreenAd(
                        state = nativeShortVideoAdState,
                        isActive = page == pagerState.currentPage
                    )

                    is ShortsFeedPage.Video -> {
                        val isEpisodeModeForItem = initialFilmId != null || episodeModeKeys.contains(feedPage.item.episodeKey())
                        ShortsPage(
                        item = feedPage.item,
                        itemIndex = feedPage.itemIndex,
                        isActive = page == pagerState.currentPage,
                        isEpisodeMode = isEpisodeModeForItem,
                        backendBaseUrl = backendBaseUrl,
                        controlsVisible = controlsVisible,
                        isPlaying = isPlaying,
                        showPlaybackOptions = showPlaybackOptions,
                        showFeedbackForm = showFeedbackForm,
                        autoNext = autoNext,
                        autoUnlock = autoUnlock,
                        ccEnabled = ccEnabled,
                        playbackSpeed = playbackSpeed,
                        bottomReservedPadding = if (isEpisodeModeForItem) 0.dp else 78.dp + bottomBannerPadding,
                        onBack = onBack,
                        onTogglePlay = {
                            controlsVisible = true
                            isPlaying = !isPlaying
                        },
                        onFeedbackClick = {
                            controlsVisible = true
                            showFeedbackForm = !showFeedbackForm
                            showPlaybackOptions = false
                        },
                        onOptionsClick = {
                            controlsVisible = true
                            showPlaybackOptions = !showPlaybackOptions
                            showFeedbackForm = false
                        },
                        onClosePopups = {
                            showPlaybackOptions = false
                            showFeedbackForm = false
                        },
                        onAutoUnlockChange = { enabled ->
                            autoUnlock = enabled
                        },
                        onAutoNextChange = { enabled ->
                            autoNext = enabled
                        },
                        onSubmitFeedback = { item, message ->
                            viewModel.sendFeedback(
                                backendBaseUrl = backendBaseUrl,
                                filmId = item.film.id,
                                episodeNumber = item.episodeNumber,
                                message = message
                            )
                        },
                        onLikeClick = { item, liked ->
                            viewModel.setEpisodeLike(
                                backendBaseUrl = backendBaseUrl,
                                filmId = item.film.id,
                                episodeNumber = item.episodeNumber,
                                liked = liked
                            )
                        },
                        onReminderClick = { item, enabled ->
                            viewModel.setReminder(
                                backendBaseUrl = backendBaseUrl,
                                film = item.film,
                                enabled = enabled
                            )
                        },
                        onEpisodeFinished = { index, item, position, duration ->
                            viewModel.completeEpisodeAndMaybePlayNext(
                                backendBaseUrl = backendBaseUrl,
                                itemIndex = index,
                                item = item,
                                progressSeconds = (position / 1000).toInt(),
                                durationSeconds = duration.takeIf { it > 0L }?.let { (it / 1000).toInt() },
                                autoNext = autoNext,
                                autoUnlock = autoUnlock
                            )
                        },
                        onProgressCheckpoint = { item, position, duration ->
                            viewModel.saveWatchProgress(
                                backendBaseUrl = backendBaseUrl,
                                item = item,
                                progressSeconds = (position / 1000).toInt(),
                                durationSeconds = duration.takeIf { it > 0L }?.let { (it / 1000).toInt() }
                            )
                        },
                        onToggleCc = { ccEnabled = !ccEnabled },
                        onCycleSpeed = {
                            playbackSpeed = when (playbackSpeed) {
                                0.75f -> 1f
                                1f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 1.75f
                                1.75f -> 2f
                                else -> 0.75f
                            }
                        },
                        dailyUnlocksUsed = dailyUnlocksUsed,
                        dailyUnlockLimit = DAILY_UNLOCK_LIMIT,
                        isEpisodeUnlockedLocally = { filmId, episodeNumber ->
                            unlockedEpisodeKeys.contains("$filmId:$episodeNumber")
                        },
                        unlockedEpisodeKeys = unlockedEpisodeKeys,
                        onUnlockedEpisodeReady = { targetItem ->
                            episodeModeKeys = episodeModeKeys + targetItem.episodeKey()
                            isPlaying = true
                            viewModel.playEpisode(
                                backendBaseUrl = backendBaseUrl,
                                itemIndex = feedPage.itemIndex,
                                currentItem = feedPage.item,
                                episodeNumber = targetItem.episodeNumber
                            )
                        },
                        onWatchAdToUnlock = { targetItem, onDone ->
                            activity?.let { act ->
                                AdsManager.loadAndShowRewardAll(
                                    activity = act,
                                    onRewardEarned = {
                                        dailyUnlocksUsed++
                                        unlockedEpisodeKeys = unlockedEpisodeKeys + "${targetItem.film.id}:${targetItem.episodeNumber}"
                                        viewModel.unlockEpisode(
                                            backendBaseUrl = backendBaseUrl,
                                            filmId = targetItem.film.id,
                                            episodeNumber = targetItem.episodeNumber
                                        )
                                        onDone(true)
                                    },
                                    onFinished = {
                                        // Ad display attempt completed
                                    }
                                )
                            } ?: run {
                                // No activity available, skip ad and unlock directly (fallback)
                                dailyUnlocksUsed++
                                unlockedEpisodeKeys = unlockedEpisodeKeys + "${targetItem.film.id}:${targetItem.episodeNumber}"
                                viewModel.unlockEpisode(
                                    backendBaseUrl = backendBaseUrl,
                                    filmId = targetItem.film.id,
                                    episodeNumber = targetItem.episodeNumber
                                )
                                onDone(true)
                            }
                        },
                        onHideControls = {
                            controlsVisible = false
                        }
                    )
                    }
                }
            }
        }
        val currentVideoItem = currentVideoPage?.item
        val isCurrentEpisodeMode = isEpisodeEntry || currentVideoItem?.let { episodeModeKeys.contains(it.episodeKey()) } == true
        if (controlsVisible && currentFeedPage is ShortsFeedPage.Video && !isCurrentEpisodeMode) {
            BottomNavigationBar(
                selected = "Shorts",
                onHome = onHome,
                onShorts = {},
                onLibrary = onLibrary,
                onRewards = onRewards,
                onProfile = onProfile,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomBannerPadding)
            )
        }
        if (bottomBannerVisible && currentFeedPage is ShortsFeedPage.Video && !isCurrentEpisodeMode) {
            AppBottomBanner(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

private fun List<ShortsItem>.withNativeAdPages(): List<ShortsFeedPage> =
    buildList {
        this@withNativeAdPages.forEachIndexed { index, item ->
            add(ShortsFeedPage.Video(item, index))
            if ((index + 1) % 3 == 0) {
                add(ShortsFeedPage.NativeFullscreenAd)
            }
        }
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun ShortsItem.episodeKey(): String = "${film.id}:${episodeNumber}"

@Composable
private fun ShortVideoNativeFullscreenAd(
    state: NativeAdState,
    isActive: Boolean
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ShortsBackground)
    ) {
        if (isActive) {
            ErainNativeAdHost(
                placementName = "native_shortvideo_fullscreen",
                state = state,
                modifier = Modifier.fillMaxSize(),
                height = maxHeight,
                fillAvailableHeight = true
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ShortsPage(
    item: ShortsItem,
    itemIndex: Int,
    isActive: Boolean,
    isEpisodeMode: Boolean,
    backendBaseUrl: String,
    controlsVisible: Boolean,
    isPlaying: Boolean,
    showPlaybackOptions: Boolean,
    showFeedbackForm: Boolean,
    autoNext: Boolean,
    autoUnlock: Boolean,
    ccEnabled: Boolean,
    playbackSpeed: Float,
    bottomReservedPadding: Dp,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onFeedbackClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onClosePopups: () -> Unit,
    onAutoNextChange: (Boolean) -> Unit,
    onAutoUnlockChange: (Boolean) -> Unit,
    onSubmitFeedback: (ShortsItem, String) -> Unit,
    onLikeClick: (ShortsItem, Boolean) -> Unit,
    onReminderClick: (ShortsItem, Boolean) -> Unit,
    onEpisodeFinished: (Int, ShortsItem, Long, Long) -> Unit,
    onProgressCheckpoint: (ShortsItem, Long, Long) -> Unit,
    onToggleCc: () -> Unit,
    onCycleSpeed: () -> Unit,
    dailyUnlocksUsed: Int,
    dailyUnlockLimit: Int,
    isEpisodeUnlockedLocally: (filmId: Int, episodeNumber: Int) -> Boolean,
    unlockedEpisodeKeys: Set<String>,
    onUnlockedEpisodeReady: (ShortsItem) -> Unit,
    onWatchAdToUnlock: (ShortsItem, onDone: (Boolean) -> Unit) -> Unit,
    onHideControls: () -> Unit = {},
) {
    var videoReady by remember(item.playUrl) { mutableStateOf(false) }
    var reminderOn by remember(item.film.id) { mutableStateOf(false) }
    var liked by remember(item.film.id, item.episodeNumber) { mutableStateOf(false) }
    var positionMs by remember(item.playUrl) { mutableStateOf(0L) }
    var durationMs by remember(item.playUrl) { mutableStateOf(0L) }
    var finishHandled by remember(item.playUrl) { mutableStateOf(false) }
    var lastProgressSaveMs by remember(item.playUrl) { mutableStateOf(0L) }
    var pendingSeekMs by remember(item.playUrl) { mutableStateOf<Long?>(null) }
    var subtitleText by remember(item.playUrl) { mutableStateOf("") }
    var feedbackText by remember(item.playUrl) { mutableStateOf("") }
    var selectedSubtitleUrl by remember(item.playUrl) {
        mutableStateOf(item.subtitleTracks.preferredEnglishSubtitleUrl())
    }
    var showSubtitleOptions by remember(item.playUrl) { mutableStateOf(false) }
    var showEpisodeOptions by remember(item.film.id) { mutableStateOf(false) }
    val subtitleUrlForPlayback = selectedSubtitleUrl.ifBlank { item.subtitleTracks.firstOrNull()?.url.orEmpty() }
    val fallbackSubtitleCues by produceState<List<SubtitleCue>>(initialValue = emptyList(), subtitleUrlForPlayback) {
        value = if (subtitleUrlForPlayback.isBlank()) emptyList() else loadSubtitleCues(subtitleUrlForPlayback)
    }
    val fallbackSubtitleText = fallbackSubtitleCues
        .firstOrNull { cue -> positionMs in cue.startMs..cue.endMs }
        ?.text
        .orEmpty()
    val context = LocalContext.current
    val savedToListText = stringResource(R.string.saved_to_list)
    val removedFromListText = stringResource(R.string.removed_from_list)
    val feedbackSentText = stringResource(R.string.feedback_sent)
    val hasPopup = showPlaybackOptions || showFeedbackForm || showSubtitleOptions || showEpisodeOptions
    var subtitleSize by remember(item.playUrl) { mutableStateOf(SubtitleSize.SMALL) }
    var selectedReportReason by remember(item.playUrl) { mutableStateOf("") }
    var showShareSheet by remember(item.film.id) { mutableStateOf(false) }

    var unlockTargetItem by remember(item.film.id, item.episodeNumber) { mutableStateOf<ShortsItem?>(null) }
    var showDailyLimitDialog by remember { mutableStateOf(false) }
    var isWatchingAd by remember(item.film.id, item.episodeNumber) { mutableStateOf(false) }

    val isLocked = item.isPaywalled() && !isEpisodeUnlockedLocally(item.film.id, item.episodeNumber)
    val fullEpisodeTarget = remember(item, isLocked) {
        if (!isLocked && item.episodeNumber == 1 && item.film.episodeTotal > 1) {
            item.copy(episodeNumber = 2, isLocked = true)
        } else {
            item
        }
    }

    // Auto-hide overlay after 5 seconds of playback in Episode mode
    LaunchedEffect(isEpisodeMode, isPlaying, isActive, controlsVisible) {
        if (isEpisodeMode && isPlaying && isActive && controlsVisible) {
            delay(5000L)
            if (isActive && controlsVisible) { // Double-check still active and controls visible
                onHideControls()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isActive || item.playUrl.isBlank() || isLocked) {
            LoadingBackdrop(
                item = item,
                showLoader = isActive && !isLocked,
                modifier = Modifier
                    .fillMaxSize()
            )
        } else {
            HlsVideoPlayer(
                playUrl = item.playUrl,
                subtitleTracks = item.subtitleTracks,
                selectedSubtitleUrl = subtitleUrlForPlayback,
                isPlaying = isPlaying,
                ccEnabled = ccEnabled,
                controlsVisible = controlsVisible,
                playbackSpeed = playbackSpeed,
                repeatCurrent = !autoNext,
                onReady = { videoReady = true },
                onProgress = { position, duration ->
                    positionMs = position
                    durationMs = duration
                    if (position >= 10_000L && position - lastProgressSaveMs >= 5_000L) {
                        lastProgressSaveMs = position
                        onProgressCheckpoint(item, position, duration)
                    }
                },
                onEnded = {
                    if (!finishHandled) {
                        finishHandled = true
                        onEpisodeFinished(itemIndex, item, positionMs, durationMs)
                    }
                },
                onSubtitleText = { subtitleText = it },
                seekToMs = pendingSeekMs,
                onSeekHandled = { pendingSeekMs = null },
                modifier = Modifier.fillMaxSize()
            )
            if (!videoReady) {
                LoadingBackdrop(
                    item = item,
                    showLoader = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .clickable(onClick = onTogglePlay)
//        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = {
                    if (isLocked) {
                        if (dailyUnlocksUsed >= dailyUnlockLimit) {
                            showDailyLimitDialog = true
                        } else {
                            unlockTargetItem = fullEpisodeTarget
                        }
                    } else {
                        onTogglePlay()
                    }
                })
        )

        if (controlsVisible) {
            ShortsOverlayGradient()
        }

        if (controlsVisible && !isPlaying && videoReady) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .padding(18.dp)
            )
        }

        if (controlsVisible) {
            SharedVideoTopBar(
                item = item,
                showActions = isEpisodeMode,
                onBack = onBack,
                onFeedbackClick = onFeedbackClick,
                onOptionsClick = onOptionsClick
            )

            SharedVideoSidebar(
                liked = liked,
                likeCount = displayLikeCount(item),
                bookmarked = reminderOn,
                saveCount = displaySaveCount(item),
                ccEnabled = ccEnabled,
                playbackSpeed = playbackSpeed,
                isEpisodeMode = isEpisodeMode,
                modifier = Modifier.align(Alignment.BottomEnd),
                onLikeClick = { newLiked ->
                    liked = newLiked
                    onLikeClick(item, newLiked)
                },
                onBookmarkClick = { newBookmarked ->
                    reminderOn = newBookmarked
                    onReminderClick(item, newBookmarked)
                    Toast
                        .makeText(context, if (newBookmarked) savedToListText else removedFromListText, Toast.LENGTH_SHORT)
                        .show()
                },
                onShareClick = { showShareSheet = true },
                onEpisodesClick = { showEpisodeOptions = true },
                onCcClick = {
                    if (item.subtitleTracks.size > 1) {
                        if (ccEnabled) {
                            onToggleCc()
                            showSubtitleOptions = false
                        } else {
                            if (selectedSubtitleUrl.isBlank()) {
                                selectedSubtitleUrl = item.subtitleTracks.firstOrNull()?.url.orEmpty()
                            }
                            onToggleCc()
                            showSubtitleOptions = true
                        }
                    } else {
                        if (!ccEnabled && selectedSubtitleUrl.isBlank()) {
                            selectedSubtitleUrl = item.subtitleTracks.firstOrNull()?.url.orEmpty()
                        }
                        onToggleCc()
                        showSubtitleOptions = false
                    }
                },
                onSpeedClick = onCycleSpeed,
                bottomReservedPadding = bottomReservedPadding
            )

            Box(modifier = Modifier.align(Alignment.BottomStart)) {
                SharedVideoCaption(
                    item = item,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isLocked = isLocked,
                    isEpisodeMode = isEpisodeMode,
                    bottomReservedPadding = bottomReservedPadding,
                    onSeekTo = { targetMs ->
                        positionMs = targetMs
                        pendingSeekMs = targetMs
                    },
                    onWatchNowClick = {
                        // When clicking "Watch Now" from Shorts view, open the full episode list in Episode mode
                        // This should navigate to Episode mode, not show an unlock dialog
                        if (isEpisodeMode) {
                            // Already in episode mode, just navigate to next episode normally
                            if (isLocked) {
                                if (dailyUnlocksUsed >= dailyUnlockLimit) {
                                    showDailyLimitDialog = true
                                } else {
                                    unlockTargetItem = fullEpisodeTarget
                                }
                            } else {
                                // Current episode not locked, play the full episode version
                                onUnlockedEpisodeReady(item)
                            }
                        } else {
                            // Not in episode mode - "Watch Now" should open Episode mode starting from Episode 1
                            // The item should be Episode 1 (or current episode)
                            val episodeToPlay = item.copy(episodeNumber = 1, isLocked = 1 > FREE_SHORTS_PREVIEW_EPISODES)
                            onUnlockedEpisodeReady(episodeToPlay)
                        }
                    }
                )
            }
        }

        val visibleSubtitleText = subtitleText.ifBlank { fallbackSubtitleText }
        if (ccEnabled && visibleSubtitleText.isNotBlank()) {
            ComposeSubtitleOverlay(
                text = visibleSubtitleText,
                controlsVisible = controlsVisible,
                fontSize = subtitleSize.toFontSizeSp(),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (hasPopup && controlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        showSubtitleOptions = false
                        showEpisodeOptions = false
                        onClosePopups()
                    }
            )
        }

        if (showPlaybackOptions && controlsVisible) {
            FeedbackOptionsSheet(
                autoNext = autoNext,
                autoUnlock = autoUnlock,
                onAutoNextChange = onAutoNextChange,
                onAutoUnlockChange = onAutoUnlockChange,
                onDismiss = { onClosePopups() }
            )
        }

        if (showFeedbackForm && controlsVisible) {
            FeedbackFormSheet(
                filmTitle = item.film.title,
                episodeNumber = item.episodeNumber,
                thumbnailUrl = item.film.imageUrl,
                selectedReason = selectedReportReason,
                onReasonSelected = { selectedReportReason = it },
                onSubmit = {
                    onSubmitFeedback(item, selectedReportReason)
                    Toast.makeText(context, feedbackSentText, Toast.LENGTH_SHORT).show()
                    selectedReportReason = ""
                    onClosePopups()
                },
                onDismiss = { onClosePopups() }
            )
        }

        if (showSubtitleOptions && controlsVisible) {
            SubtitleOptionsSheet(
                tracks = item.subtitleTracks,
                selectedUrl = selectedSubtitleUrl,
                subtitleSize = subtitleSize,
                onSelect = { track ->
                    selectedSubtitleUrl = track.url
                },
                onSizeChange = { size ->
                    subtitleSize = size
                },
                onSave = {
                    showSubtitleOptions = false
                },
                onDismiss = {
                    showSubtitleOptions = false
                }
            )
        }

        if (showEpisodeOptions && controlsVisible) {
            // Calculate the highest consecutively unlocked episode
            val maxUnlockedConsecutive = run {
                var maxUnlocked = FREE_SHORTS_PREVIEW_EPISODES
                for (ep in (FREE_SHORTS_PREVIEW_EPISODES + 1)..item.film.episodeTotal) {
                    if (isEpisodeUnlockedLocally(item.film.id, ep)) {
                        maxUnlocked = ep
                    } else {
                        break
                    }
                }
                maxUnlocked
            }
            
            val mustUnlockFirstMessage = stringResource(R.string.must_unlock_episode_first, maxUnlockedConsecutive + 1)
            
            EpisodeOptionsSheet(
                currentEpisode = item.episodeNumber,
                totalEpisodes = item.film.episodeTotal,
                unlockedThrough = maxUnlockedConsecutive,
                onEpisodeSelected = { episode ->
                    val targetItem = item.copy(
                        episodeNumber = episode,
                        isLocked = episode > FREE_SHORTS_PREVIEW_EPISODES
                    )
                    if (episode > FREE_SHORTS_PREVIEW_EPISODES &&
                        !isEpisodeUnlockedLocally(item.film.id, episode)
                    ) {
                        // Check if trying to unlock a non-consecutive episode
                        if (episode > maxUnlockedConsecutive + 1) {
                            // User is trying to skip locked episodes
                            Toast.makeText(context, mustUnlockFirstMessage, Toast.LENGTH_LONG).show()
                        } else if (dailyUnlocksUsed >= dailyUnlockLimit) {
                            showDailyLimitDialog = true
                        } else {
                            unlockTargetItem = targetItem
                        }
                    } else {
                        onUnlockedEpisodeReady(targetItem)
                    }
                },
                modifier = Modifier.align(Alignment.Center),
                onDismiss = { showEpisodeOptions = false }
            )
        }
        if (showShareSheet) {
            ShareOptionsSheet(
                shareText = buildShareText(item, context),
                onDismiss = { showShareSheet = false }
            )
        }
        val currentUnlockTarget = unlockTargetItem
        if (currentUnlockTarget != null) {
            UnlockEpisodeDialog(
                posterUrl = currentUnlockTarget.film.imageUrl,
                episodeNumber = currentUnlockTarget.episodeNumber,
                dailyUnlocksUsed = dailyUnlocksUsed,
                dailyUnlockLimit = dailyUnlockLimit,
                isLoading = isWatchingAd,
                onWatchAd = {
                    isWatchingAd = true
                    onWatchAdToUnlock(currentUnlockTarget) { unlocked ->
                        isWatchingAd = false
                        if (unlocked) {
                            unlockTargetItem = null
                            onUnlockedEpisodeReady(currentUnlockTarget)
                        }
                    }
                },
                onDismiss = {
                    if (!isWatchingAd) unlockTargetItem = null
                }
            )
        }

        if (showDailyLimitDialog) {
            DailyLimitReachedDialog(
                dailyUnlockLimit = dailyUnlockLimit,
                onBrowseFreeEpisodes = {
                    showDailyLimitDialog = false
                    showEpisodeOptions = true
                },
                onDismiss = { showDailyLimitDialog = false }
            )
        }
    }
}

@Composable
private fun ShortsOverlayGradient() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color(0xE60B0B0D),
                    0.18f to Color(0x660B0B0D),
                    0.42f to Color.Transparent,
                    0.66f to Color.Transparent,
                    1.0f to Color(0xF20B0B0D)
                )
            )
    )
}

@Composable
private fun ShortsCaption(
    item: ShortsItem,
    positionMs: Long,
    durationMs: Long,
    isLocked: Boolean,
    isEpisodeMode: Boolean,
    bottomReservedPadding: Dp,
    onSeekTo: (Long) -> Unit,
    onWatchNowClick: () -> Unit
) {
    val description = item.film.description.ifBlank { stringResource(R.string.default_short_description) }
    var descriptionExpanded by remember(item.film.id, item.episodeNumber) { mutableStateOf(false) }
    val showDescriptionToggle = description.length > 70
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = if (isLocked) 16.dp else 90.dp, bottom = bottomReservedPadding + 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.episode_progress,
                    item.episodeNumber,
                    item.film.episodeTotal
                ),
                color = Gold,
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
            Text(stringResource(R.string.trending_number), color = Color(0xFFE5BDBE), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
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
                text = if (descriptionExpanded) stringResource(R.string.view_less) else "... ${stringResource(R.string.view_more)}",
                color = Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
                modifier = Modifier.clickable { descriptionExpanded = !descriptionExpanded }
            )
        }
        Spacer(Modifier.height(10.dp))
        if (isEpisodeMode) {
            ThinSeekBar(
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
                    formatPlaybackTime(positionMs),
                    color = Color(0x99E5BDBE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                Text(
                    formatPlaybackTime(durationMs),
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
                    .background(Pink)
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
private fun ComposeSubtitleOverlay(
    text: String,
    controlsVisible: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = fontSize,
        lineHeight = fontSize.value.times(1.25f).sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        letterSpacing = 0.sp,
        modifier = modifier
            .padding(bottom = if (controlsVisible) 356.dp else 58.dp)
            .widthIn(max = 330.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xB8000000))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun SubtitleSize.toFontSizeSp(): androidx.compose.ui.unit.TextUnit = when (this) {
    SubtitleSize.SMALL -> 14.sp
    SubtitleSize.MEDIUM -> 18.sp
    SubtitleSize.LARGE -> 22.sp
}

@Composable
private fun ThinSeekBar(
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
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(24.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Pink)
            )
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Pink, CircleShape)
            )
        }
    }
}

@Composable
private fun LoadingBackdrop(
    item: ShortsItem,
    showLoader: Boolean,
    modifier: Modifier
) {
    Box(modifier = modifier.background(ShortsBackground)) {
        ShortsThumbnail(item.film.imageUrl, Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x66000000), Color(0xCC050507))))
        )
        if (showLoader) {
            CircularProgressIndicator(
                color = Gold,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
            )
            LinearProgressIndicator(
                color = Pink,
                trackColor = Color(0x33000000),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}


@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun HlsVideoPlayer(playUrl: String, modifier: Modifier) {
    HlsVideoPlayer(
        playUrl = playUrl,
        subtitleTracks = emptyList(),
        selectedSubtitleUrl = "",
        isPlaying = true,
        ccEnabled = false,
        controlsVisible = false,
        playbackSpeed = 1f,
        repeatCurrent = true,
        onReady = {},
        onProgress = { _, _ -> },
        onEnded = {},
        onSubtitleText = {},
        seekToMs = null,
        onSeekHandled = {},
        modifier = modifier
    )
}


@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@androidx.media3.common.util.UnstableApi
@Composable
private fun HlsVideoPlayer(
    playUrl: String,
    subtitleTracks: List<SubtitleTrack>,
    selectedSubtitleUrl: String,
    isPlaying: Boolean,
    ccEnabled: Boolean,
    controlsVisible: Boolean,
    playbackSpeed: Float,
    repeatCurrent: Boolean,
    onReady: () -> Unit,
    onProgress: (Long, Long) -> Unit,
    onEnded: () -> Unit,
    onSubtitleText: (String) -> Unit,
    seekToMs: Long?,
    onSeekHandled: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val latestCcEnabled by rememberUpdatedState(ccEnabled)
    val selectedSubtitleTrack = subtitleTracks.firstOrNull { it.url == selectedSubtitleUrl }
        ?: subtitleTracks.firstOrNull()
    val trackSelector = remember(playUrl) {
        DefaultTrackSelector(context)
    }
    val player = remember(playUrl) {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
    }

    DisposableEffect(playUrl) {
        val subtitleConfigurations = subtitleTracks
            .filter { it.url.isNotBlank() }
            .map { track ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
                    .setMimeType(subtitleMimeType(track.url))
                    .setLanguage(track.language.ifBlank { "en" })
                    .setSelectionFlags(0)
                    .build()
            }
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(playUrl))
            .setSubtitleConfigurations(subtitleConfigurations)
            .build()
        player.setMediaItem(mediaItem)
        player.repeatMode = if (repeatCurrent) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
        player.playWhenReady = true
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                if (!latestCcEnabled) {
                    onSubtitleText("")
                    return
                }
                onSubtitleText(
                    cueGroup.cues
                        .joinToString("\n") { it.text?.toString().orEmpty() }
                        .trim()
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) onReady()
                if (playbackState == Player.STATE_ENDED) onEnded()
            }

            override fun onPlayerError(error: PlaybackException) {
                onReady()
            }
        }
        player.addListener(listener)
        player.prepare()
        onDispose {
            onSubtitleText("")
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(ccEnabled, selectedSubtitleTrack?.language, selectedSubtitleTrack?.url) {
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !ccEnabled)
                .setPreferredTextLanguage(selectedSubtitleTrack?.language?.ifBlank { "en" } ?: "en")
                .setSelectUndeterminedTextLanguage(true)
        )
        if (!ccEnabled) onSubtitleText("")
    }

    LaunchedEffect(isPlaying) {
        player.playWhenReady = isPlaying
        if (isPlaying) player.play() else player.pause()
    }

    LaunchedEffect(playbackSpeed) {
        player.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    LaunchedEffect(repeatCurrent) {
        player.repeatMode = if (repeatCurrent) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
    }

    LaunchedEffect(seekToMs) {
        val target = seekToMs ?: return@LaunchedEffect
        player.seekTo(target.coerceAtLeast(0L))
        onSeekHandled()
    }

    LaunchedEffect(player, playUrl) {
        while (true) {
            val duration = player.duration.takeIf { it > 0L && it < Long.MAX_VALUE / 2 } ?: 0L
            onProgress(player.currentPosition.coerceAtLeast(0L), duration)
            delay(500)
        }
    }

    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = {
            (LayoutInflater.from(context).inflate(R.layout.view_shorts_player, null) as PlayerView).apply {
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setEnableComposeSurfaceSyncWorkaround(true)
                subtitleView?.visibility = View.GONE
                this.player = player
            }
        },
        update = { playerView ->
            if (playerView.player !== player) {
                playerView.player = player
            }
            playerView.subtitleView?.visibility = View.GONE
        }
    )
}

@Composable
private fun ShortsThumbnail(
    imageUrl: String,
    modifier: Modifier
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, imageUrl) {
        value = if (imageUrl.isBlank()) null else loadShortsBitmap(imageUrl)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(Color(0xFF25121C), Color(0xFF101C2A), Color(0xFF050507))
                )
            )
        )
    }
}

private suspend fun loadShortsBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        URL(imageUrl).openStream().use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}

private suspend fun loadSubtitleCues(subtitleUrl: String): List<SubtitleCue> = withContext(Dispatchers.IO) {
    runCatching {
        URL(subtitleUrl).openStream().bufferedReader().use { reader ->
            parseSubtitleCues(reader.readText())
        }
    }.getOrDefault(emptyList())
}

private fun parseSubtitleCues(raw: String): List<SubtitleCue> {
    val normalized = raw
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lineSequence()
        .dropWhile { it.trim().equals("WEBVTT", ignoreCase = true) || it.trim().startsWith("NOTE") }
        .joinToString("\n")
    return normalized.split(Regex("\n{2,}"))
        .mapNotNull { block ->
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            val timeIndex = lines.indexOfFirst { "-->" in it }
            if (timeIndex < 0) return@mapNotNull null
            val times = lines[timeIndex].split("-->")
            val start = times.getOrNull(0)?.subtitleTimeToMs() ?: return@mapNotNull null
            val end = times.getOrNull(1)?.substringBefore(' ')?.subtitleTimeToMs() ?: return@mapNotNull null
            val text = lines.drop(timeIndex + 1)
                .joinToString("\n")
                .replace(Regex("<[^>]+>"), "")
                .trim()
            if (text.isBlank()) null else SubtitleCue(start, end, text)
        }
}

private fun String.subtitleTimeToMs(): Long? {
    val clean = trim().replace(',', '.')
    val parts = clean.split(":")
    val secondsPart = parts.lastOrNull() ?: return null
    val seconds = secondsPart.substringBefore('.').toLongOrNull() ?: return null
    val millis = secondsPart.substringAfter('.', "").padEnd(3, '0').take(3).toLongOrNull() ?: 0L
    val minutes = parts.getOrNull(parts.size - 2)?.toLongOrNull() ?: 0L
    val hours = if (parts.size >= 3) parts.getOrNull(parts.size - 3)?.toLongOrNull() ?: 0L else 0L
    return hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L + millis
}

@Composable
private fun SideTextAction(
    value: String,
    label: Int,
    onClick: () -> Unit = {}
) {
    SideTextAction(value = value, label = stringResource(label), onClick = onClick)
}

@Composable
private fun SideTextAction(
    value: String,
    label: String,
    onClick: () -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0x8A111114))
                .border(1.dp, Color(0x26FFFFFF), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color(0xFFF2D7DD), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
    }
}

private fun speedLabel(speed: Float): String {
    return when (speed) {
        0.75f -> "0.75x"
        1f -> "1x"
        1.25f -> "1.25x"
        1.5f -> "1.5x"
        1.75f -> "1.75x"
        2f -> "2x"
        else -> "${speed}x"
    }
}

private fun displayLikeCount(item: ShortsItem): Int {
    val backendCount = item.likeCount.takeIf { it > 0 } ?: item.film.likeCount
    if (backendCount > 0) return backendCount
    val seed = (item.film.id.takeIf { it != 0 } ?: item.film.title.hashCode()).let { kotlin.math.abs(it) }
    return 1100 + (seed % 42000)
}

private fun displaySaveCount(item: ShortsItem): Int {
    val seedSource = item.film.id.takeIf { it != 0 } ?: item.film.title.hashCode()
    val seed = kotlin.math.abs(seedSource * 31 + item.film.episodeTotal)
    return 700 + (seed % 26000)
}

private fun List<SubtitleTrack>.preferredEnglishSubtitleUrl(): String {
    return firstOrNull { track ->
        track.language.equals("en", ignoreCase = true) ||
            track.label.equals("English", ignoreCase = true)
    }?.url ?: firstOrNull()?.url.orEmpty()
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> {
            val value = count / 100_000f
            "${(value / 10f).toCleanDecimal()}M"
        }
        count >= 1_000 -> {
            val value = count / 100f
            "${(value / 10f).toCleanDecimal()}k"
        }
        else -> count.toString()
    }
}

private fun Float.toCleanDecimal(): String {
    val oneDecimal = "%.1f".format(java.util.Locale.US, this)
    return oneDecimal.removeSuffix(".0")
}

private fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun buildShareText(item: ShortsItem, context: android.content.Context): String {
    return buildString {
        append("Watch ")
        append(item.film.title.ifBlank { "this DramaX short" })
        append(" Episode ")
        append(item.episodeNumber)
        append(" on DramaX.")
        append("\n\n")
        append("Get the app: https://play.google.com/store/apps/details?id=")
        append(context.packageName)
    }
}

private fun ShortsItem.isPaywalled(): Boolean {
    return isLocked || episodeNumber > FREE_SHORTS_PREVIEW_EPISODES
}

private fun subtitleMimeType(url: String): String {
    val lower = url.substringBefore('?').lowercase()
    return when {
        lower.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
        lower.endsWith(".ttml") || lower.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
        lower.endsWith(".ssa") || lower.endsWith(".ass") -> MimeTypes.TEXT_SSA
        else -> MimeTypes.TEXT_VTT
    }
}

@Composable
private fun ShortsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1117), Color(0xFF050507))))
            .padding(PaddingValues(horizontal = 18.dp, vertical = 24.dp))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonPiece(38, 38, CircleShape)
            Spacer(Modifier.weight(1f))
            SkeletonPiece(28, 28, CircleShape)
        }
        Spacer(Modifier.weight(1f))
        SkeletonPiece(210, 28, RoundedCornerShape(8.dp))
        Spacer(Modifier.height(12.dp))
        SkeletonPiece(320, 62, RoundedCornerShape(8.dp))
        Spacer(Modifier.height(24.dp))
        SkeletonPiece(180, 48, RoundedCornerShape(12.dp))
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun SkeletonPiece(
    width: Int,
    height: Int,
    shape: androidx.compose.ui.graphics.Shape
) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
            .clip(shape)
            .background(Color(0xFF242027))
    )
}

private const val EPISODES_PER_PAGE = 30
private const val EPISODES_PER_ROW = 5

@Composable
private fun EpisodeOptionsSheet(
    currentEpisode: Int,
    totalEpisodes: Int,
    unlockedThrough: Int = currentEpisode,
    onEpisodeSelected: (Int) -> Unit = {},
    modifier: Modifier,
    onDismiss: () -> Unit
) {
    val safeTotal = totalEpisodes.coerceAtLeast(1)
    var page by remember { mutableStateOf((currentEpisode - 1) / EPISODES_PER_PAGE) }
    val pageStart = page * EPISODES_PER_PAGE + 1
    val pageEnd = (pageStart + EPISODES_PER_PAGE - 1).coerceAtMost(safeTotal)
    val episodes = remember(page, safeTotal) { (pageStart..pageEnd).toList() }
    val hasNextPage = pageEnd < safeTotal
    val hasPrevPage = page > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF16121A))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.episodes),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(
                        R.string.episode_count,
                        currentEpisode,
                        safeTotal
                    ),
                    color = Color(0xFF9D8A91),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(18.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(EPISODES_PER_ROW),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(episodes, key = { it }) { episode ->
                    EpisodeCell(
                        episode = episode,
                        isPlaying = episode == currentEpisode,
                        isLocked = episode > unlockedThrough,
                        onClick = {
                            onEpisodeSelected(episode)
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (hasPrevPage) {
                    PagerPill(
                        text = stringResource(
                            R.string.episodes_range,
                            (page - 1) * EPISODES_PER_PAGE + 1,
                            page * EPISODES_PER_PAGE
                        ),
                        leading = true,
                        onClick = { page-- }
                    )
                    if (hasNextPage) Spacer(Modifier.width(10.dp))
                }
                if (hasNextPage) {
                    PagerPill(
                        text = stringResource(
                            R.string.episodes_range,
                            pageEnd + 1,
                            (pageEnd + EPISODES_PER_PAGE).coerceAtMost(safeTotal)
                        ),
                        leading = false,
                        onClick = { page++ }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeCell(
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
                if (isPlaying)
                    Modifier.border(1.5.dp, Gold, RoundedCornerShape(14.dp))
                else Modifier
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
        if(isLocked){
            Spacer(Modifier.weight(1f))
        }
        Text(
            episode.toString(),
            color = if (isPlaying) Gold else if (isLocked) Color(0xFF9D8FA0) else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
//        if (isPlaying) {
//            Text(
//                "PLAYING",
//                color = Gold,
//                fontSize = 8.sp,
//                fontWeight = FontWeight.Black,
//                letterSpacing = 0.5.sp
//            )
//        } else {
//            Spacer(Modifier.height(10.dp))
//        }
//        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PagerPill(
    text: String,
    leading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF262129))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (!leading) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}


enum class SubtitleSize { SMALL, MEDIUM, LARGE }

@Composable
private fun SubtitleOptionsSheet(
    tracks: List<SubtitleTrack>,
    selectedUrl: String,
    subtitleSize: SubtitleSize = SubtitleSize.SMALL,
    onSelect: (SubtitleTrack) -> Unit,
    onSizeChange: (SubtitleSize) -> Unit = {},
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var pendingUrl by remember(selectedUrl) { mutableStateOf(selectedUrl) }
    var pendingSize by remember(subtitleSize) { mutableStateOf(subtitleSize) }
    val listState = rememberLazyListState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF16121A))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.subtitles),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color(0xFF8F8791),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onDismiss)
                )
            }
            Spacer(Modifier.height(18.dp))

            // Size selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SizeOption(
                    label = stringResource(R.string.subtitle_small),
                    glyphSize = 15.sp,
                    selected = pendingSize == SubtitleSize.SMALL,
                    onClick = { pendingSize = SubtitleSize.SMALL; onSizeChange(SubtitleSize.SMALL) }
                )
                SizeOption(
                    label = stringResource(R.string.subtitle_medium),
                    glyphSize = 19.sp,
                    selected = pendingSize == SubtitleSize.MEDIUM,
                    onClick = { pendingSize = SubtitleSize.MEDIUM; onSizeChange(SubtitleSize.MEDIUM) }
                )
                SizeOption(
                    label = stringResource(R.string.subtitle_large),
                    glyphSize = 24.sp,
                    selected = pendingSize == SubtitleSize.LARGE,
                    onClick = { pendingSize = SubtitleSize.LARGE; onSizeChange(SubtitleSize.LARGE) }
                )
            }

            Spacer(Modifier.height(20.dp))

            // Language list with a thin scroll indicator on the right, like the mock
            Row(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tracks, key = { it.url }) { track ->
                        val selected = track.url == pendingUrl
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Color(0xFF2A2530) else Color.Transparent)
                                .clickable {
                                    pendingUrl = track.url
                                    onSelect(track)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                track.label,
                                color = if (selected) Gold else Color.White,
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            RadioDot(selected = selected)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(300.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFFFFF))
                ) {
                    val scrollProgress by remember {
                        derivedStateOf {
                            val info = listState.layoutInfo
                            val visibleItems = info.visibleItemsInfo
                            val totalCount = info.totalItemsCount
                            if (visibleItems.isEmpty() || totalCount == 0) {
                                0f
                            } else {
                                val firstItem = visibleItems.first()
                                val itemHeight = firstItem.size.takeIf { it > 0 }?.toFloat() ?: 1f
                                val scrolledItemUnits = firstItem.index + (-firstItem.offset / itemHeight)
                                val maxScrollableUnits = (totalCount - visibleItems.size).coerceAtLeast(1)
                                (scrolledItemUnits / maxScrollableUnits).coerceIn(0f, 1f)
                            }
                        }
                    }
                    val visibleFraction by remember {
                        derivedStateOf {
                            val info = listState.layoutInfo
                            val totalCount = info.totalItemsCount.coerceAtLeast(1)
                            (info.visibleItemsInfo.size.toFloat() / totalCount).coerceIn(0.15f, 1f)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((300.dp.value * visibleFraction).dp)
                            .padding(top = (300.dp.value * (1f - visibleFraction) * scrollProgress).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Gold)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Save button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Pink)
                    .clickable(onClick = onSave),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.save),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SizeOption(
    label: String,
    glyphSize: androidx.compose.ui.unit.TextUnit,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (selected)
                        Modifier.border(1.5.dp, Gold, RoundedCornerShape(14.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // Overlapping "Aa" glyphs to mimic the mock's stacked icon
            Box {
                Text(
                    "A",
                    color = Gold,
                    fontSize = glyphSize,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(end = 10.dp, bottom = 6.dp)
                )
                Text(
                    "A",
                    color = Gold,
                    fontSize = (glyphSize.value * 1.35f).sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) Gold else Color(0xFF3A3540)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16121A))
            )
        }
    }
}



@Composable
private fun FeedbackFormSheet(
    filmTitle: String,
    episodeNumber: Int,
    thumbnailUrl: String,
    selectedReason: String,
    onReasonSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {

    val reportReasons = listOf(
        stringResource(R.string.report_reason_episode_error),
        stringResource(R.string.report_reason_paid_film_too_long),
        stringResource(R.string.report_reason_low_quality),
        stringResource(R.string.report_reason_subtitle_missing),
        stringResource(R.string.report_reason_inaccurate_subtitle),
        stringResource(R.string.report_reason_other)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF16121A))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Report an Issue",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Help us improve your viewing experience",
                        color = Color(0xFF8F8791),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color(0xFF8F8791),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onDismiss)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Film thumbnail + title/episode
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShortsThumbnail(
                    imageUrl = thumbnailUrl,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        filmTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.episode_title, episodeNumber),
                        color = Color(0xFFB7ABB2),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "Please select a reason",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))

            Column {
                reportReasons.forEach { reason ->
                    val selected = reason == selectedReason

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReasonSelected(reason) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = reason,
                            color = if (selected) Color(0xFFFF5168) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        ReasonRadio(selected = selected)
                    }

                    if (reason != reportReasons.last()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0x14FFFFFF))
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selectedReason.isNotBlank()) Color(0xFFFF5168) else Color(0x553A3035))
                    .clickable(enabled = selectedReason.isNotBlank(), onClick = onSubmit),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.submit),
                    color = if (selectedReason.isNotBlank()) Color.White else Color(0xFF8F7D84),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ReasonRadio(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFFFF5168) else Color.Transparent)
            .border(
                width = if (selected) 0.dp else 1.5.dp,
                color = if (selected) Color.Transparent else Color(0xFF4A4550),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun FeedbackOptionsSheet(
    autoNext: Boolean,
    autoUnlock: Boolean,
    onAutoNextChange: (Boolean) -> Unit,
    onAutoUnlockChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF16121A))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Text(
                stringResource(R.string.playback_options),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(20.dp))

            PlaybackToggleRow(
                title = stringResource(R.string.auto_next_episode),
                description = stringResource(R.string.auto_next_episode_desc),
                checked = autoNext,
                onCheckedChange = onAutoNextChange
            )

            Spacer(Modifier.height(20.dp))

            PlaybackToggleRow(
                title = stringResource(R.string.auto_unlock_episodes),
                description = stringResource(R.string.auto_unlock_episodes_desc),
                checked = autoUnlock,
                onCheckedChange = onAutoUnlockChange
            )
        }
    }
}

@Composable
private fun PlaybackToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                color = Color(0xFF8F8791),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal
            )
        }
        Spacer(Modifier.width(12.dp))
        PillSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PillSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumbOffset by animateDpAsState(targetValue = if (checked) 18.dp else 2.dp, label = "switchThumb")
    Box(
        modifier = Modifier
            .width(42.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(50))
            .background(if (checked) Color(0xFF4A4550) else Color(0xFF2A2530))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFFB7ABB2))
        )
    }
}


private data class InstalledShareApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: android.graphics.Bitmap?
)


@Composable
private fun ShareOptionsSheet(
    shareText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copiedText = "copied to clipboard"

    val shareApps by produceState<List<InstalledShareApp>>(initialValue = emptyList(), shareText) {
        value = withContext(Dispatchers.IO) { context.resolveShareApps(shareText) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF16121A))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Text(
                stringResource(R.string.share),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(16.dp))

            // Share text preview card with copy icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF211D25))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    shareText,
                    color = Color(0xFFE5D2D7),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = null,
                    tint = Color(0xFFCDB8BF),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            scope.launch {
                                clipboard.setClipEntry(
                                    androidx.compose.ui.platform.ClipEntry(
                                        android.content.ClipData.newPlainText("share_text", shareText)
                                    )
                                )
                                android.widget.Toast
                                    .makeText(context, copiedText, android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                )
            }

            Spacer(Modifier.height(20.dp))

            if (shareApps.isEmpty()) {
                Text(
                    stringResource(R.string.loading_share_apps),
                    color = Color(0xFF8F8791),
                    fontSize = 13.sp
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(shareApps, key = { it.packageName + it.activityName }) { app ->
                        InstalledShareAppIcon(
                            app = app,
                            onClick = {
                                context.shareViaComponent(shareText, app)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledShareAppIcon(
    app: InstalledShareApp,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF2E2A31))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = app.icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            app.label,
            color = Color(0xFFE5D2D7),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Mirrors what the system share sheet does internally: query PackageManager
 * for all activities that can handle ACTION_SEND / text/plain, in the same
 * order the OS would rank them (queryIntentActivities already returns a
 * priority-sorted list based on usage/relevance on most OEM skins).
 */
private fun Context.resolveShareApps(shareText: String): List<InstalledShareApp> {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val resolveInfos: List<ResolveInfo> = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            sendIntent,
            PackageManager.ResolveInfoFlags.of(0L)
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(sendIntent, 0)
    }

    return resolveInfos
        .distinctBy { it.activityInfo.packageName + it.activityInfo.name }
        .mapNotNull { resolveInfo ->
            runCatching {
                val label = resolveInfo.loadLabel(packageManager).toString()
                val drawable: Drawable = resolveInfo.loadIcon(packageManager)
                InstalledShareApp(
                    label = label,
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name,
                    icon = drawable.toBitmapSafely()
                )
            }.getOrNull()
        }
}

private fun Drawable.toBitmapSafely(): android.graphics.Bitmap? = runCatching {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    bitmap
}.getOrNull()

private fun Context.shareViaComponent(shareText: String, app: InstalledShareApp) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        setClassName(app.packageName, app.activityName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(fallback, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun UnlockEpisodeDialog(
    posterUrl: String,
    episodeNumber: Int,
    dailyUnlocksUsed: Int,
    dailyUnlockLimit: Int,
    isLoading: Boolean,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color(0xFF8F8791),
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                        .clickable(enabled = !isLoading, onClick = onDismiss)
                        .padding(3.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(150.dp)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                ShortsThumbnail(posterUrl, Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE12E3E))
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "EP $episodeNumber",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                stringResource(R.string.unlock_episode_title, episodeNumber),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.unlock_episode_desc),
                color = Color(0xFF9D8A91),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            Text(
                stringResource(R.string.daily_progress, dailyUnlocksUsed, dailyUnlockLimit),
                color = Color(0xFF7A7178),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isLoading) Color(0x553A3035) else Pink)
                    .clickable(enabled = !isLoading, onClick = onWatchAd),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.watch_ad),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                stringResource(R.string.unlock_permanent_note),
                color = Color(0xFF7A7178),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DailyLimitReachedDialog(
    dailyUnlockLimit: Int,
    onBrowseFreeEpisodes: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF3A1216), Color(0xFF16121A)),
                        radius = 340f
                    )
                )
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(26.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color(0xFF8F8791),
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                        .clickable(onClick = onDismiss)
                        .padding(3.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            Icon(
                Icons.Filled.HourglassEmpty,
                contentDescription = null,
                tint = Color(0xFFF08A3C),
                modifier = Modifier.size(56.dp)
            )

            Spacer(Modifier.height(18.dp))

            Text(
                stringResource(R.string.daily_limit_title),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.daily_limit_desc, dailyUnlockLimit),
                color = Color(0xFFCDB8BF),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Pink)
                    .clickable(onClick = onBrowseFreeEpisodes),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.browse_free_episodes),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Explore,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
