package app.dramaverse.stream.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dramaverse.stream.data.AuthRepository
import app.dramaverse.stream.data.EpisodeInfo
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
    val episodesByFilm: Map<Int, List<EpisodeInfo>> = emptyMap(),
    val watchedEpisodesByFilm: Map<Int, Set<Int>> = emptyMap(),
    val switchingEpisodes: Map<Int, Int> = emptyMap(),
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

    fun loadEpisodeList(backendBaseUrl: String, filmId: Int) {
        if (filmId == 0 || _uiState.value.episodesByFilm.containsKey(filmId)) return
        viewModelScope.launch {
            val episodes = withContext(Dispatchers.IO) {
                repository.loadEpisodes(backendBaseUrl, filmId)
            }.getOrNull() ?: return@launch
            _uiState.update { state ->
                val watched = state.watchedEpisodesByFilm[filmId].orEmpty()
                state.copy(episodesByFilm = state.episodesByFilm + (filmId to episodes.markWatched(watched)))
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
            markEpisodeWatched(item.film.id, item.episodeNumber)
            if (!autoNext) return@launch

            val nextEpisode = item.episodeNumber + 1
            if (nextEpisode > item.film.episodeTotal) return@launch
            setEpisodeSwitching(itemIndex, nextEpisode)

            if (autoUnlock) {
                withContext(Dispatchers.IO) {
                    repository.unlockEpisode(
                        backendBaseUrl = backendBaseUrl,
                        filmId = item.film.id,
                        episodeNumber = nextEpisode
                    )
                }
                refreshEpisodeList(backendBaseUrl, item.film.id)
            }

            val nextItem = withContext(Dispatchers.IO) {
                repository.loadPlayback(
                    backendBaseUrl = backendBaseUrl,
                    filmId = item.film.id,
                    episodeNumber = nextEpisode
                )
            }.getOrNull()

            if (nextItem == null || nextItem.playUrl.isBlank()) {
                clearEpisodeSwitching(itemIndex)
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    switchingEpisodes = state.switchingEpisodes - itemIndex,
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

    private fun markEpisodeWatched(filmId: Int, episodeNumber: Int) {
        _uiState.update { state ->
            val watched = state.watchedEpisodesByFilm[filmId].orEmpty() + episodeNumber
            state.copy(
                watchedEpisodesByFilm = state.watchedEpisodesByFilm + (filmId to watched),
                episodesByFilm = state.episodesByFilm + (
                    filmId to state.episodesByFilm[filmId].orEmpty().markWatched(episodeNumber)
                )
            )
        }
    }

    fun playEpisode(
        backendBaseUrl: String,
        itemIndex: Int,
        currentItem: ShortsItem,
        episodeNumber: Int
    ) {
        if (currentItem.film.id == 0) return
        viewModelScope.launch {
            setEpisodeSwitching(itemIndex, episodeNumber)
            val selectedItem = withContext(Dispatchers.IO) {
                repository.loadPlayback(
                    backendBaseUrl = backendBaseUrl,
                    filmId = currentItem.film.id,
                    episodeNumber = episodeNumber
                )
            }.getOrNull()

            if (selectedItem == null || selectedItem.playUrl.isBlank()) {
                clearEpisodeSwitching(itemIndex)
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    switchingEpisodes = state.switchingEpisodes - itemIndex,
                    items = state.items.mapIndexed { index, existing ->
                        if (index == itemIndex) {
                            selectedItem.copy(film = selectedItem.film.mergeFallback(existing.film))
                        } else {
                            existing
                        }
                    }
                )
            }
        }
    }

    private fun setEpisodeSwitching(itemIndex: Int, episodeNumber: Int) {
        _uiState.update { state ->
            state.copy(switchingEpisodes = state.switchingEpisodes + (itemIndex to episodeNumber))
        }
    }

    private fun clearEpisodeSwitching(itemIndex: Int) {
        _uiState.update { state ->
            state.copy(switchingEpisodes = state.switchingEpisodes - itemIndex)
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

    private suspend fun refreshEpisodeList(backendBaseUrl: String, filmId: Int) {
        val episodes = withContext(Dispatchers.IO) {
            repository.loadEpisodes(backendBaseUrl, filmId)
        }.getOrNull() ?: return
        _uiState.update { state ->
            val watched = state.watchedEpisodesByFilm[filmId].orEmpty()
            state.copy(episodesByFilm = state.episodesByFilm + (filmId to episodes.markWatched(watched)))
        }
    }
}

private fun List<EpisodeInfo>.markWatched(episodeNumber: Int): List<EpisodeInfo> {
    if (isEmpty()) return this
    return map { episode ->
        if (episode.episodeNumber == episodeNumber) episode.copy(isWatched = true) else episode
    }
}

private fun List<EpisodeInfo>.markWatched(episodeNumbers: Set<Int>): List<EpisodeInfo> {
    if (isEmpty() || episodeNumbers.isEmpty()) return this
    return map { episode ->
        if (episode.episodeNumber in episodeNumbers) episode.copy(isWatched = true) else episode
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
