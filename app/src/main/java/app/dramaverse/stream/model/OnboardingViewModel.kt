package app.dramaverse.stream.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnboardingPage(
    val title: String,
    val accentTitle: String,
    val description: String,
    val visual: OnboardingVisual
)

enum class OnboardingVisual {
    DramaPhone,
    Collections,
    RomancePhone,
    Rewards
}

data class OnboardingUiState(
    val pages: List<OnboardingPage> = listOf(
        OnboardingPage(
            title = "Discover addictive",
            accentTitle = "short dramas",
            description = "Bite-sized stories that fit into your busy life. Experience premium storytelling anywhere.",
            visual = OnboardingVisual.DramaPhone
        ),
        OnboardingPage(
            title = "Explore Curated",
            accentTitle = "Collections",
            description = "Explore curated collections of dramas in various categories and genres.",
            visual = OnboardingVisual.Collections
        ),
        OnboardingPage(
            title = "Explore Curated",
            accentTitle = "Collections",
            description = "Explore the curated collections of dramas in various categories.",
            visual = OnboardingVisual.RomancePhone
        ),
        OnboardingPage(
            title = "Daily Rewards & Missions",
            accentTitle = "",
            description = "Earn free coins every day just by watching. Complete missions to unlock premium episodes.",
            visual = OnboardingVisual.Rewards
        )
    ),
    val selectedPage: Int = 0
) {
    val isLastPage: Boolean = selectedPage == pages.lastIndex
}

class OnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectPage(page: Int) {
        _uiState.update { it.copy(selectedPage = page.coerceIn(0, it.pages.lastIndex)) }
    }
}
