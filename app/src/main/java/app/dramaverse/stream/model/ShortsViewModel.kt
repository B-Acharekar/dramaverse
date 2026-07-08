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

    fun loadInitial(backendBaseUrl: String, initialFilmId: Int?) {
        if (_uiState.value.items.isNotEmpty() && currentBackendBaseUrl == backendBaseUrl) return
        currentBackendBaseUrl = backendBaseUrl
        nextPage = 1
        viewModelScope.launch {
            _uiState.update { ShortsUiState(isLoading = true) }
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
            val merged = (state.items + sortedItems)
                .distinctBy { it.film.id.takeIf { id -> id != 0 } ?: it.film.title }
            state.copy(isLoading = false, items = merged, errorMessage = null)
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
