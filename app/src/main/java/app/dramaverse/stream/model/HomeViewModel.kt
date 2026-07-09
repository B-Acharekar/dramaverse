package app.dramaverse.stream.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dramaverse.stream.data.AuthRepository
import app.dramaverse.stream.data.HomeFeed
import app.dramaverse.stream.data.HomeRepository
import app.dramaverse.stream.data.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val feed: HomeFeed? = null,
    val errorMessage: String? = null,
    val selectedMood: String? = null,
    val isMoodLoading: Boolean = false,
    val savedFilmIds: Set<Int> = emptySet()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = HomeRepository(
        application.applicationContext,
        AuthRepository(application.applicationContext)
    )
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            HomeRepository.prefetchedFeed.collectLatest { feed ->
                if (feed != null) {
                    _uiState.update { it.copy(isLoading = false, feed = feed) }
                }
            }
        }
    }

    fun loadHome(backendBaseUrl: String) {
        val language = LocaleHelper.persistedLanguageCode(appContext)
        val hasFeed = _uiState.value.feed != null
        if (!hasFeed) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                repository.loadHome(
                    backendBaseUrl = backendBaseUrl,
                    language = language
                ).onSuccess { feed ->
                    if (feed != null) {
                        _uiState.update { it.copy(isLoading = false, feed = feed, errorMessage = null) }
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Unable to load home."
                        )
                    }
                }
            }
            viewModelScope.launch {
                repository.refreshHome(
                    backendBaseUrl = backendBaseUrl,
                    language = language
                ).onSuccess { feed ->
                    _uiState.update { it.copy(isLoading = false, feed = feed, errorMessage = null) }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = it.feed == null,
                            errorMessage = error.message ?: "Unable to load home."
                        )
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            repository.refreshContinueWatching(
                backendBaseUrl = backendBaseUrl,
                language = language
            ).onSuccess { continueWatching ->
                _uiState.update { state ->
                    val currentFeed = state.feed ?: return@update state
                    state.copy(feed = currentFeed.copy(continueWatching = continueWatching), errorMessage = null)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Unable to refresh progress."
                    )
                }
            }
        }
    }

    fun selectMood(backendBaseUrl: String, mood: String) {
        val language = LocaleHelper.persistedLanguageCode(appContext)
        viewModelScope.launch {
            _uiState.update { it.copy(selectedMood = mood, isMoodLoading = true, errorMessage = null) }
            repository.searchMood(
                backendBaseUrl = backendBaseUrl,
                mood = mood,
                language = language
            ).onSuccess { feed ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feed = feed,
                        selectedMood = mood,
                        isMoodLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isMoodLoading = false,
                        errorMessage = error.message ?: "Unable to search mood."
                    )
                }
            }
        }
    }

    fun search(backendBaseUrl: String, query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val language = LocaleHelper.persistedLanguageCode(appContext)
        viewModelScope.launch {
            _uiState.update { it.copy(selectedMood = trimmed, isMoodLoading = true, errorMessage = null) }
            repository.searchMood(
                backendBaseUrl = backendBaseUrl,
                mood = trimmed,
                language = language
            ).onSuccess { feed ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feed = feed,
                        selectedMood = trimmed,
                        isMoodLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isMoodLoading = false,
                        errorMessage = error.message ?: "Unable to search films."
                    )
                }
            }
        }
    }

    fun setReminder(backendBaseUrl: String, filmId: Int, enabled: Boolean = true) {
        if (filmId == 0) return
        val language = LocaleHelper.persistedLanguageCode(appContext)
        // Optimistic update keeps the hero bookmark responsive; rollback happens on API failure.
        _uiState.update { state ->
            state.copy(
                savedFilmIds = if (enabled) state.savedFilmIds + filmId else state.savedFilmIds - filmId,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            repository.setReminder(
                backendBaseUrl = backendBaseUrl,
                filmId = filmId,
                enabled = enabled,
                language = language
            ).onFailure { error ->
                _uiState.update {
                    it.copy(
                        savedFilmIds = if (enabled) it.savedFilmIds - filmId else it.savedFilmIds + filmId,
                        errorMessage = error.message ?: "Unable to update watch list."
                    )
                }
            }
        }
    }

    fun openHotSearch(backendBaseUrl: String) {
        val language = LocaleHelper.persistedLanguageCode(appContext)
        viewModelScope.launch {
            _uiState.update { it.copy(selectedMood = "hot", isMoodLoading = true, errorMessage = null) }
            repository.hotSearch(
                backendBaseUrl = backendBaseUrl,
                language = language
            ).onSuccess { feed ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feed = feed,
                        selectedMood = "hot",
                        isMoodLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isMoodLoading = false,
                        errorMessage = error.message ?: "Unable to load search."
                    )
                }
            }
        }
    }
}
