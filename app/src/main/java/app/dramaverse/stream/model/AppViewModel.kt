package app.dramaverse.stream.model

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dramaverse.stream.data.AuthRepository
import app.dramaverse.stream.data.HomeRepository
import app.dramaverse.stream.data.LocaleHelper
import app.dramaverse.stream.R
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.onesignal.OneSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DramaVerse"
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
    Search
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
    private val remoteConfig = configureRemoteConfig(application.applicationContext)
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var oneSignalInitializedAppId: String? = null

    init {
        loadRemoteConfig()
    }

    fun onSplashFinished() {
        // Language selection triggers Activity recreation; this pending step restores the intended screen.
        LocaleHelper.consumePendingStep(appContext)?.let { pending ->
            val pendingStep = runCatching { AppStep.valueOf(pending) }.getOrNull()
            if (pendingStep == AppStep.Onboarding || pendingStep == AppStep.Home) {
                val language = LocaleHelper.persistedLanguageName(appContext) ?: _uiState.value.selectedLanguage ?: "English"
                _uiState.update { it.copy(currentStep = pendingStep, selectedLanguage = language) }
                registerDevice(language)
                return
            }
        }
        if (prefs.getBoolean(KEY_ONBOARDING_DONE, false)) {
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
                // Advance immediately so a slow/missed recreate cannot trap the user on Language.
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
        _uiState.update { it.copy(currentStep = AppStep.Home) }
    }

    fun openHome() {
        _uiState.update { it.copy(currentStep = AppStep.Home, selectedShortFilmId = null) }
    }

    fun openShorts(filmId: Int? = null) {
        _uiState.update { it.copy(currentStep = AppStep.Shorts, selectedShortFilmId = filmId) }
    }

    fun openLibrary() {
        _uiState.update { it.copy(currentStep = AppStep.Library, selectedShortFilmId = null) }
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

    private fun loadRemoteConfig() {
        val config = remoteConfig ?: return
        applyRemoteValues(config)
        config.fetchAndActivate().addOnCompleteListener {
            applyRemoteValues(config)
        }
    }

    private fun applyRemoteValues(config: FirebaseRemoteConfig) {
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

private fun configureRemoteConfig(context: Context): FirebaseRemoteConfig? {
    return runCatching {
        FirebaseApp.initializeApp(context)
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build()
            )
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }.onFailure {
        Log.w(TAG, "Firebase Remote Config is using local defaults until Firebase config is added.", it)
    }.getOrNull()
}
