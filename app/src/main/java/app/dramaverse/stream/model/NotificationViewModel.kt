package app.dramaverse.stream.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dramaverse.stream.data.AppNotification
import app.dramaverse.stream.data.AuthRepository
import app.dramaverse.stream.data.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val isLoading: Boolean = true,
    val unreadCount: Int = 0,
    val items: List<AppNotification> = emptyList(),
    val errorMessage: String? = null
)

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotificationRepository(
        application.applicationContext,
        AuthRepository(application.applicationContext)
    )
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    fun loadNotifications(backendBaseUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadNotifications(backendBaseUrl)
                .onSuccess { feed ->
                    _uiState.update {
                        it.copy(isLoading = false, unreadCount = feed.unreadCount, items = feed.items)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
