package com.drama.x.drama.series.dramax.dramaseries

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drama.x.drama.series.dramax.dramaseries.model.AppStep
import com.drama.x.drama.series.dramax.dramaseries.model.AppViewModel
import com.drama.x.drama.series.dramax.dramaseries.screen.CustomSplashScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.LanguageScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.OnboardingScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.ConfirmUninstallScreen
import com.drama.x.drama.series.dramax.dramaseries.screen.SurveyUninstallScreen
import com.drama.x.drama.series.dramax.dramaseries.ads.AdRemoteConfig
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager

@Composable
fun DramaXApp(
    initialAction: String? = null,
    viewModel: AppViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var onboardingFinishInProgress by remember { mutableStateOf(false) }

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
            autoRedirectAfterAdReady = uiState.widgetUninstallAutoRedirect,
            onAdReadyForAutoRedirect = viewModel::onWidgetUninstallConfirmAdReady,
            onBackHome = viewModel::returnFromUninstallPrompt,
            onStillUninstall = viewModel::openSurveyUninstall
        )

        AppStep.SurveyUninstall -> SurveyUninstallScreen(
            onBackHome = viewModel::returnFromUninstallPrompt
        )

        // Full product surfaces are intentionally disabled for the current ads/onboarding/uninstall QA build.
        AppStep.Home,
        AppStep.Shorts,
        AppStep.Library,
        AppStep.Search,
        AppStep.Rewards,
        AppStep.Profile,
        AppStep.Planner,
        AppStep.Notifications -> LimitedBuildWelcomeScreen()
    }

//    NotificationPermissionRequester(currentStep = uiState.currentStep)
}

//@Composable
//private fun LimitedBuildWelcomeScreen() {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xFF111113)),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            text = stringResource(R.string.welcome_to_home_temp),
//            color = Color.White,
//            fontSize = 26.sp,
//            fontWeight = FontWeight.ExtraBold,
//            textAlign = TextAlign.Center,
//            letterSpacing = 0.sp
//        )
//    }
//    NotificationPermissionRequester()
//}

@Composable
private fun LimitedBuildWelcomeScreen() {
    var isFullyLoaded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111113))
            .onGloballyPositioned {
                if (!isFullyLoaded) {
                    isFullyLoaded = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.welcome_to_home_temp),
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp
        )
    }
    if (isFullyLoaded) {
        NotificationPermissionRequester()
    }
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
