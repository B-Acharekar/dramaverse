package com.drama.x.drama.series.dramax.dramaseries

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drama.x.drama.series.dramax.dramaseries.model.AppStep
import com.drama.x.drama.series.dramax.dramaseries.model.AppViewModel
import kotlinx.coroutines.delay
import com.drama.x.drama.series.dramax.dramaseries.screen.CustomSplashScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.EpisodeScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.LanguageScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.OnboardingScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.ConfirmUninstallScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.HomeScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.LibraryScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.NotificationScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.PlannerScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.ProfileScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.RewardScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.SearchResultsScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.ShortsScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.SurveyUninstallScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.WelcomeBackScreen
import com.drama.x.drama.series.dramax.dramaseries.ads.AdRemoteConfig
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.data.RatingManager
import com.drama.x.drama.series.dramax.dramaseries.screen.AppRatingDialog

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun DramaXApp(
    initialAction: String? = null,
    viewModel: AppViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var onboardingFinishInProgress by remember { mutableStateOf(false) }

    // Mark as shortcut launch if app is opened via shortcuts (Home, MyList only)
    // Uninstall shortcut should follow normal splash + ads flow
    LaunchedEffect(initialAction) {
        if (initialAction == MainActivity.ACTION_WIDGET_HOME || 
            initialAction == MainActivity.ACTION_WIDGET_MY_LIST) {
            GlobalApp.isLaunchedFromShortcut = true
        }
    }

//    val currentStep = when {
//        initialAction == MainActivity.ACTION_WIDGET_UNINSTALL && uiState.currentStep == AppStep.Splash ->
//            AppStep.SplashUninstall
//        initialAction == MainActivity.ACTION_WIDGET_MY_LIST && uiState.currentStep == AppStep.Splash ->
//            AppStep.Library  // Skip splash and go directly to My List (Library)
//        else -> uiState.currentStep
//    }

    val currentStep = when {
        // Uninstall widget → show splash uninstall with ads
        initialAction == MainActivity.ACTION_WIDGET_UNINSTALL && uiState.currentStep == AppStep.Splash ->
            AppStep.SplashUninstall

        initialAction == MainActivity.ACTION_WIDGET_MY_LIST && uiState.currentStep == AppStep.Splash ->
            AppStep.Library

        initialAction == MainActivity.ACTION_WIDGET_HOME && uiState.currentStep == AppStep.Splash ->
            AppStep.Home

        else -> uiState.currentStep
    }


    var pendingRatingOnHome by remember { mutableStateOf(false) }
    var showHomeRatingDialog by remember { mutableStateOf(false) }
    val ratingManager = remember { RatingManager.getInstance(context) }

    //    var pendingRatingOnHome by remember { mutableStateOf(false) }
    // Handle shortcut navigation after splash finishes (for actions that still need splash)
//    LaunchedEffect(initialAction, uiState.currentStep) {
//        if (initialAction == null) return@LaunchedEffect
//
//        // My List shortcut bypasses splash, so handle it immediately
//        if (initialAction == MainActivity.ACTION_WIDGET_MY_LIST && uiState.currentStep == AppStep.Splash) {
//            viewModel.startWidgetMyList()
//            return@LaunchedEffect
//        }
//
//        // Wait until splash is done before navigating for other actions
//        if (uiState.currentStep == AppStep.Splash ||
//            uiState.currentStep == AppStep.SplashUninstall) {
//            return@LaunchedEffect
//        }
//
//        when (initialAction) {
//            MainActivity.ACTION_WIDGET_HOME -> viewModel.startWidgetHome()
//            // ACTION_WIDGET_MY_LIST is handled above to skip splash
//            // Uninstall flow is handled by the when expression above
//        }
//    }

    LaunchedEffect(initialAction, uiState.currentStep) {
        if (initialAction == null) return@LaunchedEffect

        when {
            initialAction == MainActivity.ACTION_WIDGET_UNINSTALL && uiState.currentStep == AppStep.Splash -> {
                 // Trigger splash uninstall flow - this will be handled by the currentStep logic above
                 return@LaunchedEffect
            }
            initialAction == MainActivity.ACTION_WIDGET_MY_LIST && uiState.currentStep == AppStep.Splash -> {
                viewModel.startWidgetMyList()
                return@LaunchedEffect
            }
            initialAction == MainActivity.ACTION_WIDGET_HOME && uiState.currentStep == AppStep.Splash -> {
                viewModel.startWidgetHome()
                return@LaunchedEffect
            }
        }

        if (uiState.currentStep == AppStep.Splash ||
            uiState.currentStep == AppStep.SplashUninstall
        ) {
            return@LaunchedEffect
        }
    }


    LaunchedEffect(currentStep) {
        if (currentStep == AppStep.Shorts || currentStep == AppStep.Episodes) {
            context.findActivity()?.let { AdsManager.loadInterBack(it) }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (com.drama.x.drama.series.dramax.dramaseries.GlobalApp.shouldShowWelcomeBackOnResume && 
                !com.drama.x.drama.series.dramax.dramaseries.GlobalApp.isLaunchedFromShortcut) {
                val step = uiState.currentStep
                if (step != AppStep.Splash &&
                    step != AppStep.SplashUninstall &&
                    step != AppStep.Language &&
                    step != AppStep.Onboarding &&
                    step != AppStep.WelcomeBack) {
                    com.drama.x.drama.series.dramax.dramaseries.GlobalApp.shouldShowWelcomeBackOnResume = false
                    viewModel.openWelcomeBack()
                }
            }
            kotlinx.coroutines.delay(250)
        }
    }

    LaunchedEffect(uiState.recreateRequested) {
        if (uiState.recreateRequested) {
            viewModel.onRecreateHandled()
            // Recreate only after persisting locale so stringResource() resolves in the selected language.
            context.findActivity()?.let { activity ->
                AdsManager.preserveNativeAdsForActivityRecreate()
                activity.recreateWithoutTransition()
            }
        }
    }

//
//    fun openHomeWithBackAd() {
//        val activity = context.findActivity()
//        val cameFromElsewhere = currentStep != AppStep.Home
//        if (activity == null || !cameFromElsewhere) {
//            viewModel.openHome()
//            return
//        }
//        viewModel.openHome() // navigate immediately, never blocked on ad load
//        AdsManager.showInterBackIfReady(activity) { adShown ->
//            if (adShown) {
//                // Flow 3 spec: "skip the Rate dialog only for that trigger" —
//                // deliberately do NOT call markDialogShown() here, so the next
//                // eligible trigger is still checked fresh.
//                return@showInterBackIfReady
//            }
//            // No ad shown -> Flow 3: user returned to Home with no interstitial.
//            if (ratingManager.canShowRatingDialog()) {
//                showHomeRatingDialog = true
//                ratingManager.markDialogShown()
//            }
//        }
//    }

    fun openHomeWithBackAd() {
        val activity = context.findActivity()
        
        // First, try to use the navigation stack via handleBackNavigation
        // This respects the proper back history rather than always going to Shorts
        val handled = viewModel.handleBackNavigation()
        if (handled) {
            // Back navigation worked - show interstitial if appropriate
            activity?.let { 
                AdsManager.showInterBackIfReady(it) { adShown ->
                    if (adShown) {
                        if (pendingRatingOnHome && ratingManager.canShowRatingDialog()) {
                            showHomeRatingDialog = true
                            ratingManager.markDialogShown()
                        }
                        pendingRatingOnHome = false
                        return@showInterBackIfReady
                    }
                    // No ad shown - show rating if eligible
                    if (pendingRatingOnHome && ratingManager.canShowRatingDialog()) {
                        showHomeRatingDialog = true
                        ratingManager.markDialogShown()
                    } else if (!pendingRatingOnHome && ratingManager.canShowRatingDialog()) {
                        showHomeRatingDialog = true
                        ratingManager.markDialogShown()
                    }
                    pendingRatingOnHome = false
                }
            }
            return
        }
        
        // If we're already on Home or back navigation returned false, show rating if applicable
        if (pendingRatingOnHome && ratingManager.canShowRatingDialog()) {
            showHomeRatingDialog = true
            ratingManager.markDialogShown()
            pendingRatingOnHome = false
        }
    }

    // Global back button handler for system back navigation
    BackHandler(enabled = true) {
        openHomeWithBackAd()
    }

    when (currentStep) {
        AppStep.Splash -> CustomSplashScreen(
            onFinished = {
                viewModel.onSplashFinished()
            }
        )

        AppStep.SplashUninstall -> CustomSplashScreen(
            uninstallFlow = true,
            onFinished = {
                viewModel.onUninstallSplashFinished()
            }
        )

        AppStep.Language -> {
            LanguageScreen(
                delayDoneAfterSelection = uiState.delayDoneLanguage,
                onContinue = { language ->
                    viewModel.onLanguageFinished(language)
                }
            )
        }

        AppStep.Onboarding -> OnboardingScreen(
            onEntered = viewModel::onOnboardingEntered,
            onFinished = {
                if (onboardingFinishInProgress) {
                    return@OnboardingScreen
                }
                onboardingFinishInProgress = true
                viewModel.onOnboardingFinished()
            }
        )

        AppStep.WelcomeBack -> WelcomeBackScreen(
            onFinished = {
                val activity = context.findActivity()
                if (activity != null) {
                    AdsManager.showInterWelcome(activity) {
                        viewModel.onWelcomeBackFinished()
                    }
                } else {
                    viewModel.onWelcomeBackFinished()
                }
            }
        )

        AppStep.ConfirmUninstall -> ConfirmUninstallScreen(
            onBackHome = viewModel::returnFromUninstallPrompt,
            onStillUninstall = viewModel::openSurveyUninstall
        )

        AppStep.SurveyUninstall -> SurveyUninstallScreen(
            onBackHome = viewModel::returnFromUninstallPrompt
        )

//        AppStep.Home -> HomeScreen(
//            backendBaseUrl = uiState.backendBaseUrl,
//            shouldTriggerRating = pendingRatingOnHome,           // NEW
//            onRatingTriggered = { pendingRatingOnHome = false },
//            onOpenEpisodes = { filmId ->
//                // Open Episodes screen when clicking a film (episode number handled elsewhere)
//                viewModel.openEpisodes(filmId)
//            },
//            onOpenShorts = {
//                // Open generic Shorts screen for casual browsing
//                viewModel.openShorts(null)
//            },
//            onLibrary = viewModel::openLibrary,
//            onSearch = viewModel::openSearch,
//            onRewards = viewModel::openRewards,
//            onNotifications = viewModel::openNotifications,
//            onProfile = viewModel::openProfile
//        )

        AppStep.Home -> HomeScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onOpenEpisodes = { filmId ->
                viewModel.openEpisodes(filmId)
            },
            onOpenShorts = {
                viewModel.openShorts(null)
            },
            onLibrary = viewModel::openLibrary,
            onSearch = viewModel::openSearch,
            onRewards = viewModel::openRewards,
            onNotifications = viewModel::openNotifications,
            onProfile = viewModel::openProfile
        )

//        AppStep.Shorts -> ShortsScreen(
//            backendBaseUrl = uiState.backendBaseUrl,
//            initialFilmId = uiState.selectedShortFilmId,
//            initialEpisodeNumber = uiState.selectedEpisodeNumber,
//            onBack = ::openHomeWithBackAd,
//            onHome = ::openHomeWithBackAd,
//            onLibrary = viewModel::openLibrary,
//            onRewards = viewModel::openRewards,
//            onProfile = viewModel::openProfile,
//            onNavigateToEpisodes = viewModel::openEpisodes,
//            onRequestRatingOnHome = { pendingRatingOnHome = true }
//        )

        AppStep.Shorts -> ShortsScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            initialFilmId = uiState.selectedShortFilmId,
            initialEpisodeNumber = uiState.selectedEpisodeNumber,
            onBack = ::openHomeWithBackAd,
            onHome = ::openHomeWithBackAd,
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile,
            onNavigateToEpisodes = viewModel::openEpisodes,
            onRequestRatingOnHome = { pendingRatingOnHome = true }
        )

//        AppStep.Episodes -> ShortsScreen(
//            backendBaseUrl = uiState.backendBaseUrl,
//            initialFilmId = uiState.selectedEpisodeFilmId ?: uiState.selectedShortFilmId,
//            initialEpisodeNumber = uiState.selectedEpisodeNumber,
//            onBack = ::openHomeWithBackAd,
//            onHome = ::openHomeWithBackAd,
//            onLibrary = viewModel::openLibrary,
//            onRewards = viewModel::openRewards,
//            onProfile = viewModel::openProfile,
//            onNavigateToEpisodes = viewModel::openEpisodes,
//            onRequestRatingOnHome = { pendingRatingOnHome = true }
//        )

        AppStep.Episodes -> ShortsScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            initialFilmId = uiState.selectedEpisodeFilmId ?: uiState.selectedShortFilmId,
            initialEpisodeNumber = uiState.selectedEpisodeNumber,
            onBack = ::openHomeWithBackAd,
            onHome = ::openHomeWithBackAd,
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile,
            onNavigateToEpisodes = viewModel::openEpisodes,
            onRequestRatingOnHome = { pendingRatingOnHome = true }
        )

        AppStep.Library -> LibraryScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onHome = ::openHomeWithBackAd,
            onShorts = { viewModel.openShorts(null) },
            onOpenShorts = viewModel::openShorts,
            onSearch = viewModel::openSearch,
            onRewards = viewModel::openRewards,
            onPlanner = viewModel::openPlanner,
            onProfile = viewModel::openProfile
        )

        AppStep.Search -> SearchResultsScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            query = uiState.searchQuery,
            onBack = ::openHomeWithBackAd,
            onHome = ::openHomeWithBackAd,
            onShorts = { viewModel.openShorts(null) },
            onLibrary = viewModel::openLibrary,
            onOpenShorts = viewModel::openShorts,
            onSearch = viewModel::openSearch,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile
        )

        AppStep.Rewards -> RewardScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onHome = ::openHomeWithBackAd,
            onShorts = { viewModel.openShorts(null) },
            onLibrary = viewModel::openLibrary,
            onProfile = viewModel::openProfile
        )

        AppStep.Profile -> ProfileScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onHome = ::openHomeWithBackAd,
            onShorts = { viewModel.openShorts(null) },
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
        )

        AppStep.Planner -> PlannerScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onBack = viewModel::openLibrary,
            onHome = ::openHomeWithBackAd,
            onShorts = { viewModel.openShorts(null) },
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile
        )

        AppStep.Notifications -> NotificationScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onBack = ::openHomeWithBackAd,
            onHome = ::openHomeWithBackAd,
            onShorts = { viewModel.openShorts(null) },
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile
        )
    }

    if (showHomeRatingDialog && currentStep == AppStep.Home) {
        AppRatingDialog(
            onDismiss = { showHomeRatingDialog = false },
            onRated = { showHomeRatingDialog = false }
        )
    }

//    NotificationPermissionRequester(currentStep = uiState.currentStep)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
private fun Activity.recreateWithoutTransition() {
    overridePendingTransition(0, 0)
    recreate()
    overridePendingTransition(0, 0)
}

//@Composable
//private fun NotificationPermissionRequester(currentStep: AppStep) {
//    val context = LocalContext.current
//    val permissionLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestPermission(),
//        onResult = {}
//    )
//
//    LaunchedEffect(currentStep) {
//        if (currentStep == AppStep.Splash || currentStep == AppStep.SplashUninstall) {
//            return@LaunchedEffect
//        }
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//            return@LaunchedEffect
//        }
//
//        val permission = Manifest.permission.POST_NOTIFICATIONS
//        val isGranted =
//            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
//        if (!isGranted) {
//            AdsManager.suppressResumeInterstitialForExternalDialog("notification_permission_dialog")
//            permissionLauncher.launch(permission)
//        }
//    }
//}

@Composable
private fun NotificationPermissionRequester() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@LaunchedEffect
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val isGranted =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            AdsManager.suppressResumeInterstitialForExternalDialog("notification_permission_dialog")
            permissionLauncher.launch(permission)
        }
    }
}
