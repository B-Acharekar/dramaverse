package app.dramaverse.stream.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.model.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.dramaverse.stream.R
import app.dramaverse.stream.data.LocaleHelper

private val BgColor = Color(0xFF0E0B10)
private val CardColor = Color(0xFF17141B)
private val DividerColor = Color(0xFF2A2530)
private val GoldColor = Color(0xFFF4B93B)
private val TextPrimary = Color(0xFFF5F3F7)
private val TextSecondary = Color(0xFF9C96A5)
private val SectionLabelColor = Color(0xFF6F6878)
private val AvatarRingGradient = Brush.sweepGradient(
    listOf(Color(0xFFF4B93B), Color(0xFFE05C8A), Color(0xFF9B5DE5), Color(0xFFF4B93B))
)

private val RosePrimary = Color(0xFFF2C4CE)      // row titles + icon tint
private val RoseMuted = Color(0xFFC79AA6)        // subtitles, section labels, chevrons
private val RoseIconBg = Color(0x29F2C4CE)       // translucent circular icon backdrop

data class ProfileStats(
    val coins: Int = 1240,
    val hoursWatched: Int = 48,
    val minutesWatched: Int = 0,
    val episodesWatched: Int = 156
)


private enum class ProfileImageTarget { BANNER, AVATAR }


private data class PresetAvatar(
    @DrawableRes val imageRes: Int,
    val backgroundColor: Color
)

private val presetAvatars = listOf(
    PresetAvatar(R.drawable.avatar_1, Color(0xFFE05C8A)),
    PresetAvatar(R.drawable.avatar_2, Color(0xFFF4B93B)),
    PresetAvatar(R.drawable.avatar_3, Color(0xFF5DBB63)),
    PresetAvatar(R.drawable.avatar_4, Color(0xFF5C8AE0)),
    PresetAvatar(R.drawable.avatar_5, Color(0xFF9B5DE5)),
    PresetAvatar(R.drawable.avatar_6, Color(0xFFE0745C))
)

@Composable
fun ProfileScreen(
    backendBaseUrl: String,
    userName: String = "Drama Enthusiast",
//    isVipGold: Boolean = true,
    stats: ProfileStats = ProfileStats(),
    currentLanguage: String = "English (US)",
    bannerImageUrl: String? = null,
    avatarImageUrl: String? = null,
    onHome: () -> Unit = {},
    onShorts: () -> Unit = {},
    onLibrary: () -> Unit = {},
    onEditAvatar: () -> Unit = {},
    onRewards:() -> Unit = {},
//    onSubscription: () -> Unit = {},
//    onWallet: () -> Unit = {},
//    onDownloads: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onWatchHistory: () -> Unit = {},
    onMyWatchlist: () -> Unit = {},
    onLanguage: () -> Unit = {},
    onSettings: () -> Unit = {},
    onHelpCenter: () -> Unit = {},
    onRateUs: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val mediaState by viewModel.mediaState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val profile = uiState.summary
    val dynamicUserName = profile?.userName ?: userName
    val dynamicStats = ProfileStats(
        coins = profile?.coins ?: stats.coins,
        hoursWatched = profile?.hoursWatched ?: stats.hoursWatched,
        minutesWatched = profile?.minutesWatched ?: stats.minutesWatched,
        episodesWatched = profile?.episodesWatched ?: stats.episodesWatched
    )
    val dynamicAvatarUrl = avatarImageUrl ?: profile?.avatarUrl

    LaunchedEffect(backendBaseUrl) {
        viewModel.loadProfile(backendBaseUrl)
    }
    // Picker flow state (purely transient UI state — fine to keep local).
    var showTargetDialog by remember { mutableStateOf(false) }
    var sourceDialogTarget by remember { mutableStateOf<ProfileImageTarget?>(null) }
    var avatarGridTarget by remember { mutableStateOf<ProfileImageTarget?>(null) }
    var galleryPickTarget by remember { mutableStateOf<ProfileImageTarget?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    val displayedLanguage = LocaleHelper.persistedLanguageName(context) ?: currentLanguage


    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            when (galleryPickTarget) {
                ProfileImageTarget.BANNER -> viewModel.setBannerFromUri(uri)
                ProfileImageTarget.AVATAR -> viewModel.setAvatarFromUri(uri)
                null -> Unit
            }
        }
        galleryPickTarget = null
    }

    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            BottomNavigationBar(
                selected = "Profile",
                onHome = onHome,
                onShorts = onShorts,
                onLibrary = onLibrary,
                onRewards = onRewards,
                onProfile = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHeader(
                userName = dynamicUserName,
//                isVipGold = isVipGold,
                bannerImageUrl = bannerImageUrl,
                bannerFilePath = mediaState.bannerFilePath,
                bannerPreset = mediaState.bannerPresetIndex?.let { presetAvatars.getOrNull(it) },
                isSavingBanner = mediaState.isSavingBanner,
                avatarImageUrl = dynamicAvatarUrl,
                avatarFilePath = mediaState.avatarFilePath,
                avatarPreset = mediaState.avatarPresetIndex?.let { presetAvatars.getOrNull(it) },
                isSavingAvatar = mediaState.isSavingAvatar,
                onEditAvatar = {
                    showTargetDialog = true
                    onEditAvatar()
                }
            )

            StatsRow(stats = dynamicStats)

//            QuickActionsRow(
//                onSubscription = onSubscription,
//                onWallet = onWallet,
//                onDownloads = onDownloads
//            )

            SectionLabel(stringResource(R.string.account_settings))
            SettingsCard {
//                SettingsRow(Icons.Outlined.Edit, R.string.edit_profile, onClick = onEditProfile)
                SettingsRow(Icons.Outlined.History, R.string.watch_history, onClick = onWatchHistory)
                RowDivider()
                SettingsRow(Icons.Outlined.BookmarkBorder, R.string.my_watchlist, onClick = onMyWatchlist)
            }

            SectionLabel(stringResource(R.string.preferences_support))
            SettingsCard {
                SettingsRow(
                    Icons.Outlined.Language,
                    R.string.language_title,
                    subtitle = displayedLanguage,
                    onClick = { showLanguageDialog = true }
                )
//                SettingsRow(Icons.Outlined.Settings, R.string.settings, onClick = onSettings)
            }

            SectionLabel(stringResource(R.string.privacy))
            SettingsCard {
                SettingsRow(Icons.Outlined.StarBorder, R.string.rate_us, onClick = {
                    showRateDialog = true
                    onRateUs()
                })
                RowDivider()
                SettingsRow(Icons.Outlined.Shield, R.string.privacy_policy, onClick = {
                    showPrivacyDialog = true
                    onPrivacyPolicy()
                })
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguage = displayedLanguage,
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            containerColor = CardColor,
            title = { Text(stringResource(R.string.rate_us), color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Enjoying DramaVerse? Rate the app on the Play Store.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRateDialog = false
                        context.openPlayStoreRating()
                    }
                ) {
                    Text("Rate Now", color = GoldColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = CardColor,
            title = { Text(stringResource(R.string.privacy_policy), color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Privacy Policy placeholder\n\nDramaVerse stores your device guest ID, app preferences, rewards, watch progress, watchlist, planner items, and feedback only to power the app experience. A full policy URL can be added here before release.",
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK", color = GoldColor)
                }
            }
        )
    }

    // --- Step 1: which photo to update -------------------------------------------------
    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            containerColor = CardColor,
            title = { Text(stringResource(R.string.update_profile_photo), color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    PickerOptionRow(Icons.Outlined.Image, stringResource(R.string.banner_image)) {
                        showTargetDialog = false
                        sourceDialogTarget = ProfileImageTarget.BANNER
                    }
                    RowDivider()
                    PickerOptionRow(Icons.Outlined.AccountCircle, stringResource(R.string.avatar_image)) {
                        showTargetDialog = false
                        sourceDialogTarget = ProfileImageTarget.AVATAR
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }

    // --- Step 2: pick a source for the chosen photo -------------------------------------
    sourceDialogTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { sourceDialogTarget = null },
            containerColor = CardColor,
            title = {
                Text(
                    text = if (target == ProfileImageTarget.BANNER) stringResource(R.string.update_banner_image) else stringResource(R.string.update_avatar_image),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    PickerOptionRow(Icons.Outlined.PhotoLibrary, stringResource(R.string.choose_from_gallery)) {
                        galleryPickTarget = target
                        sourceDialogTarget = null
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    RowDivider()
                    PickerOptionRow(Icons.Outlined.Face, stringResource(R.string.choose_an_avatar)) {
                        avatarGridTarget = target
                        sourceDialogTarget = null
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { sourceDialogTarget = null }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }

    // --- Step 3: pick one of the available preset avatars -------------------------------
    avatarGridTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { avatarGridTarget = null },
            containerColor = CardColor,
            title = { Text(stringResource(R.string.choose_an_avatar), color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    presetAvatars.withIndex().toList().chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { (index, preset) ->
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(preset.backgroundColor)
                                        .clickable {
                                            when (target) {
                                                ProfileImageTarget.BANNER -> viewModel.setBannerPreset(index)
                                                ProfileImageTarget.AVATAR -> viewModel.setAvatarPreset(index)
                                            }
                                            avatarGridTarget = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = preset.imageRes),
                                        contentDescription = "Preset avatar",
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { avatarGridTarget = null }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
//    isVipGold: Boolean,
    bannerImageUrl: String?,
    bannerFilePath: String?,
    bannerPreset: PresetAvatar?,
    isSavingBanner: Boolean,
    avatarImageUrl: String?,
    avatarFilePath: String?,
    avatarPreset: PresetAvatar?,
    isSavingAvatar: Boolean,
    onEditAvatar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            when {
                bannerFilePath != null -> LocalFileImage(
                    path = bannerFilePath,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                bannerPreset != null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(bannerPreset.backgroundColor, Color(0xFF1C1420), BgColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = bannerPreset.imageRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                !bannerImageUrl.isNullOrBlank() -> NetworkDramaImage(
                    imageUrl = bannerImageUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    seed = userName
                )
                else -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF3A2036), Color(0xFF1C1420), BgColor)
                            )
                        )
                )
            }
            if (isSavingBanner) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldColor, modifier = Modifier.size(28.dp))
                }
            }
        }
        // Bottom fade so content below reads cleanly against the photo.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, BgColor)))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(AvatarRingGradient)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2530)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        avatarFilePath != null -> LocalFileImage(
                            path = avatarFilePath,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        avatarPreset != null -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(avatarPreset.backgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = avatarPreset.imageRes),
                                contentDescription = "Profile avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        !avatarImageUrl.isNullOrBlank() -> NetworkDramaImage(
                            imageUrl = avatarImageUrl,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            seed = userName
                        )
                        else -> Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile avatar",
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    if (isSavingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0x66000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GoldColor, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE05C8A))
                        .clickable { onEditAvatar() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit profile photo",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = userName,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

//            if (isVipGold) {
//                Spacer(modifier = Modifier.height(4.dp))
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(
//                        imageVector = Icons.Filled.WorkspacePremium,
//                        contentDescription = null,
//                        tint = GoldColor,
//                        modifier = Modifier.size(14.dp)
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text(
//                        text = "VIP GOLD MEMBER",
//                        color = GoldColor,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                }
//            }
        }
    }
}

@Composable
private fun StatsRow(stats: ProfileStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(value = stats.coins.toString(), label = stringResource(R.string.coins), valueColor = GoldColor)
        StatDivider()
        StatItem(value = stats.watchTimeLabel(), label = stringResource(R.string.watched))
        StatDivider()
        StatItem(value = stats.episodesWatched.toString(), label = stringResource(R.string.episodes_cap))
    }
}

@Composable
private fun StatItem(value: String, label: String, valueColor: Color = TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = SectionLabelColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(DividerColor)
    )
}

@Composable
private fun QuickActionsRow(
    onSubscription: () -> Unit,
    onWallet: () -> Unit,
    onDownloads: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickAction(Icons.Outlined.CardMembership, stringResource(R.string.subscription), onSubscription)
        QuickAction(Icons.Outlined.AccountBalanceWallet, stringResource(R.string.wallet), onWallet)
        QuickAction(Icons.Outlined.Download, stringResource(R.string.downloads), onDownloads)
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = GoldColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = RoseMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: Int,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(RoseIconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = RosePrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(title), color = RosePrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, color = RoseMuted, fontSize = 12.sp)
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = RoseMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Simple icon + label row used inside the picker dialogs. */
@Composable
private fun PickerOptionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = GoldColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 50.dp)
            .background(DividerColor)
            .height(1.dp)
    )
}

/** Loads and displays an image from a file on disk (the app's persisted copy of a picked photo). */
@Composable
private fun LocalFileImage(
    path: String,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier.background(CardColor))
    }
}


@Composable
fun LanguagePickerDialog(
    currentLanguage: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val languages = remember {
        listOf(
            "English",
            "Spanish",
            "Deutsch",
            "Portuguese",
            "Turkish",
            "Arabic",
            "Hindi",
            "Japanese",
            "Korean",
            "Chinese"
        )
    }
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    val listState = rememberLazyListState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(Color(0xFF161616))
        ) {
            LanguagePickerHeader(
                onDoneClick = {
                    LocaleHelper.persistLanguage(context, selectedLanguage)
                    LocaleHelper.persistPendingStep(context, "Profile")
                    onDismiss()
                    context.findActivity()?.recreate()
                }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 11.5.dp),
                contentPadding = PaddingValues(top = 7.5.dp, bottom = 7.5.dp),
                verticalArrangement = Arrangement.spacedBy(7.5.dp)
            ) {
                items(languages) { language ->
                    LanguagePickerItem(
                        name = language,
                        selected = selectedLanguage == language,
                        onClick = { selectedLanguage = language }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguagePickerHeader(
    onDoneClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .statusBarsPadding()
            .padding(start = 13.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.language_title),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.clickable(onClick = onDoneClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u2713",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = stringResource(R.string.done),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun LanguagePickerItem(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF3A3A3C), RoundedCornerShape(50))
            .then(
                if (selected) {
                    Modifier.border(0.6.dp, Color.White, RoundedCornerShape(50))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguagePickerSelectionRing(selected = selected)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun LanguagePickerSelectionRing(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .aspectRatio(1f)
            .border(
                width = 1.dp,
                color = if (selected) Color.White else Color(0xFF4D7C3C),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

/** Unwraps a possibly-decorated Context (e.g. from [LocaleHelper.wrap]) down to its hosting Activity. */
private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openPlayStoreRating() {
    val marketIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(marketIntent) }
        .onFailure { runCatching { startActivity(webIntent) } }
}

private fun ProfileStats.watchTimeLabel(): String {
    if (hoursWatched <= 0) return "${minutesWatched.coerceAtLeast(0)}m"
    val remainingMinutes = minutesWatched % 60
    return if (remainingMinutes > 0) "${hoursWatched}h ${remainingMinutes}m" else "${hoursWatched}h"
}
