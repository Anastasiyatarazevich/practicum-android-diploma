package ru.practicum.android.diploma.search.presenter.model

sealed interface SearchState {
    data class Content(val data: List<VacancyPreviewUi>, val found: Int) : SearchState
    data class LoadingMore(val data: List<VacancyPreviewUi>) : SearchState
    data class ContentWithLoadingError(val data: List<VacancyPreviewUi>) : SearchState
    data object NotFound : SearchState
    data object Loading : SearchState
    data object Error : SearchState
    data object NoInternet : SearchState
    data object Empty : SearchState
}
