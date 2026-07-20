package com.drama.x.drama.series.dramax.dramaseries

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.drama.x.drama.series.dramax.dramaseries.ads.AdRemoteConfig
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.ads.isNetworkAvailable
import com.drama.x.drama.series.dramax.dramaseries.model.AppStep
import com.drama.x.drama.series.dramax.dramaseries.model.AppViewModel
import com.drama.x.drama.series.dramax.dramaseries.screen.ConfirmUninstallScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.CustomSplashScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.EpisodeScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.HomeScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.LanguageScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.LibraryScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.NoInternetScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.NotificationScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.OnboardingScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.PlannerScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.ProfileScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.RewardScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.SearchResultsScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.ShortsScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.SurveyUninstallScreen
import kotlinx.coroutines.delay

@Composable
fun DramaXApp(
    initialAction: String? = null,
    viewModel: AppViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var onboardingFinishInProgress by remember { mutableStateOf(false) }
    var hasNetwork by remember(context) { mutableStateOf(isNetworkAvailable(context)) }

    LaunchedEffect(context) {
        while (true) {
            hasNetwork = isNetworkAvailable(context)
            delay(35_000L)
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
            MainActivity.ACTION_WIDGET_UNINSTALL -> viewModel.startWidgetUninstallFlow()
        }
    }

    if (!hasNetwork) {
        NoInternetScreen(
            onRetry = {
                hasNetwork = isNetworkAvailable(context)
            }
        )
        return
    }

    when (uiState.currentStep) {
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
                val activity = context.findActivity()
                if (activity == null) {
                    viewModel.onOnboardingFinished()
                } else {
                    AdsManager.loadAndShowInterstitial(
                        activity = activity,
                        placementName = "inter_onboarding",
                        config = AdRemoteConfig.interOnboarding,
                        timeoutMs = 12_000L,
                        bypassInterstitialInterval = true,
                        onFinished = viewModel::onOnboardingFinished
                    )
                }
            }
        )

        AppStep.ConfirmUninstall -> ConfirmUninstallScreen(
            onBackHome = viewModel::openHome,
            onStillUninstall = viewModel::openSurveyUninstall
        )

        AppStep.SurveyUninstall -> SurveyUninstallScreen(
            onBackHome = viewModel::openHome
        )

        AppStep.Home -> HomeScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onOpenShorts = viewModel::openEpisodes,
            onLibrary = viewModel::openLibrary,
            onSearch = viewModel::openSearch,
            onRewards = viewModel::openRewards,
            onNotifications = viewModel::openNotifications,
            onProfile = viewModel::openProfile
        )

        AppStep.Shorts -> ShortsScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            initialFilmId = null,
            onBack = viewModel::openHome,
            onWatchFull = viewModel::openEpisodes,
            onHome = viewModel::openHome,
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile
        )

        AppStep.Episodes -> EpisodeScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            filmId = uiState.selectedShortFilmId,
            onBack = viewModel::openHome,
            onHome = viewModel::openHome,
            onShorts = { viewModel.openShorts() },
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile
        )

        AppStep.Library -> LibraryScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onHome = viewModel::openHome,
            onShorts = { viewModel.openShorts() },
            onOpenShorts = viewModel::openEpisodes,
            onSearch = viewModel::openSearch,
            onRewards = viewModel::openRewards,
            onPlanner = viewModel::openPlanner,
            onProfile = viewModel::openProfile
        )

        AppStep.Search -> SearchResultsScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            query = uiState.searchQuery,
            onBack = viewModel::openHome,
            onHome = viewModel::openHome,
            onShorts = { viewModel.openShorts() },
            onLibrary = viewModel::openLibrary,
            onOpenShorts = viewModel::openEpisodes,
            onSearch = viewModel::openSearch,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile
        )

        AppStep.Rewards -> RewardScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onHome = viewModel::openHome,
            onShorts = { viewModel.openShorts() },
            onLibrary = viewModel::openLibrary,
            onProfile = viewModel::openProfile
        )

        AppStep.Profile -> ProfileScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            currentLanguage = uiState.selectedLanguage ?: "English",
            onHome = viewModel::openHome,
            onShorts = { viewModel.openShorts() },
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onLanguage = viewModel::openLanguage,
            onWatchHistory = viewModel::openLibrary,
            onMyWatchlist = viewModel::openLibrary
        )

        AppStep.Planner -> PlannerScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onBack = viewModel::openLibrary,
            onHome = viewModel::openHome,
            onShorts = { viewModel.openShorts() },
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile
        )

        AppStep.Notifications -> NotificationScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onBack = viewModel::openHome,
            onHome = viewModel::openHome,
            onShorts = { viewModel.openShorts() },
            onLibrary = viewModel::openLibrary,
            onRewards = viewModel::openRewards,
            onProfile = viewModel::openProfile
        )
    }

    NotificationPermissionRequester(currentStep = uiState.currentStep)
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

@Composable
private fun NotificationPermissionRequester(currentStep: AppStep) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(currentStep) {
        if (currentStep != AppStep.Home) {
            return@LaunchedEffect
        }
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
