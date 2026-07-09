package app.dramaverse.stream.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dramaverse.stream.data.AuthRepository
import app.dramaverse.stream.data.ShortsItem
import app.dramaverse.stream.data.ShortsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ShortsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val items: List<ShortsItem> = emptyList(),
    val errorMessage: String? = null
)

class ShortsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ShortsRepository(AuthRepository(application.applicationContext))
    private val _uiState = MutableStateFlow(ShortsUiState())
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()
    private var nextPage = 1
    private var currentBackendBaseUrl = ""
    private var currentInitialFilmId: Int? = null

    fun loadInitial(backendBaseUrl: String, initialFilmId: Int?) {
        if (
            _uiState.value.items.isNotEmpty() &&
            currentBackendBaseUrl == backendBaseUrl &&
            currentInitialFilmId == initialFilmId
        ) return
        currentBackendBaseUrl = backendBaseUrl
        currentInitialFilmId = initialFilmId
        nextPage = 1
        viewModelScope.launch {
            _uiState.update { ShortsUiState(isLoading = true) }
            if (initialFilmId != null && initialFilmId != 0) {
                withContext(Dispatchers.IO) {
                    repository.loadPlayback(backendBaseUrl, initialFilmId)
                }.onSuccess { selectedItem ->
                    if (selectedItem.playUrl.isBlank()) return@onSuccess
                    _uiState.update {
                        it.copy(isLoading = false, items = listOf(selectedItem), errorMessage = null)
                    }
                }
            }
            loadPage(backendBaseUrl, initialFilmId)
            ensurePlayback(0, backendBaseUrl)
        }
    }

    fun loadMoreIfNeeded(currentIndex: Int, backendBaseUrl: String) {
        val state = _uiState.value
        if (state.isLoadingMore || state.items.isEmpty()) return
        if (currentIndex < state.items.lastIndex - 2) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            loadPage(backendBaseUrl, null)
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    fun ensurePlayback(index: Int, backendBaseUrl: String) {
        val item = _uiState.value.items.getOrNull(index) ?: return
        if (item.playUrl.isNotBlank() || item.film.id == 0) return
        viewModelScope.launch {
            val playback = withContext(Dispatchers.IO) {
                repository.loadPlayback(backendBaseUrl, item.film.id)
            }.getOrNull() ?: return@launch
            _uiState.update { state ->
                state.copy(
                    items = state.items.mapIndexed { itemIndex, existing ->
                        if (itemIndex == index) playback.copy(film = playback.film.mergeFallback(existing.film)) else existing
                    }
                )
            }
        }
    }

    fun setEpisodeLike(
        backendBaseUrl: String,
        filmId: Int,
        episodeNumber: Int,
        liked: Boolean
    ) {
        if (filmId == 0) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.setEpisodeLike(
                    backendBaseUrl = backendBaseUrl,
                    filmId = filmId,
                    episodeNumber = episodeNumber,
                    liked = liked
                )
            }
        }
    }

    fun setReminder(
        backendBaseUrl: String,
        filmId: Int,
        enabled: Boolean
    ) {
        if (filmId == 0) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.setReminder(
                    backendBaseUrl = backendBaseUrl,
                    filmId = filmId,
                    enabled = enabled
                )
            }
        }
    }

    fun unlockEpisode(
        backendBaseUrl: String,
        filmId: Int,
        episodeNumber: Int
    ) {
        if (filmId == 0) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.unlockEpisode(
                    backendBaseUrl = backendBaseUrl,
                    filmId = filmId,
                    episodeNumber = episodeNumber
                )
            }
        }
    }

    fun completeEpisodeAndMaybePlayNext(
        backendBaseUrl: String,
        itemIndex: Int,
        item: ShortsItem,
        progressSeconds: Int,
        durationSeconds: Int?,
        autoNext: Boolean,
        autoUnlock: Boolean
    ) {
        if (item.film.id == 0) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveWatchProgress(
                    backendBaseUrl = backendBaseUrl,
                    filmId = item.film.id,
                    episodeNumber = item.episodeNumber,
                    progressSeconds = progressSeconds,
                    durationSeconds = durationSeconds,
                    completed = true
                )
            }
            if (!autoNext) return@launch

            val nextEpisode = item.episodeNumber + 1
            if (nextEpisode > item.film.episodeTotal) return@launch

            if (autoUnlock) {
                withContext(Dispatchers.IO) {
                    repository.unlockEpisode(
                        backendBaseUrl = backendBaseUrl,
                        filmId = item.film.id,
                        episodeNumber = nextEpisode
                    )
                }
            }

            val nextItem = withContext(Dispatchers.IO) {
                repository.loadPlayback(
                    backendBaseUrl = backendBaseUrl,
                    filmId = item.film.id,
                    episodeNumber = nextEpisode
                )
            }.getOrNull() ?: return@launch

            if (nextItem.isLocked || nextItem.playUrl.isBlank()) return@launch

            _uiState.update { state ->
                state.copy(
                    items = state.items.mapIndexed { index, existing ->
                        if (index == itemIndex) {
                            nextItem.copy(film = nextItem.film.mergeFallback(existing.film))
                        } else {
                            existing
                        }
                    }
                )
            }
        }
    }

    fun sendFeedback(
        backendBaseUrl: String,
        filmId: Int,
        episodeNumber: Int,
        message: String
    ) {
        if (filmId == 0 || message.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.sendFeedback(
                    backendBaseUrl = backendBaseUrl,
                    filmId = filmId,
                    episodeNumber = episodeNumber,
                    message = message.trim()
                )
            }
        }
    }

    private suspend fun loadPage(backendBaseUrl: String, initialFilmId: Int?) {
        val page = nextPage
        val items = withContext(Dispatchers.IO) {
            repository.loadFilms(backendBaseUrl, page = page)
        }.getOrElse { error ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = error.message ?: "Unable to load shorts."
                )
            }
            return
        }
        nextPage += 1
        val sortedItems = if (initialFilmId != null) {
            items.sortedBy { if (it.film.id == initialFilmId) 0 else 1 }
        } else {
            items
        }
        _uiState.update { state ->
            val merged = if (initialFilmId != null) {
                state.items + sortedItems.filterNot { it.film.id == initialFilmId }
            } else {
                state.items + sortedItems
            }
            val distinct = merged
                .distinctBy { it.film.id.takeIf { id -> id != 0 } ?: it.film.title }
            state.copy(isLoading = false, items = distinct, errorMessage = null)
        }
    }
}

private fun app.dramaverse.stream.data.DramaItem.mergeFallback(
    fallback: app.dramaverse.stream.data.DramaItem
): app.dramaverse.stream.data.DramaItem {
    return copy(
        description = description.ifBlank { fallback.description },
        imageUrl = imageUrl.ifBlank { fallback.imageUrl },
        rating = rating.ifBlank { fallback.rating },
        genre = genre.ifBlank { fallback.genre },
        isPremium = isPremium || fallback.isPremium
    )
}
