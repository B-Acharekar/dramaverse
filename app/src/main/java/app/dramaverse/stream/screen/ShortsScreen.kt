package app.dramaverse.stream.screen

import android.net.Uri
import android.view.View
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.data.ShortsItem
import app.dramaverse.stream.model.ShortsViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
    viewModel: ShortsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState { uiState.items.size.coerceAtLeast(1) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var showFeedbackOptions by remember { mutableStateOf(false) }
    var autoUnlock by remember { mutableStateOf(false) }

    LaunchedEffect(backendBaseUrl, initialFilmId) {
        viewModel.loadInitial(backendBaseUrl, initialFilmId)
    }

    LaunchedEffect(pagerState.currentPage, uiState.items.size) {
        controlsVisible = true
        isPlaying = true
        showFeedbackOptions = false
        viewModel.ensurePlayback(pagerState.currentPage, backendBaseUrl)
        viewModel.loadMoreIfNeeded(pagerState.currentPage, backendBaseUrl)
    }

    LaunchedEffect(controlsVisible, isPlaying, pagerState.currentPage) {
        if (controlsVisible && isPlaying) {
            delay(4500)
            controlsVisible = false
            showFeedbackOptions = false
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
                    isActive = page == pagerState.currentPage,
                    controlsVisible = controlsVisible,
                    isPlaying = isPlaying,
                    showFeedbackOptions = showFeedbackOptions,
                    autoUnlock = autoUnlock,
                    onBack = onBack,
                    onTogglePlay = {
                        if (controlsVisible) {
                            isPlaying = !isPlaying
                        } else {
                            controlsVisible = true
                        }
                    },
                    onToggleControls = { controlsVisible = !controlsVisible },
                    onFeedbackClick = {
                        controlsVisible = true
                        showFeedbackOptions = !showFeedbackOptions
                    },
                    onAutoUnlockChange = { autoUnlock = it }
                )
            }
        }
        if (controlsVisible) {
            BottomNavigationBar(
                selected = "Shorts",
                onHome = onHome,
                onShorts = {},
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ShortsPage(
    item: ShortsItem,
    isActive: Boolean,
    controlsVisible: Boolean,
    isPlaying: Boolean,
    showFeedbackOptions: Boolean,
    autoUnlock: Boolean,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleControls: () -> Unit,
    onFeedbackClick: () -> Unit,
    onAutoUnlockChange: (Boolean) -> Unit
) {
    var videoReady by remember(item.playUrl) { mutableStateOf(false) }
    var reminderOn by remember(item.film.id) { mutableStateOf(false) }

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
                subtitleUrl = item.subtitleUrl,
                isPlaying = isPlaying,
                onReady = { videoReady = true },
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
                onFeedbackClick = onFeedbackClick
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp, bottom = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                SideAction(Icons.Filled.Favorite, "Like", Color(0xFFFFAAB6))
                SideAction(
                    icon = if (reminderOn) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    label = "Save",
                    tint = if (reminderOn) Gold else Color.White,
                    onClick = { reminderOn = !reminderOn }
                )
                SideAction(Icons.Filled.Share, "Share", Color.White)
                SideAction(Icons.Filled.VideoLibrary, "Episodes", Color.White)
                SideAction(Icons.Filled.ClosedCaption, "CC", Color.White)
                SideTextAction("1x", "Speed")
            }

            Box(modifier = Modifier.align(Alignment.BottomStart)) {
                ShortsCaption(item)
            }
        }

        if (showFeedbackOptions && controlsVisible) {
            FeedbackOptionsSheet(
                autoUnlock = autoUnlock,
                onAutoUnlockChange = onAutoUnlockChange,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ShortsTopBar(
    item: ShortsItem,
    onBack: () -> Unit,
    onFeedbackClick: () -> Unit
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
            "Episode ${item.episodeNumber}/${item.film.episodeTotal}",
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
                .clickable(onClick = onFeedbackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ShortsCaption(item: ShortsItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 86.dp, bottom = 104.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Episode ${item.episodeNumber}/${item.film.episodeTotal}",
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
            Text("Trending #1", color = Color(0xFFFFC3CA), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            item.film.title,
            color = Color.White,
            fontSize = 25.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            item.film.description,
            color = Color(0xFFE5D2D7),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp
        )
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
    autoUnlock: Boolean,
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
        Text("Playback options", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto unlock episodes", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Text("Unlock the next paid episode automatically when available.", color = Color(0xFFC8B6BC), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            }
            Switch(checked = autoUnlock, onCheckedChange = onAutoUnlockChange)
        }
    }
}

@Composable
private fun HlsVideoPlayer(playUrl: String, modifier: Modifier) {
    HlsVideoPlayer(
        playUrl = playUrl,
        subtitleUrl = "",
        isPlaying = true,
        onReady = {},
        modifier = modifier
    )
}

@Composable
private fun HlsVideoPlayer(
    playUrl: String,
    subtitleUrl: String,
    isPlaying: Boolean,
    onReady: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val player = remember(playUrl, subtitleUrl) { ExoPlayer.Builder(context).build() }

    DisposableEffect(playUrl, subtitleUrl) {
        val subtitleConfigurations = if (subtitleUrl.isNotBlank()) {
            listOf(
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("en")
                    .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                    .build()
            )
        } else {
            emptyList()
        }
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(playUrl))
            .setSubtitleConfigurations(subtitleConfigurations)
            .build()
        player.setMediaItem(mediaItem)
        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        player.playWhenReady = true
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) onReady()
            }
        }
        player.addListener(listener)
        player.prepare()
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(isPlaying) {
        player.playWhenReady = isPlaying
        if (isPlaying) player.play() else player.pause()
    }

    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = {
            PlayerView(context).apply {
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                subtitleView?.visibility = View.GONE
                this.player = player
            }
        },
        update = { playerView ->
            if (playerView.player !== player) {
                playerView.player = player
            }
        }
    )
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
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0x8A111114))
                .border(1.dp, Color(0x26FFFFFF), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = Color(0xFFF2D7DD), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
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
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0x8A111114))
                .border(1.dp, Color(0x26FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = Color(0xFFF2D7DD), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
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
