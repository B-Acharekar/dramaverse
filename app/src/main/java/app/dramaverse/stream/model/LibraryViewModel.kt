package app.dramaverse.stream.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dramaverse.stream.data.AuthRepository
import app.dramaverse.stream.data.LibraryFeed
import app.dramaverse.stream.data.LibraryRepository
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
    private val repository = LibraryRepository(
        context = application.applicationContext,
        authRepository = AuthRepository(application.applicationContext)
    )
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun loadLibrary(backendBaseUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.feed == null, errorMessage = null) }
            repository.loadLibrary(backendBaseUrl = backendBaseUrl)
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
}
