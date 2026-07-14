package com.drama.x.drama.series.dramax.dramaseries.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drama.x.drama.series.dramax.dramaseries.data.DramaItem
import com.drama.x.drama.series.dramax.dramaseries.data.LocaleHelper
import com.drama.x.drama.series.dramax.dramaseries.data.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val results: List<DramaItem> = emptyList(),
    val errorMessage: String? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = SearchRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun search(backendBaseUrl: String, query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        if (_uiState.value.query == trimmed && _uiState.value.results.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, query = trimmed, errorMessage = null) }
            repository.searchFilms(
                backendBaseUrl = backendBaseUrl,
                query = trimmed,
                language = LocaleHelper.persistedLanguageCode(appContext)
            )
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(isLoading = false, query = trimmed, results = results, errorMessage = null)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, query = trimmed, errorMessage = error.message ?: "Unable to search.")
                    }
                }
        }
    }
}
