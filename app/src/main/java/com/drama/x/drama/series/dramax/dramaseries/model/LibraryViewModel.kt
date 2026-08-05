package com.drama.x.drama.series.dramax.dramaseries.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drama.x.drama.series.dramax.dramaseries.data.AuthRepository
import com.drama.x.drama.series.dramax.dramaseries.data.LibraryFeed
import com.drama.x.drama.series.dramax.dramaseries.data.LibraryRepository
import com.drama.x.drama.series.dramax.dramaseries.data.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = true,
    val feed: LibraryFeed? = null,
    val errorMessage: String? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = LibraryRepository(
        context = application.applicationContext,
        authRepository = AuthRepository(application.applicationContext)
    )
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun loadLibrary(backendBaseUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.feed == null, errorMessage = null) }
            repository.loadCachedLibrary()?.let { cached ->
                _uiState.update { it.copy(isLoading = false, feed = cached, errorMessage = null) }
            }
            repository.loadLibrary(
                backendBaseUrl = backendBaseUrl,
                language = LocaleHelper.persistedLanguageCode(appContext)
            )
                .onSuccess { feed ->
                    _uiState.update { LibraryUiState(isLoading = false, feed = feed) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load library."
                        )
                    }
                }
        }
    }

    fun clearHistory(backendBaseUrl: String) {
        viewModelScope.launch {
            repository.clearHistory(backendBaseUrl)
                .onSuccess {
                    // Update UI immediately with cleared history
                    _uiState.update { state ->
                        state.feed?.let { feed ->
                            state.copy(feed = feed.copy(watchHistory = emptyList()))
                        } ?: state
                    }
                }
        }
    }

    /** Removes only the specified history items from the local UI state immediately. */
    fun removeHistoryItems(
        backendBaseUrl: String,
        itemFilmIds: Set<Int>
    ) {
        viewModelScope.launch {
            val currentFeed = _uiState.value.feed ?: return@launch
            val remainingHistory = currentFeed.watchHistory.filter { it.film.id !in itemFilmIds }
            val updatedFeed = currentFeed.copy(watchHistory = remainingHistory)
            _uiState.update { it.copy(feed = updatedFeed) }
            // If no history left, clear local storage too
            if (remainingHistory.isEmpty()) {
                repository.clearHistory(backendBaseUrl)
            }
        }
    }

    /** Removes only the specified favorite items from the local UI state immediately. */
    fun removeFavoriteItems(
        backendBaseUrl: String,
        itemFilmIds: Set<Int>
    ) {
        viewModelScope.launch {
            val currentFeed = _uiState.value.feed ?: return@launch
            val removedFilms = currentFeed.watchList.filter { it.id in itemFilmIds }
            val remainingFavorites = currentFeed.watchList.filter { it.id !in itemFilmIds }
            val updatedFeed = currentFeed.copy(watchList = remainingFavorites)
            _uiState.update { it.copy(feed = updatedFeed) }
            // Toggle watchlist off for each removed film (local-only now)
            removedFilms.forEach { film ->
                repository.toggleWatchList(backendBaseUrl, film, false)
            }
        }
    }

    fun toggleWatchList(backendBaseUrl: String, film: com.drama.x.drama.series.dramax.dramaseries.data.DramaItem, enable: Boolean) {
        viewModelScope.launch {
            // Update UI immediately
            _uiState.update { state ->
                state.feed?.let { feed ->
                    val updatedWatchList = if (enable) {
                        (listOf(film) + feed.watchList).distinctBy { it.id }
                    } else {
                        feed.watchList.filterNot { it.id == film.id }
                    }
                    state.copy(feed = feed.copy(watchList = updatedWatchList))
                } ?: state
            }
            // Save locally (no backend call)
            repository.toggleWatchList(backendBaseUrl, film, enable)
        }
    }
}
