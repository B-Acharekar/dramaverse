package com.drama.x.drama.series.dramax.dramaseries.model

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drama.x.drama.series.dramax.dramaseries.data.AuthRepository
import com.drama.x.drama.series.dramax.dramaseries.data.HomeRepository
import com.drama.x.drama.series.dramax.dramaseries.data.LocaleHelper
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
private const val PREFS_NAME = "dramaverse_onboarding"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

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
    val isFirstLaunch: Boolean = true,
    val widgetUninstallAutoRedirect: Boolean = false

)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val authRepository = AuthRepository(application.applicationContext)
    private val homeRepository = HomeRepository(application.applicationContext, authRepository)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        _uiState.update { it.copy(currentStep = AppStep.Language, widgetUninstallAutoRedirect = false) }
    }

    fun onLanguageFinished(language: String) {
        val nextStep = AppStep.Onboarding
        cancelWidgetUninstallRedirect()
        LocaleHelper.persistLanguage(appContext, language)
        pendingPostLanguageRecreate = true
        _uiState.update {
            it.copy(
                selectedLanguage = language,
                currentStep = nextStep,
                recreateRequested = true,
                widgetUninstallAutoRedirect = false
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
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        _uiState.update { it.copy(currentStep = AppStep.Home, widgetUninstallAutoRedirect = false) }
    }

    fun startWidgetHome() {
        cancelWidgetUninstallRedirect()
        _uiState.update {
            it.copy(
                currentStep = AppStep.Home,
                selectedShortFilmId = null,
                widgetUninstallAutoRedirect = false
            )
        }
    }

    fun startWidgetUninstallFlow() {
        cancelWidgetUninstallRedirect()
        _uiState.update {
            it.copy(
                currentStep = AppStep.SplashUninstall,
                selectedShortFilmId = null,
                widgetUninstallAutoRedirect = true
            )
        }
    }

    fun onWidgetUninstallConfirmAdReady() {
        val state = _uiState.value
        if (!state.widgetUninstallAutoRedirect || state.currentStep != AppStep.ConfirmUninstall) return
        _uiState.update {
            it.copy(
                currentStep = AppStep.Language,
                selectedShortFilmId = null,
                widgetUninstallAutoRedirect = false
            )
        }
    }

    fun onUninstallSplashFinished() {
        _uiState.update { it.copy(currentStep = AppStep.ConfirmUninstall) }
    }

    fun openSurveyUninstall() {
        cancelWidgetUninstallRedirect()
        _uiState.update { it.copy(currentStep = AppStep.SurveyUninstall, widgetUninstallAutoRedirect = false) }
    }

    fun openHome() {
        cancelWidgetUninstallRedirect()
        _uiState.update {
            it.copy(
                currentStep = AppStep.Home,
                selectedShortFilmId = null,
                widgetUninstallAutoRedirect = false
            )
        }
    }

    fun returnFromUninstallPrompt() {
        cancelWidgetUninstallRedirect()
        val destination = if (prefs.getBoolean(KEY_ONBOARDING_DONE, false)) {
            AppStep.Home
        } else {
            AppStep.Language
        }
        _uiState.update {
            it.copy(
                currentStep = destination,
                selectedShortFilmId = null,
                widgetUninstallAutoRedirect = false
            )
        }
    }

    fun openShorts(filmId: Int? = null) {
        _uiState.update {
            it.copy(
                currentStep = AppStep.Shorts,
                selectedShortFilmId = filmId,
                widgetUninstallAutoRedirect = false
            )
        }
    }
    fun openProfile() {
        _uiState.update { it.copy(currentStep = AppStep.Profile, selectedShortFilmId = null, widgetUninstallAutoRedirect = false) }
    }

    fun openLibrary() {
        _uiState.update { it.copy(currentStep = AppStep.Library, selectedShortFilmId = null, widgetUninstallAutoRedirect = false) }
    }

    fun openRewards() {
        _uiState.update { it.copy(currentStep = AppStep.Rewards, selectedShortFilmId = null, widgetUninstallAutoRedirect = false) }
    }

    fun openPlanner() {
        _uiState.update { it.copy(currentStep = AppStep.Planner, selectedShortFilmId = null, widgetUninstallAutoRedirect = false) }
    }

    fun openNotifications() {
        _uiState.update { it.copy(currentStep = AppStep.Notifications, selectedShortFilmId = null, widgetUninstallAutoRedirect = false) }
    }

    fun openSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _uiState.update {
            it.copy(
                currentStep = AppStep.Search,
                selectedShortFilmId = null,
                searchQuery = trimmed,
                widgetUninstallAutoRedirect = false
            )
        }
    }

    private fun cancelWidgetUninstallRedirect() {
        _uiState.update { it.copy(widgetUninstallAutoRedirect = false) }
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
