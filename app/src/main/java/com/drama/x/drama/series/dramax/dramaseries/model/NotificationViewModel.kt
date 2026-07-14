package com.drama.x.drama.series.dramax.dramaseries.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drama.x.drama.series.dramax.dramaseries.data.AppNotification
import com.drama.x.drama.series.dramax.dramaseries.data.AuthRepository
import com.drama.x.drama.series.dramax.dramaseries.data.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val isLoading: Boolean = true,
    val isClearing: Boolean = false,
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

    fun clearAll(backendBaseUrl: String) {
        viewModelScope.launch {
            val previousItems = _uiState.value.items
            val previousUnread = _uiState.value.unreadCount
            _uiState.update { it.copy(isClearing = true, unreadCount = 0, items = emptyList(), errorMessage = null) }
            repository.clearNotifications(backendBaseUrl)
                .onSuccess {
                    _uiState.update { it.copy(isClearing = false, unreadCount = 0, items = emptyList()) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isClearing = false,
                            unreadCount = previousUnread,
                            items = previousItems,
                            errorMessage = error.message
                        )
                    }
                }
        }
    }
}
