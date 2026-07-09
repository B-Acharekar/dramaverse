package app.dramaverse.stream.model

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import app.dramaverse.stream.R

data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val accentTitle: Int,
    @StringRes val description: Int,
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
            title = R.string.onboarding_title_1,
            accentTitle = R.string.onboarding_accent_title_1,
            description = R.string.onboarding_description_1,
            visual = OnboardingVisual.DramaPhone
        ),
        OnboardingPage(
            title = R.string.onboarding_title_2,
            accentTitle = R.string.onboarding_accent_title_2,
            description = R.string.onboarding_description_2,
            visual = OnboardingVisual.Collections
        ),
        OnboardingPage(
            title = R.string.onboarding_title_3,
            accentTitle = R.string.onboarding_accent_title_3,
            description = R.string.onboarding_description_3,
            visual = OnboardingVisual.RomancePhone
        ),
        OnboardingPage(
            title = R.string.onboarding_title_4,
            accentTitle = 0, // No accent title
            description = R.string.onboarding_description_4,
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
