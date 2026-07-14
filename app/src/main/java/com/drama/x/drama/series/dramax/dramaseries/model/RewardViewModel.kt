package com.drama.x.drama.series.dramax.dramaseries.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drama.x.drama.series.dramax.dramaseries.data.AuthRepository
import com.drama.x.drama.series.dramax.dramaseries.data.RewardFeed
import com.drama.x.drama.series.dramax.dramaseries.data.RewardRepository
import com.drama.x.drama.series.dramax.dramaseries.data.fallbackRewardFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class RewardUiState(
    val isLoading: Boolean = true,
    val feed: RewardFeed? = null,
    val errorMessage: String? = null,
    val showRules: Boolean = false,
    val spinPointerIndex: Int = 0,
    val spinTurns: Int = 0
)

class RewardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RewardRepository(
        application.applicationContext,
        AuthRepository(application.applicationContext)
    )
    private val _uiState = MutableStateFlow(RewardUiState())
    val uiState: StateFlow<RewardUiState> = _uiState.asStateFlow()

    fun loadRewards(backendBaseUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.loadRewards(backendBaseUrl)
                .onSuccess { feed -> _uiState.update { it.copy(isLoading = false, feed = feed) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            feed = it.feed ?: fallbackRewardFeed(),
                            errorMessage = error.message
                        )
                    }
                }
        }
    }

    fun setRulesVisible(visible: Boolean) {
        _uiState.update { it.copy(showRules = visible) }
    }

    fun claimDailyCheckIn(backendBaseUrl: String) {
        viewModelScope.launch {
            repository.claimAction(backendBaseUrl, "daily_check_in")
                .onSuccess { feed -> _uiState.update { it.copy(feed = feed, errorMessage = null) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun claimDailyTask(backendBaseUrl: String, taskId: String) {
        viewModelScope.launch {
            repository.claimAction(
                backendBaseUrl = backendBaseUrl,
                action = "daily_task",
                metadata = JSONObject().put("task_id", taskId)
            ).onSuccess { feed ->
                _uiState.update { it.copy(feed = feed, errorMessage = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun spin(backendBaseUrl: String) {
        viewModelScope.launch {
            repository.claimAction(backendBaseUrl, "weekly_spin")
                .onSuccess { feed ->
                    val segments = feed.spin.segments
                    val pointer = feed.spin.selectedIndex?.coerceIn(0, (segments.size - 1).coerceAtLeast(0)) ?: 0
                    _uiState.update {
                        it.copy(
                            feed = feed,
                            spinPointerIndex = pointer,
                            spinTurns = it.spinTurns + 1,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }
}
