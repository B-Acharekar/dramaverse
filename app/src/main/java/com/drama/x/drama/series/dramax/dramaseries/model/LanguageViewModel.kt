package com.drama.x.drama.series.dramax.dramaseries.model

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val PREFS_NAME = "dramaverse_onboarding"
private const val KEY_SELECTED_LANGUAGE = "selected_language"
private const val KEY_OPENED_LANGUAGE_BEFORE = "has_opened_language_before"

data class LanguageUiState(
    val languages: List<String> = listOf(
        "English",
        "Tiếng Việt",
        "Español",
        "Français",
        "Deutsch",
        "Italiano",
        "Português",
        "Türkçe",
        "العربية",
        "हिन्दी",
        "한국어",
        "日本語",
        "中文"
    ),
    val selectedLanguage: String? = null
)

class LanguageViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LanguageUiState())
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    val isFirstLanguageVisit: Boolean
        get() = !getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_OPENED_LANGUAGE_BEFORE, false)

    fun selectLanguage(language: String) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun confirmLanguage(): String {
        val chosenLanguage = _uiState.value.selectedLanguage ?: "English"
        getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_LANGUAGE, chosenLanguage)
            .putBoolean(KEY_OPENED_LANGUAGE_BEFORE, true)
            .apply()
        return chosenLanguage
    }
}
