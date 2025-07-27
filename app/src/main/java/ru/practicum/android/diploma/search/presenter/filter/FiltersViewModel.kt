package ru.practicum.android.diploma.search.presenter.filter

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.practicum.android.diploma.search.domain.api.FiltersInteractor
import ru.practicum.android.diploma.search.domain.model.Industry

class FiltersViewModel(
    private val filtersInteractor: FiltersInteractor
) : ViewModel() {

    private val _selectedIndustry = MutableStateFlow<Industry?>(null)
    val selectedIndustry = _selectedIndustry.asStateFlow()

    private val _expectedSalary = MutableStateFlow("")
    val expectedSalary = _expectedSalary.asStateFlow()

    private val _noSalaryOnly = MutableStateFlow(false)
    val noSalaryOnly = _noSalaryOnly.asStateFlow()

    val hasActiveFilters = combine(
        _selectedIndustry,
        _expectedSalary,
        _noSalaryOnly
    ) { industry, salary, noSalary ->
        industry != null || salary.isNotBlank() || noSalary
    }

    init {
        loadSavedFilters()
    }

    fun updateIndustry(industry: Industry?) {
        _selectedIndustry.value = industry
    }

    fun updateSalary(salary: String) {
        _expectedSalary.value = salary
    }

    fun updateNoSalaryOnly(isChecked: Boolean) {
        _noSalaryOnly.value = isChecked
    }

    fun saveFilters() {
        viewModelScope.launch {
            filtersInteractor.saveFilters(
                industry = _selectedIndustry.value,
                salary = _expectedSalary.value,
                onlyWithSalary = _noSalaryOnly.value
            )
        }
    }

    fun clearFilters() {
        updateIndustry(null)
        updateSalary("")
        updateNoSalaryOnly(false)
    }

    private fun loadSavedFilters() {
        viewModelScope.launch {
            val (industry, salary, onlyWithSalary) = filtersInteractor.getSavedFilters()
            _selectedIndustry.value = industry
            _expectedSalary.value = salary ?: ""
            _noSalaryOnly.value = onlyWithSalary
        }
    }

    fun getFiltersBundle(): Bundle {
        return Bundle().apply {
            _selectedIndustry.value?.let { putString("industry", it.id) }
            if (_expectedSalary.value.isNotBlank()) {
                putString("salary", _expectedSalary.value)
            }
            if (_noSalaryOnly.value) {
                putBoolean("only_with_salary", true)
            }
        }
    }
}
