package com.drama.x.drama.series.dramax.dramaseries.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drama.x.drama.series.dramax.dramaseries.data.AuthRepository
import com.drama.x.drama.series.dramax.dramaseries.data.HomeRepository
import com.drama.x.drama.series.dramax.dramaseries.data.LocaleHelper
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.ads.RemoteConfigUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.onesignal.OneSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DramaX"
private const val RC_DELAY_DONE_LANGUAGE = "delay_button_done_language"
private const val RC_ONESIGNAL_APP_ID = "onesignal_app_id"
private const val RC_BACKEND_BASE_URL = "backend_base_url"
private const val DEFAULT_BACKEND_URL = "https://dramaverse-backend-lbq5.onrender.com"

enum class AppStep {
    Splash,
    Language,
    Onboarding,
    Home,
    Shorts,
    Library,
    Search,
    Rewards,
    Profile,
    Planner,
    Notifications,
    SplashUninstall,
    ConfirmUninstall,
    SurveyUninstall
}

data class AppUiState(
    val currentStep: AppStep = AppStep.Splash,
    val delayDoneLanguage: Boolean = false,
    val backendBaseUrl: String = DEFAULT_BACKEND_URL,
    val selectedLanguage: String? = null,
    val selectedShortFilmId: Int? = null,
    val searchQuery: String = "",
    val recreateRequested: Boolean = false,
    val isFirstLaunch: Boolean = true

)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val authRepository = AuthRepository(application.applicationContext)
    private val homeRepository = HomeRepository(application.applicationContext, authRepository)
    private val remoteConfig = RemoteConfigUtils.configure(application.applicationContext)
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var oneSignalInitializedAppId: String? = null

    companion object {
        @Volatile
        private var pendingPostLanguageRecreate = false
    }

    init {
        applyRemoteValues(remoteConfig)
    }

    fun onSplashFinished() {
        RemoteConfigUtils.applyCached(remoteConfig)

        if (pendingPostLanguageRecreate) {
            // We're resuming from the locale-reload recreate() that
            // immediately follows language selection in this same session.
            pendingPostLanguageRecreate = false
            val language = LocaleHelper.persistedLanguageName(appContext)
                ?: _uiState.value.selectedLanguage
                ?: "English"
            _uiState.update { it.copy(currentStep = AppStep.Onboarding, selectedLanguage = language) }
            registerDevice(language)
            return
        }

        // Every other splash finish — including every fresh app open — always
        // starts the user back at Language.
        _uiState.update { it.copy(currentStep = AppStep.Language) }
    }

    fun onLanguageFinished(language: String) {
        val nextStep = AppStep.Onboarding
        LocaleHelper.persistLanguage(appContext, language)
        pendingPostLanguageRecreate = true
        _uiState.update {
            it.copy(
                selectedLanguage = language,
                currentStep = nextStep,
                recreateRequested = true
            )
        }
        registerDevice(language)
    }

    fun onRecreateHandled() {
        _uiState.update { it.copy(recreateRequested = false) }
    }

    fun onOnboardingEntered() {
        registerDevice(_uiState.value.selectedLanguage)
    }

    fun onOnboardingFinished() {
        _uiState.update { it.copy(currentStep = AppStep.Home) }
    }

    fun startWidgetHome() {
        _uiState.update {
            it.copy(
                currentStep = AppStep.Home,
                selectedShortFilmId = null
            )
        }
    }

    fun startWidgetUninstallFlow() {
        pendingPostLanguageRecreate = false
        AdsManager.clearUninstallAds()
        _uiState.update {
            it.copy(
                currentStep = AppStep.SplashUninstall,
                selectedShortFilmId = null
            )
        }
    }

    fun onUninstallSplashFinished() {
        _uiState.update { it.copy(currentStep = AppStep.ConfirmUninstall) }
    }

    fun openSurveyUninstall() {
        _uiState.update { it.copy(currentStep = AppStep.SurveyUninstall) }
    }

    fun openHome() {
        _uiState.update {
            it.copy(
                currentStep = AppStep.Home,
                selectedShortFilmId = null
            )
        }
    }

    fun returnFromUninstallPrompt() {
        pendingPostLanguageRecreate = false
        _uiState.update {
            it.copy(
                currentStep = AppStep.Language,
                selectedShortFilmId = null
            )
        }
    }

    fun openShorts(filmId: Int? = null) {
        _uiState.update {
            it.copy(
                currentStep = AppStep.Shorts,
                selectedShortFilmId = filmId
            )
        }
    }
    fun openProfile() {
        _uiState.update { it.copy(currentStep = AppStep.Profile, selectedShortFilmId = null) }
    }

    fun openLibrary() {
        _uiState.update { it.copy(currentStep = AppStep.Library, selectedShortFilmId = null) }
    }

    fun openRewards() {
        _uiState.update { it.copy(currentStep = AppStep.Rewards, selectedShortFilmId = null) }
    }

    fun openPlanner() {
        _uiState.update { it.copy(currentStep = AppStep.Planner, selectedShortFilmId = null) }
    }

    fun openNotifications() {
        _uiState.update { it.copy(currentStep = AppStep.Notifications, selectedShortFilmId = null) }
    }

    fun openSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _uiState.update {
            it.copy(
                currentStep = AppStep.Search,
                selectedShortFilmId = null,
                searchQuery = trimmed
            )
        }
    }

    private fun applyRemoteValues(config: FirebaseRemoteConfig?) {
        config ?: return
        val backendUrl = config.getString(RC_BACKEND_BASE_URL).ifBlank { DEFAULT_BACKEND_URL }
        _uiState.update {
            it.copy(
                delayDoneLanguage = config.getBoolean(RC_DELAY_DONE_LANGUAGE),
                backendBaseUrl = backendUrl
            )
        }

        val oneSignalAppId = config.getString(RC_ONESIGNAL_APP_ID)
        if (oneSignalAppId.isNotBlank() && oneSignalInitializedAppId != oneSignalAppId) {
            runCatching {
                OneSignal.initWithContext(getApplication(), oneSignalAppId)
                oneSignalInitializedAppId = oneSignalAppId
            }.onFailure {
                Log.w(TAG, "OneSignal initialization skipped.", it)
            }
        }
    }

    private fun registerDevice(language: String?) {
        viewModelScope.launch {
            authRepository.registerDevice(
                backendBaseUrl = _uiState.value.backendBaseUrl,
                language = language ?: "en"
            ).onSuccess {
                prefetchHome()
            }.onFailure {
                Log.w(TAG, "Device auth call failed.", it)
            }
        }
    }

    private fun prefetchHome() {
        viewModelScope.launch {
            homeRepository.refreshHome(
                backendBaseUrl = _uiState.value.backendBaseUrl,
                language = LocaleHelper.persistedLanguageCode(appContext)
            ).onFailure {
                Log.w(TAG, "Home prefetch failed.", it)
            }
        }
    }
}
