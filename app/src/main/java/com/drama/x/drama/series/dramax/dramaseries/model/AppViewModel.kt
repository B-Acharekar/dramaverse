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
private const val DEFAULT_BACKEND_URL = "https://drama-verse-backend.vercel.app"
private const val PREFS_NAME = "dramaverse_onboarding"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

enum class AppStep {
    Splash,
    Language,
    Onboarding,
    Home,
    Shorts,
    Episodes,
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
    val recreateRequested: Boolean = false
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

    init {
        applyRemoteValues(remoteConfig)
    }

    fun onSplashFinished() {
        RemoteConfigUtils.applyCached(remoteConfig)
        // Language selection triggers Activity recreation; this pending step restores the intended screen.
        val onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        LocaleHelper.consumePendingStep(appContext)?.let { pending ->
            val pendingStep = runCatching { AppStep.valueOf(pending) }.getOrNull()
            val restoredStep = when {
                pendingStep == AppStep.Home && onboardingDone -> AppStep.Home
                pendingStep == AppStep.Profile && onboardingDone -> AppStep.Profile
                pendingStep == AppStep.Onboarding && !onboardingDone -> AppStep.Onboarding
                else -> null
            }
            if (restoredStep != null) {
                val language = LocaleHelper.persistedLanguageName(appContext) ?: _uiState.value.selectedLanguage ?: "English"
                _uiState.update { it.copy(currentStep = restoredStep, selectedLanguage = language) }
                registerDevice(language)
                return
            }
        }
        if (onboardingDone) {
            val language = LocaleHelper.persistedLanguageName(appContext) ?: "English"
            _uiState.update { it.copy(currentStep = AppStep.Home, selectedLanguage = language) }
            registerDevice(language)
        } else {
            _uiState.update { it.copy(currentStep = AppStep.Language) }
        }
    }

    fun onLanguageFinished(language: String) {
        val nextStep = if (prefs.getBoolean(KEY_ONBOARDING_DONE, false)) AppStep.Home else AppStep.Onboarding
        LocaleHelper.persistLanguage(appContext, language)
        LocaleHelper.persistPendingStep(appContext, nextStep.name)
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
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        LocaleHelper.persistPendingStep(appContext, AppStep.Home.name)
        _uiState.update { it.copy(currentStep = AppStep.Home) }
    }

    fun startWidgetHome() {
        _uiState.update { it.copy(currentStep = AppStep.Home, selectedShortFilmId = null) }
    }

    fun startWidgetUninstallFlow() {
        _uiState.update { it.copy(currentStep = AppStep.SplashUninstall, selectedShortFilmId = null) }
    }

    fun onUninstallSplashFinished() {
        _uiState.update { it.copy(currentStep = AppStep.ConfirmUninstall) }
    }

    fun openSurveyUninstall() {
        _uiState.update { it.copy(currentStep = AppStep.SurveyUninstall) }
    }

    fun openHome() {
        _uiState.update { it.copy(currentStep = AppStep.Home, selectedShortFilmId = null) }
    }

    fun openShorts(filmId: Int? = null) {
        _uiState.update { it.copy(currentStep = AppStep.Shorts, selectedShortFilmId = filmId) }
    }

    fun openEpisodes(filmId: Int?) {
        if (filmId == null || filmId == 0) {
            openShorts()
            return
        }
        _uiState.update { it.copy(currentStep = AppStep.Episodes, selectedShortFilmId = filmId) }
    }

    fun openProfile() {
        _uiState.update { it.copy(currentStep = AppStep.Profile, selectedShortFilmId = null) }
    }

    fun openLanguage() {
        _uiState.update { it.copy(currentStep = AppStep.Language, selectedShortFilmId = null) }
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
        _uiState.update {
            it.copy(
                delayDoneLanguage = config.getBoolean(RC_DELAY_DONE_LANGUAGE),
                backendBaseUrl = DEFAULT_BACKEND_URL
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
