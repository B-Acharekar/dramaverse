package app.dramaverse.stream.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dramaverse.stream.data.AuthRepository
import app.dramaverse.stream.data.RewardFeed
import app.dramaverse.stream.data.RewardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RewardUiState(
    val isLoading: Boolean = true,
    val feed: RewardFeed? = null,
    val errorMessage: String? = null,
    val selectedPlanIndex: Int = 1
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
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    fun selectPlan(index: Int) {
        _uiState.update { it.copy(selectedPlanIndex = index) }
    }

    fun claimDailyCheckIn(backendBaseUrl: String) {
        viewModelScope.launch {
            repository.claimAction(backendBaseUrl, "daily_check_in", 10)
                .onSuccess { feed -> _uiState.update { it.copy(feed = feed, errorMessage = null) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun spin(backendBaseUrl: String) {
        viewModelScope.launch {
            repository.claimAction(backendBaseUrl, "lucky_spin", 25)
                .onSuccess { feed -> _uiState.update { it.copy(feed = feed, errorMessage = null) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }
}
