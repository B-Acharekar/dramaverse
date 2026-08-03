package com.drama.x.drama.series.dramax.dramaseries.model

import android.app.Application
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

data class EpisodeUiState(
    val isLoading: Boolean = true,
    val filmId: Int? = null,
    val filmTitle: String = "",
    val episodes: List<ShortsItem> = emptyList(),
    val unlockedEpisodes: Set<Int> = emptySet(),
    val dailyUnlocksUsed: Int = 0,
    val dailyUnlockLimit: Int = 7,
    val currentEpisodeIndex: Int = 0,
    val errorMessage: String? = null
)

class EpisodeViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = ShortsRepository(AuthRepository(application.applicationContext))
    private val savedWatchListStore = SavedWatchListStore(application.applicationContext)
    private val savedWatchHistoryStore = SavedWatchHistoryStore(application.applicationContext)
    
    private val _uiState = MutableStateFlow(EpisodeUiState())
    val uiState: StateFlow<EpisodeUiState> = _uiState.asStateFlow()
    
    private var currentBackendBaseUrl = ""

    fun loadEpisodes(backendBaseUrl: String, filmId: Int?) {
        if (filmId == null || filmId == 0) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "No film selected (filmId: $filmId)") }
            return
        }
        if (currentBackendBaseUrl == backendBaseUrl && _uiState.value.filmId == filmId && _uiState.value.episodes.isNotEmpty()) {
            return  // Already loaded
        }
        
        currentBackendBaseUrl = backendBaseUrl
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Load episode 1 first so the screen becomes interactive immediately (< 1s goal).
            val result = withContext(Dispatchers.IO) {
                repository.loadPlayback(
                    backendBaseUrl = backendBaseUrl,
                    filmId = filmId,
                    language = selectedLanguageCode(),
                    episodeNumber = 1
                )
            }
            
            val firstEpisode = result.getOrNull()
            
            if (firstEpisode == null) {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to load series"
                android.util.Log.e("EpisodeViewModel", "Failed to load episode 1 for filmId=$filmId: $errorMsg")
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load series: $errorMsg") }
                return@launch
            }
            
            val totalEpisodes = firstEpisode.film.episodeTotal.coerceAtLeast(1)
            
            // Show first episode right away — screen is ready in < 1s.
            _uiState.update {
                it.copy(
                    isLoading = false,
                    filmId = filmId,
                    filmTitle = firstEpisode.film.title,
                    // Pre-populate list with placeholder items so the pager knows total count.
                    episodes = listOf(firstEpisode) + (2..totalEpisodes).map { epNum ->
                        firstEpisode.copy(
                            episodeNumber = epNum,
                            playUrl = ""  // will be filled lazily
                        )
                    },
                    errorMessage = null
                )
            }
            
            // Load remaining episodes lazily in the background — no blocking wait.
            for (epNum in 2..totalEpisodes) {
                val playback = withContext(Dispatchers.IO) {
                    repository.loadPlayback(
                        backendBaseUrl = backendBaseUrl,
                        filmId = filmId,
                        language = selectedLanguageCode(),
                        episodeNumber = epNum
                    )
                }.getOrNull() ?: continue
                
                _uiState.update { state ->
                    state.copy(
                        episodes = state.episodes.mapIndexed { idx, existing ->
                            if (existing.episodeNumber == epNum) playback else existing
                        }
                    )
                }
            }
        }
    }
    
    fun ensurePlayback(index: Int, backendBaseUrl: String) {
        val item = _uiState.value.episodes.getOrNull(index) ?: return
        if (item.playUrl.isNotBlank() || item.film.id == 0) return
        
        viewModelScope.launch {
            val playback = withContext(Dispatchers.IO) {
                repository.loadPlayback(
                    backendBaseUrl = backendBaseUrl,
                    filmId = item.film.id,
                    language = selectedLanguageCode(),
                    episodeNumber = item.episodeNumber
                )
            }.getOrNull() ?: return@launch
            
            _uiState.update { state ->
                state.copy(
                    episodes = state.episodes.mapIndexed { itemIndex, existing ->
                        if (itemIndex == index) playback else existing
                    }
                )
            }
        }
        
        // Preload next episode
        preloadEpisode(index + 1, backendBaseUrl)
    }
    
    private fun preloadEpisode(index: Int, backendBaseUrl: String) {
        val item = _uiState.value.episodes.getOrNull(index) ?: return
        if (item.playUrl.isNotBlank() || item.film.id == 0) return
        
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.loadPlayback(
                    backendBaseUrl = backendBaseUrl,
                    filmId = item.film.id,
                    language = selectedLanguageCode(),
                    episodeNumber = item.episodeNumber
                )
            }
        }
    }
    
    fun unlockEpisode(backendBaseUrl: String, filmId: Int, episodeNumber: Int) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    unlockedEpisodes = state.unlockedEpisodes + episodeNumber,
                    dailyUnlocksUsed = state.dailyUnlocksUsed + 1
                )
            }
            
            // Save unlock to backend
            withContext(Dispatchers.IO) {
                repository.unlockEpisode(
                    backendBaseUrl = backendBaseUrl,
                    filmId = filmId,
                    episodeNumber = episodeNumber
                )
            }
        }
    }
    
    fun saveWatchProgress(
        backendBaseUrl: String,
        filmId: Int,
        episodeNumber: Int,
        progressSeconds: Int,
        durationSeconds: Int?
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveWatchProgress(
                    backendBaseUrl = backendBaseUrl,
                    filmId = filmId,
                    episodeNumber = episodeNumber,
                    progressSeconds = progressSeconds,
                    durationSeconds = durationSeconds,
                    completed = false
                )
            }
        }
    }
    
    fun isEpisodeLocked(episodeNumber: Int): Boolean {
        // First FREE_EPISODES episodes are always unlocked without spending a daily token.
        if (episodeNumber <= FREE_EPISODES) return false
        return episodeNumber !in _uiState.value.unlockedEpisodes
    }
    
    fun canUnlockMore(): Boolean {
        return _uiState.value.dailyUnlocksUsed < _uiState.value.dailyUnlockLimit
    }
    
    private fun selectedLanguageCode(): String {
        return LocaleHelper.persistedLanguageCode(appContext)
    }
}

private const val FREE_EPISODES = 7  // Episodes 1–7 are always free; 8+ require a daily ad unlock