package ru.practicum.android.diploma.search.presenter.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.practicum.android.diploma.search.domain.api.SearchInteractor
import ru.practicum.android.diploma.search.domain.model.FailureType
import ru.practicum.android.diploma.search.domain.model.VacancyPreview
import ru.practicum.android.diploma.search.presenter.model.SearchState
import ru.practicum.android.diploma.search.presenter.model.VacancyPreviewUi
import ru.practicum.android.diploma.util.VacancyFormatter

class SearchViewModel(
    private val searchInteractor: SearchInteractor
) : ViewModel() {

    private val _state = MutableStateFlow<SearchState>(SearchState.Empty)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var currentPage = 0
    private var maxPages = 1
    private var currentQuery = ""
    private var currentFilters: Map<String, String?> = emptyMap()
    private var vacanciesList = mutableListOf<VacancyPreviewUi>()
    private var isLoading = false

    fun searchVacancies(text: String, filters: Map<String, String?> = emptyMap()) {
        if (text == currentQuery && filters == currentFilters || text.isBlank()) {
            return
        }
        currentQuery = text
        currentFilters = filters
        currentPage = 0
        maxPages = 1
        vacanciesList.clear()
        loadPage()
    }

    fun loadNextPage() {
        if (isLoading || currentPage >= maxPages - 1) return
        currentPage++
        loadPage()
    }

    fun clearSearch() {
        currentQuery = ""
        vacanciesList.clear()
        _state.value = SearchState.Empty
    }

    private fun loadPage() {
        if (isLoading) return
        isLoading = true

        if (currentPage == 0) {
            _state.value = SearchState.Loading
        } else {
            _state.value = SearchState.LoadingMore(vacanciesList)
        }

        viewModelScope.launch {
            searchInteractor.getVacancies(currentQuery, currentPage, currentFilters)
                .collect { pair ->
                    val newData = pair.first
                    val errorType = pair.second

                    when {
                        errorType != null -> {
                            handleError(errorType)
                        }
                        !newData.isNullOrEmpty() -> {
                            maxPages = newData.first().pages
                            val uiData = newData.map { it.toUiModel() }
                            if (currentPage == 0) {
                                vacanciesList.clear()
                            }
                            vacanciesList.addAll(uiData)
                            _state.value = SearchState.Content(vacanciesList.toList(), newData.first().found)
                        }
                        else -> {
                            if (vacanciesList.isEmpty()) {
                                _state.value = SearchState.NotFound
                            }
                        }
                    }
                    isLoading = false
                }
        }
    }

    private fun handleError(errorType: FailureType) {
        if (currentPage > 0) {
            _state.value = SearchState.ContentWithLoadingError(vacanciesList.toList())
        } else {
            when (errorType) {
                is FailureType.NoInternet -> _state.value = SearchState.NoInternet
                is FailureType.ApiError -> _state.value = SearchState.Error
                is FailureType.NotFound -> _state.value = SearchState.NotFound
            }
        }
    }

    private fun VacancyPreview.toUiModel(): VacancyPreviewUi {
        return VacancyPreviewUi(
            id = id,
            found = found,
            name = name,
            employerName = employerName,
            salary = VacancyFormatter.formatSalary(from, to, currency),
            logoUrl = url
        )
    }
}
