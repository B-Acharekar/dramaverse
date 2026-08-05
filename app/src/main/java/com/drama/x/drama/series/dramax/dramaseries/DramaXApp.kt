package com.drama.x.drama.series.dramax.dramaseries

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
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

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun DramaXApp(
    initialAction: String? = null,
    viewModel: AppViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var onboardingFinishInProgress by remember { mutableStateOf(false) }
    
    // Flow 3: Track navigation for Return to Home trigger
    var previousStep by remember { mutableStateOf<AppStep?>(null) }
    var wasInterstitialShown by remember { mutableStateOf(false) }
    var shouldTriggerHomeRating by remember { mutableStateOf(false) }
    
    val currentStep = when {
        initialAction == MainActivity.ACTION_WIDGET_UNINSTALL && uiState.currentStep == AppStep.Splash ->
            AppStep.SplashUninstall
        initialAction == MainActivity.ACTION_WIDGET_HOME && uiState.currentStep == AppStep.Splash ->
            AppStep.Home
        initialAction == MainActivity.ACTION_WIDGET_MY_LIST && uiState.currentStep == AppStep.Splash ->
            AppStep.Library
        else -> uiState.currentStep
    }
    
    // Flow 3: Detect return to Home and trigger rating
    LaunchedEffect(currentStep) {
        if (currentStep == AppStep.Home && 
            previousStep != null && 
            previousStep != AppStep.Splash && 
            previousStep != AppStep.SplashUninstall &&
            previousStep != AppStep.Language &&
            previousStep != AppStep.Onboarding &&
            previousStep != AppStep.WelcomeBack) {
            
            val triggerHelper = com.drama.x.drama.series.dramax.dramaseries.data.RatingTriggerHelper.getInstance(context)
            val shouldShowRating = triggerHelper.shouldTriggerOnReturnToHome(
                interstitialShown = wasInterstitialShown
            )
            if (shouldShowRating) {
                shouldTriggerHomeRating = true
                triggerHelper.markDialogShown()
            }
        }
        
        previousStep = currentStep
        wasInterstitialShown = false // Reset for next navigation
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (com.drama.x.drama.series.dramax.dramaseries.GlobalApp.shouldShowWelcomeBackOnResume) {
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

    LaunchedEffect(initialAction) {
        when (initialAction) {
            MainActivity.ACTION_WIDGET_HOME -> viewModel.startWidgetHome()
            MainActivity.ACTION_WIDGET_MY_LIST -> viewModel.startWidgetMyList()
            MainActivity.ACTION_WIDGET_UNINSTALL -> viewModel.startWidgetUninstallFlow()
        }
    }

    fun openHomeWithBackAd() {
        val activity = context.findActivity()
        if (activity == null || currentStep == AppStep.Home) {
            viewModel.openHome()
            return
        }
        AdsManager.loadAndShowInterstitial(
            activity = activity,
            placementName = "inter_back",
            config = AdRemoteConfig.interBack,
            timeoutMs = 2_500L,
            onFinished = {
                wasInterstitialShown = true // Mark that ad was shown
                viewModel.openHome()
            }
        )
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

        AppStep.Home -> HomeScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            shouldTriggerRating = shouldTriggerHomeRating,
            onRatingTriggered = { shouldTriggerHomeRating = false },
            onOpenEpisodes = { filmId ->
                // Open Episodes screen when clicking a film (episode number handled elsewhere)
                viewModel.openEpisodes(filmId)
            },
            onOpenShorts = {
                // Open generic Shorts screen for casual browsing
                viewModel.openShorts(null)
            },
            onLibrary = viewModel::openLibrary,
            onSearch = viewModel::openSearch,
            onRewards = viewModel::openRewards,
            onNotifications = viewModel::openNotifications,
            onProfile = viewModel::openProfile
        )

        AppStep.Shorts -> ShortsScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            initialFilmId = uiState.selectedShortFilmId,
            initialEpisodeNumber = uiState.selectedEpisodeNumber,
            onBack = ::openHomeWithBackAd,
            onHome = ::openHomeWithBackAd,
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile,
            onNavigateToEpisodes = viewModel::openEpisodes
        )

        AppStep.Episodes -> ShortsScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            initialFilmId = uiState.selectedEpisodeFilmId ?: uiState.selectedShortFilmId,
            initialEpisodeNumber = uiState.selectedEpisodeNumber,
            onBack = ::openHomeWithBackAd,
            onHome = ::openHomeWithBackAd,
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile,
            onNavigateToEpisodes = viewModel::openEpisodes
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
