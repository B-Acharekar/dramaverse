package app.dramaverse.stream

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.model.AppStep
import app.dramaverse.stream.model.AppViewModel
import app.dramaverse.stream.screen.CustomSplashScreen
import app.dramaverse.stream.screen.HomeScreen
import app.dramaverse.stream.screen.LanguageScreen
import app.dramaverse.stream.screen.LibraryScreen
import app.dramaverse.stream.screen.OnboardingScreen
import app.dramaverse.stream.screen.SearchResultsScreen
import app.dramaverse.stream.screen.ShortsScreen

@Composable
fun DramaVerseApp(viewModel: AppViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.currentStep) {
        AppStep.Splash -> CustomSplashScreen(
            onFinished = {
                viewModel.onSplashFinished()
            }
        )

        AppStep.Language -> {
            LanguageScreen(
                delayDoneAfterSelection = uiState.delayDoneLanguage,
                onContinue = { language ->
                    viewModel.onLanguageFinished(language)
                }
            )
            NotificationPermissionRequester()
        }

        AppStep.Onboarding -> OnboardingScreen(
            onEntered = viewModel::onOnboardingEntered,
            onFinished = viewModel::onOnboardingFinished
        )

        AppStep.Home -> HomeScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onOpenShorts = viewModel::openShorts,
            onLibrary = viewModel::openLibrary,
            onSearch = viewModel::openSearch
        )

        AppStep.Shorts -> ShortsScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            initialFilmId = uiState.selectedShortFilmId,
            onBack = viewModel::openHome,
            onHome = viewModel::openHome,
            onLibrary = viewModel::openLibrary
        )

        AppStep.Library -> LibraryScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            onHome = viewModel::openHome,
            onShorts = { viewModel.openShorts(null) },
            onOpenShorts = viewModel::openShorts,
            onSearch = viewModel::openSearch
        )

        AppStep.Search -> SearchResultsScreen(
            backendBaseUrl = uiState.backendBaseUrl,
            query = uiState.searchQuery,
            onBack = viewModel::openHome,
            onHome = viewModel::openHome,
            onShorts = { viewModel.openShorts(null) },
            onLibrary = viewModel::openLibrary,
            onOpenShorts = viewModel::openShorts,
            onSearch = viewModel::openSearch
        )
    }
}

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
            permissionLauncher.launch(permission)
        }
    }
}
