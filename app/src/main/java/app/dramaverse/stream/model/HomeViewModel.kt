package app.dramaverse.stream.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dramaverse.stream.data.AuthRepository
import app.dramaverse.stream.data.HomeFeed
import app.dramaverse.stream.data.HomeRepository
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
    val isMoodLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
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
                    _uiState.update { HomeUiState(isLoading = false, feed = feed) }
                }
            }
        }
    }

    fun loadHome(backendBaseUrl: String) {
        val hasFeed = _uiState.value.feed != null
        if (!hasFeed) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                repository.loadHome(
                    backendBaseUrl = backendBaseUrl,
                    language = "en"
                ).onSuccess { feed ->
                    if (feed != null) {
                        _uiState.update { HomeUiState(isLoading = false, feed = feed) }
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
                    language = "en"
                ).onSuccess { feed ->
                    _uiState.update { HomeUiState(isLoading = false, feed = feed) }
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
                language = "en"
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
        viewModelScope.launch {
            _uiState.update { it.copy(selectedMood = mood, isMoodLoading = true, errorMessage = null) }
            repository.searchMood(
                backendBaseUrl = backendBaseUrl,
                mood = mood,
                language = "en"
            ).onSuccess { feed ->
                _uiState.update {
                    HomeUiState(
                        isLoading = false,
                        feed = feed,
                        selectedMood = mood,
                        isMoodLoading = false
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

    fun openHotSearch(backendBaseUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedMood = "hot", isMoodLoading = true, errorMessage = null) }
            repository.hotSearch(
                backendBaseUrl = backendBaseUrl,
                language = "en"
            ).onSuccess { feed ->
                _uiState.update {
                    HomeUiState(
                        isLoading = false,
                        feed = feed,
                        selectedMood = "hot",
                        isMoodLoading = false
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
