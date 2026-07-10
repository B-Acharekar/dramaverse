package app.dramaverse.stream.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dramaverse.stream.data.AuthRepository
import app.dramaverse.stream.data.DramaItem
import app.dramaverse.stream.data.DramaNotificationScheduler
import app.dramaverse.stream.data.NotificationRepository
import app.dramaverse.stream.data.PlannerItem
import app.dramaverse.stream.data.PlannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId

data class PlannerUiState(
    val isLoading: Boolean = true,
    val items: List<PlannerItem> = emptyList(),
    val suggestions: List<DramaItem> = emptyList(),
    val selectedFilm: DramaItem? = null,
    val selectedEpisode: Int = 1,
    val errorMessage: String? = null
)

class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val authRepository = AuthRepository(application.applicationContext)
    private val repository = PlannerRepository(application.applicationContext, authRepository)
    private val notificationRepository = NotificationRepository(application.applicationContext, authRepository)
    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    fun loadPlanner(backendBaseUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val items = repository.loadPlanner(backendBaseUrl).getOrElse { emptyList() }
            val suggestions = repository.loadSuggestions(backendBaseUrl).getOrElse { emptyList() }
            _uiState.update {
                val selected = it.selectedFilm?.takeIf { current ->
                    suggestions.any { film -> film.id == current.id && film.title == current.title }
                } ?: suggestions.firstOrNull()
                it.copy(
                    isLoading = false,
                    items = items,
                    suggestions = suggestions,
                    selectedFilm = selected
                )
            }
        }
    }

    fun selectFilm(film: DramaItem) {
        _uiState.update { it.copy(selectedFilm = film, selectedEpisode = 1) }
    }

    fun changeEpisode(delta: Int) {
        _uiState.update { state ->
            val total = state.selectedFilm?.episodeTotal?.coerceAtLeast(1) ?: 1
            state.copy(selectedEpisode = (state.selectedEpisode + delta).coerceIn(1, total))
        }
    }

    fun scheduleTomorrow(backendBaseUrl: String) {
        val film = _uiState.value.selectedFilm ?: return
        val scheduledAt = OffsetDateTime.now(ZoneId.systemDefault()).plusDays(1).withHour(20).withMinute(0).withSecond(0).withNano(0)
        viewModelScope.launch {
            repository.savePlannerItem(
                backendBaseUrl = backendBaseUrl,
                film = film,
                episode = _uiState.value.selectedEpisode,
                scheduledAt = scheduledAt,
                remindBeforeMinutes = 15
            ).onSuccess { item ->
                DramaNotificationScheduler.schedulePlannerReminder(appContext, item)
                notificationRepository.trackNotification(
                    backendBaseUrl,
                    "Drama planned",
                    "${item.title} is scheduled for tomorrow.",
                    "planner"
                )
                _uiState.update { it.copy(items = (it.items + item).distinctBy(PlannerItem::id), errorMessage = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }
}
