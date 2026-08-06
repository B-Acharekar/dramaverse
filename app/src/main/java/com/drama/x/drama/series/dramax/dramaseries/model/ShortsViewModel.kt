package com.drama.x.drama.series.dramax.dramaseries.model

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drama.x.drama.series.dramax.dramaseries.data.AuthRepository
import com.drama.x.drama.series.dramax.dramaseries.data.DramaItem
import com.drama.x.drama.series.dramax.dramaseries.data.EpisodeInfo
import com.drama.x.drama.series.dramax.dramaseries.data.LocaleHelper
import com.drama.x.drama.series.dramax.dramaseries.data.SavedWatchHistoryStore
import com.drama.x.drama.series.dramax.dramaseries.data.SavedWatchListStore
import com.drama.x.drama.series.dramax.dramaseries.data.ShortsItem
import com.drama.x.drama.series.dramax.dramaseries.data.ShortsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext

data class ShortsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val items: List<ShortsItem> = emptyList(),
    val episodesByFilm: Map<Int, List<EpisodeInfo>> = emptyMap(),
    val watchedEpisodesByFilm: Map<Int, Set<Int>> = emptyMap(),
    val switchingEpisodes: Map<Int, Int> = emptyMap(),
    val errorMessage: String? = null,
    val savedFilmIds: Set<Int> = emptySet(),
)

class ShortsViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = ShortsRepository(
        AuthRepository(application.applicationContext),
        application.applicationContext
    )
    private val savedWatchListStore = SavedWatchListStore(application.applicationContext)
    private val savedWatchHistoryStore = SavedWatchHistoryStore(application.applicationContext)
    private val _uiState = MutableStateFlow(ShortsUiState())
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()
    private var nextPage = 1
    private var currentBackendBaseUrl = ""
    private var currentInitialFilmId: Int? = null
    private var genericFeedOpenNonce = 0

    fun loadInitial(backendBaseUrl: String, initialFilmId: Int?, initialEpisodeNumber: Int? = null) {
        val isGenericFeed = initialFilmId == null || initialFilmId == 0

        // Generic feed mode: existing behavior
        if (
            _uiState.value.items.isNotEmpty() &&
            currentBackendBaseUrl == backendBaseUrl &&
            currentInitialFilmId == initialFilmId &&
            !isGenericFeed
        ) return
        currentBackendBaseUrl = backendBaseUrl
        currentInitialFilmId = initialFilmId
        // Generic Shorts should feel fresh on each open, while film-specific opens remain stable.
        genericFeedOpenNonce = if (isGenericFeed) nextGenericFeedOpenNonce() else 0
        nextPage = 1
        viewModelScope.launch {
            // Preserve existing savedFilmIds when reloading, don't reset from storage
            val existingSavedFilmIds = _uiState.value.savedFilmIds.ifEmpty { currentSavedFilmIds() }
            _uiState.update { ShortsUiState(isLoading = true, savedFilmIds = existingSavedFilmIds) }
            if (initialFilmId != null && initialFilmId != 0) {
                // Episode mode: if a specific episode number is requested, load it; otherwise resume from watch history
                val episodeToLoad = initialEpisodeNumber?.coerceAtLeast(1)
                    ?: savedWatchHistoryStore.getLastWatchedEpisode(initialFilmId)
                    ?: 1
                withContext(Dispatchers.IO) {
                    repository.loadPlayback(
                        backendBaseUrl = backendBaseUrl,
                        filmId = initialFilmId,
                        episodeNumber = episodeToLoad,
                        language = selectedLanguageCode()
                    )
                }.onSuccess { firstItem ->
                    if (firstItem.playUrl.isBlank()) return@onSuccess
                    val totalEpisodes = firstItem.film.episodeTotal.coerceAtLeast(1)
                    val initialItems = (1..totalEpisodes).map { epNum ->
                        if (epNum == episodeToLoad) {
                            firstItem
                        } else {
                            firstItem.copy(
                                episodeNumber = epNum,
                                playUrl = "",
                                isLocked = epNum > 7
                            )
                        }
                    }
                    _uiState.update {
                        it.copy(isLoading = false, items = initialItems, errorMessage = null)
                    }
                    // Now load ALL episodes of this drama to fill the feed sequentially
                    loadEpisodeFeed(backendBaseUrl, initialFilmId, firstItem, episodeToLoad)
                }
                ensurePlayback(episodeToLoad - 1, backendBaseUrl)
                return@launch  // Skip generic loadPage entirely in episode mode
            }
            loadPage(backendBaseUrl, initialFilmId, genericFeedOpenNonce)
            ensurePlayback(0, backendBaseUrl)
        }
    }

    /**
     * Loads all episodes of a drama and populates the feed sequentially:
     * ep1, ep2, ... epN (first FREE_SHORTS_PREVIEW_EPISODES free, rest locked).
     * Called only in episode mode (initialFilmId != null).
     */

    private fun currentSavedFilmIds(): Set<Int> {
        return savedWatchListStore.readItems()
            .map { it.id }
            .filter { it != 0 }
            .toSet()
    }

    private suspend fun loadEpisodeFeed(
        backendBaseUrl: String,
        filmId: Int,
        firstItem: ShortsItem,
        startingEpisodeNumber: Int = 1
    ) {
        val totalEpisodes = firstItem.film.episodeTotal.coerceAtLeast(1)
        if (totalEpisodes <= 1) return  // Only one episode, nothing to expand

        // Load remaining episodes (excluding the starting episode) in parallel batches of 3
        val episodeNumbers = (1..totalEpisodes).filterNot { it == startingEpisodeNumber }
        val episodeItems = mutableListOf<ShortsItem>()

        episodeNumbers.chunked(3).forEach { batch ->
            val batchResults = batch.map { epNum ->
                withContext(Dispatchers.IO) {
                    repository.loadPlayback(
                        backendBaseUrl = backendBaseUrl,
                        filmId = filmId,
                        episodeNumber = epNum,
                        language = selectedLanguageCode()
                    ).getOrNull()
                }
            }
            batchResults.filterNotNull().forEach { item ->
                episodeItems.add(item)
            }
            // Update UI incrementally after each batch so user sees episodes appear without re-ordering
            if (episodeItems.isNotEmpty()) {
                _uiState.update { state ->
                    val updatedItems = state.items.map { existing ->
                        episodeItems.firstOrNull { it.episodeNumber == existing.episodeNumber } ?: existing
                    }
                    state.copy(isLoading = false, items = updatedItems, errorMessage = null)
                }
            }
        }
    }



    fun loadMoreIfNeeded(currentIndex: Int, backendBaseUrl: String) {
        val state = _uiState.value
        if (state.isLoadingMore || state.items.isEmpty()) return
        // In episode mode we never load more generic content — the episode list is finite
        if (currentInitialFilmId != null && currentInitialFilmId != 0) return
        // Optimized: Prefetch earlier (at 60% threshold instead of 75%) for smoother scrolling
        if (currentIndex < state.items.lastIndex - 3) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            loadPage(backendBaseUrl, null, genericFeedOpenNonce)
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    /**
     * Returns true if we're in episode mode (viewing a specific drama's episodes only).
     * In this mode, the pager should not load additional dramas.
     */
    fun isInEpisodeMode(): Boolean = currentInitialFilmId != null && currentInitialFilmId != 0

    /**
     * Load a drama's episodes for episode mode when entering from generic feed (Watch Now).
     * Replaces the mixed feed with just this drama's episodes.
     */
    fun loadDramaEpisodesForWatchNow(
        backendBaseUrl: String,
        filmId: Int,
        startEpisode: Int = 1
    ) {
        val isGenericFeed = currentInitialFilmId == null || currentInitialFilmId == 0
        if (!isGenericFeed) return  // Already in episode mode

        currentInitialFilmId = filmId
        viewModelScope.launch {
            _uiState.update { ShortsUiState(isLoading = true) }
            
            withContext(Dispatchers.IO) {
                repository.loadPlayback(
                    backendBaseUrl = backendBaseUrl,
                    filmId = filmId,
                    episodeNumber = startEpisode,
                    language = selectedLanguageCode()
                )
            }.onSuccess { firstItem ->
                if (firstItem.playUrl.isBlank()) return@onSuccess
                val totalEpisodes = firstItem.film.episodeTotal.coerceAtLeast(1)
                val initialItems = (1..totalEpisodes).map { epNum ->
                    if (epNum == startEpisode) {
                        firstItem
                    } else {
                        firstItem.copy(
                            episodeNumber = epNum,
                            playUrl = "",
                            isLocked = epNum > 7
                        )
                    }
                }
                _uiState.update {
                    it.copy(isLoading = false, items = initialItems, errorMessage = null)
                }
                // Load remaining episodes in background
                loadEpisodeFeed(backendBaseUrl, filmId, firstItem, startEpisode)
            }
            ensurePlayback(startEpisode - 1, backendBaseUrl)
        }
    }

    fun ensurePlayback(index: Int, backendBaseUrl: String) {
        val item = _uiState.value.items.getOrNull(index) ?: return
        if (item.playUrl.isNotBlank() || item.film.id == 0) return
        viewModelScope.launch {
            val playback = loadPlaybackWithRetry(
                backendBaseUrl = backendBaseUrl,
                filmId = item.film.id,
                episodeNumber = item.episodeNumber,
                maxRetries = 3
            ) ?: return@launch
            _uiState.update { state ->
                state.copy(
                    items = state.items.mapIndexed { itemIndex, existing ->
                        if (itemIndex == index) playback.copy(film = playback.film.mergeFallback(existing.film)) else existing
                    }
                )
            }
        }
        
        // Preload next 2 videos to eliminate loading delays during scrolling
        preloadVideoUrl(index + 1, backendBaseUrl)
        preloadVideoUrl(index + 2, backendBaseUrl)
    }
    
    /**
     * Load playback with retry logic and exponential backoff
     */
    private suspend fun loadPlaybackWithRetry(
        backendBaseUrl: String,
        filmId: Int,
        episodeNumber: Int,
        maxRetries: Int = 4  // Increased from 3
    ): ShortsItem? {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.loadPlayback(
                        backendBaseUrl = backendBaseUrl,
                        filmId = filmId,
                        episodeNumber = episodeNumber,
                        language = selectedLanguageCode()
                    )
                }
                
                val playback = result.getOrNull()
                if (playback != null && playback.playUrl.isNotBlank()) {
                    // Validate URL is accessible before returning
                    if (isVideoUrlAccessible(playback.playUrl)) {
                        return playback
                    } else {
                        android.util.Log.w("ShortsViewModel", "Video URL not accessible: ${playback.playUrl}")
                    }
                }
                
                // If playUrl is blank, try backup URL if available
                lastException = result.exceptionOrNull() as? Exception 
                    ?: Exception("Empty or inaccessible playback URL received")
                    
            } catch (e: Exception) {
                lastException = e
                android.util.Log.w("ShortsViewModel", "Playback attempt ${attempt + 1} failed: ${e.message}")
            }
            
            // Progressive backoff: 500ms, 1s, 2s, 3s (faster initial retries)
            if (attempt < maxRetries - 1) {
                val backoffDelay = when (attempt) {
                    0 -> 500L
                    1 -> 1000L
                    2 -> 2000L
                    else -> 3000L
                }
                kotlinx.coroutines.delay(backoffDelay)
            }
        }
        
        android.util.Log.e("ShortsViewModel", "Failed to load playback after $maxRetries attempts", lastException)
        return null
    }
    
    private suspend fun isVideoUrlAccessible(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .head() // HEAD request to check accessibility without downloading
                    .build()
                
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                android.util.Log.w("ShortsViewModel", "URL accessibility check failed: ${e.message}")
                false
            }
        }
    }

    private fun preloadVideoUrl(index: Int, backendBaseUrl: String) {
        val item = _uiState.value.items.getOrNull(index) ?: return
        if (item.playUrl.isNotBlank() || item.film.id == 0) return
        viewModelScope.launch {
            val playback = withContext(Dispatchers.IO) {
                repository.loadPlayback(
                    backendBaseUrl = backendBaseUrl,
                    filmId = item.film.id,
                    episodeNumber = item.episodeNumber,
                    language = selectedLanguageCode()
                )
            }.getOrNull() ?: return@launch
            // Only update if the URL was empty (don't overwrite if already loaded)
            _uiState.update { state ->
                state.copy(
                    items = state.items.mapIndexed { itemIndex, existing ->
                        if (itemIndex == index && existing.playUrl.isBlank()) {
                            playback.copy(film = playback.film.mergeFallback(existing.film))
                        } else {
                            existing
                        }
                    }
                )
            }
        }
    }

    fun getLastWatchedEpisode(filmId: Int?): Int? {
        if (filmId == null || filmId == 0) return null
        return savedWatchHistoryStore.getLastWatchedEpisode(filmId)
    }

    fun loadEpisodeList(backendBaseUrl: String, filmId: Int) {
        if (filmId == 0 || _uiState.value.episodesByFilm.containsKey(filmId)) return
        viewModelScope.launch {
            val episodes = withContext(Dispatchers.IO) {
                repository.loadEpisodes(
                    backendBaseUrl = backendBaseUrl,
                    filmId = filmId,
                    language = selectedLanguageCode()
                )
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
                    liked = liked,
                    language = selectedLanguageCode()
                )
            }
        }
    }

    fun setReminder(
        backendBaseUrl: String,
        film: DramaItem,
        enabled: Boolean
    ) {
        val filmId = film.id
        if (filmId == 0) return
        // Update UI state immediately (optimistic) so the sidebar icon reflects
        // the change without waiting for the network call.
        _uiState.update { state ->
            state.copy(
                savedFilmIds = if (enabled) state.savedFilmIds + filmId else state.savedFilmIds - filmId
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.setReminder(
                    backendBaseUrl = backendBaseUrl,
                    filmId = filmId,
                    film = film,
                    enabled = enabled,
                    language = selectedLanguageCode()
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
                    episodeNumber = episodeNumber,
                    language = selectedLanguageCode()
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
        savedWatchHistoryStore.save(
            film = item.film,
            episodeNumber = item.episodeNumber,
            progressSeconds = progressSeconds,
            durationSeconds = durationSeconds,
            completed = true
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveWatchProgress(
                    backendBaseUrl = backendBaseUrl,
                    filmId = item.film.id,
                    episodeNumber = item.episodeNumber,
                    progressSeconds = progressSeconds,
                    durationSeconds = durationSeconds,
                    completed = true,
                    language = selectedLanguageCode()
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
                        episodeNumber = nextEpisode,
                        language = selectedLanguageCode()
                    )
                }
                refreshEpisodeList(backendBaseUrl, item.film.id)
            }

            val nextItem = withContext(Dispatchers.IO) {
                repository.loadPlayback(
                    backendBaseUrl = backendBaseUrl,
                    filmId = item.film.id,
                    episodeNumber = nextEpisode,
                    language = selectedLanguageCode()
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

    fun saveWatchProgress(
        backendBaseUrl: String,
        item: ShortsItem,
        progressSeconds: Int,
        durationSeconds: Int?
    ) {
        if (item.film.id == 0 || progressSeconds < 1) return
        savedWatchHistoryStore.save(
            film = item.film,
            episodeNumber = item.episodeNumber,
            progressSeconds = progressSeconds,
            durationSeconds = durationSeconds,
            completed = false
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveWatchProgress(
                    backendBaseUrl = backendBaseUrl,
                    filmId = item.film.id,
                    episodeNumber = item.episodeNumber,
                    progressSeconds = progressSeconds,
                    durationSeconds = durationSeconds,
                    completed = false,
                    language = selectedLanguageCode()
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
                    episodeNumber = episodeNumber,
                    language = selectedLanguageCode()
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
                    message = message.trim(),
                    language = selectedLanguageCode()
                )
            }
        }
    }

    private suspend fun loadPage(backendBaseUrl: String, initialFilmId: Int?, feedNonce: Int) {
        val page = nextPage
        val items = withContext(Dispatchers.IO) {
            repository.loadFilms(
                backendBaseUrl = backendBaseUrl,
                page = page,
                language = selectedLanguageCode()
            )
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
            items.rotated(feedNonce)
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
            repository.loadEpisodes(
                backendBaseUrl = backendBaseUrl,
                filmId = filmId,
                language = selectedLanguageCode()
            )
        }.getOrNull() ?: return
        _uiState.update { state ->
            val watched = state.watchedEpisodesByFilm[filmId].orEmpty()
            state.copy(episodesByFilm = state.episodesByFilm + (filmId to episodes.markWatched(watched)))
        }
    }

    private fun nextGenericFeedOpenNonce(): Int {
        val prefs = getApplication<Application>()
            .applicationContext
            .getSharedPreferences(SHORTS_PREFS_NAME, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_GENERIC_OPEN_COUNT, 0) + 1
        prefs.edit().putInt(KEY_GENERIC_OPEN_COUNT, next).apply()
        // Time bucket prevents reopening the app from pinning the same first video all day.
        val timeBucket = (System.currentTimeMillis() / 10_000L).toInt()
        return next + timeBucket
    }

    private fun selectedLanguageCode(): String = LocaleHelper.persistedLanguageCode(appContext)
}

private const val SHORTS_PREFS_NAME = "dramaverse_shorts"
private const val KEY_GENERIC_OPEN_COUNT = "generic_open_count"

private fun List<ShortsItem>.rotated(seed: Int): List<ShortsItem> {
    if (size <= 1) return this
    val offset = Math.floorMod(seed, size)
    return drop(offset) + take(offset)
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

private fun com.drama.x.drama.series.dramax.dramaseries.data.DramaItem.mergeFallback(
    fallback: com.drama.x.drama.series.dramax.dramaseries.data.DramaItem
): com.drama.x.drama.series.dramax.dramaseries.data.DramaItem {
    return copy(
        description = description.ifBlank { fallback.description },
        imageUrl = imageUrl.ifBlank { fallback.imageUrl },
        rating = rating.ifBlank { fallback.rating },
        genre = genre.ifBlank { fallback.genre },
        isPremium = isPremium || fallback.isPremium
    )
}
