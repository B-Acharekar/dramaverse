package app.dramaverse.stream.screen

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Feedback
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.R
import app.dramaverse.stream.data.ShortsItem
import app.dramaverse.stream.data.SubtitleTrack
import app.dramaverse.stream.model.ShortsViewModel
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
import kotlinx.coroutines.withContext
import java.net.URL

private val ShortsBackground = Color(0xFF050507)
private val Gold = Color(0xFFF5C65B)
private val Pink = Color(0xFFFF4D73)

@Composable
fun ShortsScreen(
    backendBaseUrl: String,
    initialFilmId: Int?,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onRewards: () -> Unit,
    onPlanner: () -> Unit,
    viewModel: ShortsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState { uiState.items.size.coerceAtLeast(1) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var showPlaybackOptions by remember { mutableStateOf(false) }
    var showFeedbackForm by remember { mutableStateOf(false) }
    var autoNext by remember { mutableStateOf(false) }
    var autoUnlock by remember { mutableStateOf(false) }
    var ccEnabled by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }

    LaunchedEffect(backendBaseUrl, initialFilmId) {
        viewModel.loadInitial(backendBaseUrl, initialFilmId)
    }

    LaunchedEffect(initialFilmId, uiState.items.size) {
        val isGenericFeed = initialFilmId == null || initialFilmId == 0
        if (!isGenericFeed || uiState.items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(10_000)
            val nextPage = (pagerState.currentPage + 1).coerceAtMost(uiState.items.lastIndex)
            if (nextPage != pagerState.currentPage) {
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage, uiState.items.size) {
        controlsVisible = true
        isPlaying = true
        showPlaybackOptions = false
        showFeedbackForm = false
        viewModel.ensurePlayback(pagerState.currentPage, backendBaseUrl)
        viewModel.loadMoreIfNeeded(pagerState.currentPage, backendBaseUrl)
    }

    LaunchedEffect(controlsVisible, isPlaying, pagerState.currentPage) {
        if (controlsVisible && isPlaying) {
            delay(8000)
            controlsVisible = false
            showPlaybackOptions = false
            showFeedbackForm = false
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
                ShortsPage(
                    item = uiState.items[page],
                    itemIndex = page,
                    isActive = page == pagerState.currentPage,
                    backendBaseUrl = backendBaseUrl,
                    controlsVisible = controlsVisible,
                    isPlaying = isPlaying,
                    showPlaybackOptions = showPlaybackOptions,
                    showFeedbackForm = showFeedbackForm,
                    autoNext = autoNext,
                    autoUnlock = autoUnlock,
                    ccEnabled = ccEnabled,
                    playbackSpeed = playbackSpeed,
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
                        showPlaybackOptions = false
                    },
                    onAutoNextChange = { enabled ->
                        autoNext = enabled
                        showPlaybackOptions = false
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
                            filmId = item.film.id,
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
                    onToggleCc = { ccEnabled = !ccEnabled },
                    onCycleSpeed = {
                        playbackSpeed = when (playbackSpeed) {
                            1f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2f
                            else -> 1f
                        }
                    }
                )
            }
        }
        if (controlsVisible) {
            BottomNavigationBar(
                selected = "Shorts",
                onHome = onHome,
                onShorts = {},
                onLibrary = onLibrary,
                onRewards = onRewards,
                onPlanner = onPlanner,
                modifier = Modifier.align(Alignment.BottomCenter)
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
    backendBaseUrl: String,
    controlsVisible: Boolean,
    isPlaying: Boolean,
    showPlaybackOptions: Boolean,
    showFeedbackForm: Boolean,
    autoNext: Boolean,
    autoUnlock: Boolean,
    ccEnabled: Boolean,
    playbackSpeed: Float,
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
    onToggleCc: () -> Unit,
    onCycleSpeed: () -> Unit
) {
    var videoReady by remember(item.playUrl) { mutableStateOf(false) }
    var reminderOn by remember(item.film.id) { mutableStateOf(false) }
    var liked by remember(item.film.id, item.episodeNumber) { mutableStateOf(false) }
    var positionMs by remember(item.playUrl) { mutableStateOf(0L) }
    var durationMs by remember(item.playUrl) { mutableStateOf(0L) }
    var finishHandled by remember(item.playUrl) { mutableStateOf(false) }
    var pendingSeekMs by remember(item.playUrl) { mutableStateOf<Long?>(null) }
    var subtitleText by remember(item.playUrl) { mutableStateOf("") }
    var feedbackText by remember(item.playUrl) { mutableStateOf("") }
    var selectedSubtitleUrl by remember(item.playUrl) { mutableStateOf("") }
    var showSubtitleOptions by remember(item.playUrl) { mutableStateOf(false) }
    var showEpisodeOptions by remember(item.film.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val savedToListText = stringResource(R.string.saved_to_list)
    val removedFromListText = stringResource(R.string.removed_from_list)
    val feedbackSentText = stringResource(R.string.feedback_sent)
    val hasPopup = showPlaybackOptions || showFeedbackForm || showSubtitleOptions || showEpisodeOptions

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isActive || item.playUrl.isBlank()) {
            LoadingBackdrop(
                item = item,
                showLoader = isActive,
                modifier = Modifier
                    .fillMaxSize()
            )
        } else {
            HlsVideoPlayer(
                playUrl = item.playUrl,
                subtitleTracks = item.subtitleTracks,
                selectedSubtitleUrl = selectedSubtitleUrl,
                isPlaying = isPlaying,
                ccEnabled = ccEnabled,
                controlsVisible = controlsVisible,
                playbackSpeed = playbackSpeed,
                repeatCurrent = !autoNext,
                onReady = { videoReady = true },
                onProgress = { position, duration ->
                    positionMs = position
                    durationMs = duration
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onTogglePlay)
        )

        if (controlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xD6000000),
                                Color(0x24000000),
                                Color.Transparent,
                                Color(0x33000000),
                                Color(0xE8050507)
                            ),
                            startY = 0f
                        )
                    )
            )
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
            ShortsTopBar(
                item = item,
                onBack = onBack,
                onFeedbackClick = onFeedbackClick,
                onOptionsClick = onOptionsClick
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 88.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SideAction(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    formatCount(displayLikeCount(item) + if (liked) 1 else 0),
                    if (liked) Pink else Color(0xFFFFAAB6),
                    onClick = {
                        liked = !liked
                        onLikeClick(item, liked)
                    }
                )
                SideAction(
                    icon = if (reminderOn) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    label = formatCount(displaySaveCount(item) + if (reminderOn) 1 else 0),
                    tint = if (reminderOn) Gold else Color.White,
                    onClick = {
                        reminderOn = !reminderOn
                        onReminderClick(item, reminderOn)
                        Toast
                            .makeText(context, if (reminderOn) savedToListText else removedFromListText, Toast.LENGTH_SHORT)
                            .show()
                    }
                )
                SideAction(Icons.Filled.Share, R.string.share, Color.White)
                SideAction(
                    Icons.Filled.VideoLibrary,
                    R.string.episodes,
                    Color.White,
                    onClick = { showEpisodeOptions = true }
                )
                SideAction(
                    Icons.Filled.ClosedCaption,
                    R.string.cc,
                    if (ccEnabled && item.subtitleUrl.isNotBlank()) Gold else Color.White,
                    onClick = {
                        if (item.subtitleTracks.size > 1) {
                            if (!ccEnabled && selectedSubtitleUrl.isBlank()) {
                                selectedSubtitleUrl = item.subtitleTracks.firstOrNull()?.url.orEmpty()
                            }
                            if (!ccEnabled) onToggleCc()
                            showSubtitleOptions = !showSubtitleOptions
                        } else {
                            if (!ccEnabled && selectedSubtitleUrl.isBlank()) {
                                selectedSubtitleUrl = item.subtitleTracks.firstOrNull()?.url.orEmpty()
                            }
                            onToggleCc()
                            showSubtitleOptions = false
                        }
                    }
                )
                SideTextAction(speedLabel(playbackSpeed), R.string.speed, onCycleSpeed)
            }

            Box(modifier = Modifier.align(Alignment.BottomStart)) {
                ShortsCaption(
                    item = item,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeekTo = { targetMs ->
                        positionMs = targetMs
                        pendingSeekMs = targetMs
                    }
                )
            }
        }

        if (ccEnabled && subtitleText.isNotBlank()) {
            ComposeSubtitleOverlay(
                text = subtitleText,
                controlsVisible = controlsVisible,
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }

        if (showFeedbackForm && controlsVisible) {
            FeedbackFormSheet(
                value = feedbackText,
                onValueChange = { feedbackText = it },
                onSubmit = {
                    if (feedbackText.isNotBlank()) {
                        onSubmitFeedback(item, feedbackText)
                        Toast.makeText(context, feedbackSentText, Toast.LENGTH_SHORT).show()
                        feedbackText = ""
                        onClosePopups()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (showSubtitleOptions && controlsVisible) {
            SubtitleOptionsSheet(
                tracks = item.subtitleTracks,
                selectedUrl = selectedSubtitleUrl,
                onSelect = { track ->
                    selectedSubtitleUrl = track.url
                    showSubtitleOptions = false
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (showEpisodeOptions && controlsVisible) {
            EpisodeOptionsSheet(
                currentEpisode = item.episodeNumber,
                totalEpisodes = item.film.episodeTotal,
                modifier = Modifier.align(Alignment.BottomCenter),
                onDismiss = { showEpisodeOptions = false }
            )
        }
    }
}

@Composable
private fun ShortsTopBar(
    item: ShortsItem,
    onBack: () -> Unit,
    onFeedbackClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x66111114))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable(onClick = onBack)
                .padding(9.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(
                R.string.episode_progress,
                item.episodeNumber,
                item.film.episodeTotal
            ),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            letterSpacing = 0.sp
        )
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x66111114))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable(onClick = onFeedbackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Feedback, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x66111114))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable(onClick = onOptionsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ShortsCaption(
    item: ShortsItem,
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit
) {
    val safeDuration = durationMs.takeIf { it > 0L && it < Long.MAX_VALUE / 2 } ?: 0L
    var descriptionExpanded by remember(item.film.id, item.episodeNumber) { mutableStateOf(false) }
    val description = item.film.description.ifBlank { stringResource(R.string.default_short_description) }
    val showDescriptionToggle = description.length > 42
    var sliderPosition by remember(positionMs, safeDuration) {
        mutableStateOf(positionMs.coerceIn(0L, safeDuration).toFloat())
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 86.dp, bottom = 104.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.episode_progress,
                    item.episodeNumber,
                    item.film.episodeTotal
                ),
                color = Gold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x5533230A))
                    .border(1.dp, Color(0x88F5C65B), RoundedCornerShape(6.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.trending_number), color = Color(0xFFFFC3CA), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            item.film.title,
            color = Color.White,
            fontSize = 20.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            description,
            color = Color(0xFFE5D2D7),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (descriptionExpanded) 8 else 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        if (showDescriptionToggle) {
            Spacer(Modifier.height(4.dp))
            Text(
                if (descriptionExpanded) stringResource(R.string.view_less) else stringResource(R.string.view_more),
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
                modifier = Modifier.clickable { descriptionExpanded = !descriptionExpanded }
            )
        }
        Spacer(Modifier.height(12.dp))
        ThinSeekBar(
            progress = if (safeDuration > 0L) sliderPosition / safeDuration.toFloat() else 0f,
            onSeekFraction = { fraction ->
                val targetMs = (safeDuration * fraction).toLong().coerceIn(0L, safeDuration)
                sliderPosition = targetMs.toFloat()
                onSeekTo(targetMs)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatPlaybackTime(sliderPosition.toLong()), color = Color(0xFFE8D7DC), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
            Spacer(Modifier.weight(1f))
            Text(formatPlaybackTime(safeDuration), color = Color(0xFFE8D7DC), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun ComposeSubtitleOverlay(
    text: String,
    controlsVisible: Boolean,
    modifier: Modifier
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 16.sp,
        lineHeight = 20.sp,
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

@Composable
private fun FeedbackOptionsSheet(
    autoNext: Boolean,
    autoUnlock: Boolean,
    onAutoNextChange: (Boolean) -> Unit,
    onAutoUnlockChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xF2141217))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 98.dp)
    ) {
        Text(stringResource(R.string.playback_options), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.auto_next_episode), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Text(stringResource(R.string.auto_next_episode_desc), color = Color(0xFFC8B6BC), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            }
            Switch(checked = autoNext, onCheckedChange = onAutoNextChange)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.auto_unlock_episodes), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Text(stringResource(R.string.auto_unlock_episodes_desc), color = Color(0xFFC8B6BC), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            }
            Switch(checked = autoUnlock, onCheckedChange = onAutoUnlockChange)
        }
    }
}

@Composable
private fun FeedbackFormSheet(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .imePadding()
            .padding(top = 18.dp, bottom = 98.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xF2141217))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(stringResource(R.string.send_feedback), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            minLines = 3,
            maxLines = 5,
            textStyle = TextStyle(color = Color.White, letterSpacing = 0.sp),
            placeholder = {
                Text(stringResource(R.string.feedback_hint), color = Color(0xFF9D8A91), letterSpacing = 0.sp)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSubmit,
            enabled = value.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Pink,
                contentColor = Color.White,
                disabledContainerColor = Color(0x553A3035),
                disabledContentColor = Color(0xFF8F7D84)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.submit), fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun SubtitleOptionsSheet(
    tracks: List<SubtitleTrack>,
    selectedUrl: String,
    onSelect: (SubtitleTrack) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 98.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xF2141217))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            stringResource(R.string.subtitles),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )
        tracks.forEach { track ->
            val selected = track.url == selectedUrl
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0x33F5C65B) else Color.Transparent)
                    .clickable { onSelect(track) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    track.label,
                    color = if (selected) Gold else Color(0xFFE8D7DC),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Text(stringResource(R.string.subtitle_on), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
                }
            }
        }
    }
}

@Composable
private fun EpisodeOptionsSheet(
    currentEpisode: Int,
    totalEpisodes: Int,
    modifier: Modifier,
    onDismiss: () -> Unit
) {
    val episodes = remember(totalEpisodes) { (1..totalEpisodes.coerceAtLeast(1)).toList() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 98.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xF2141217))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.episodes),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$currentEpisode/$totalEpisodes",
                color = Color(0xFFCDB8BF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.height(260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(episodes) { episode ->
                val selected = episode == currentEpisode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color(0x33F5C65B) else Color(0x14111114))
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.episode_title,episode),
                        color = if (selected) Gold else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (selected) stringResource(R.string.playing) else stringResource(R.string.free),
                        color = if (selected) Gold else Color(0xFFCDB8BF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.sp
                    )
                }
            }
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
private fun SideAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: Int,
    tint: Color,
    onClick: () -> Unit = {}
) {
    SideAction(icon = icon, label = stringResource(label), tint = tint, onClick = onClick)
}

@Composable
private fun SideAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
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
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color(0xFFF2D7DD), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
    }
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

@Composable
private fun SideTextAction(
    value: String,
    label: Int,
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
        Text(stringResource(label), color = Color(0xFFF2D7DD), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
    }
}

private fun speedLabel(speed: Float): String {
    return when (speed) {
        1f -> "1x"
        1.25f -> "1.25x"
        1.5f -> "1.5x"
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
