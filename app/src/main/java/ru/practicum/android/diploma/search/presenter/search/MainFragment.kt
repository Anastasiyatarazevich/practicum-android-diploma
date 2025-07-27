package ru.practicum.android.diploma.search.presenter.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.databinding.FragmentMainBinding
import ru.practicum.android.diploma.search.presenter.filter.FiltersViewModel
import ru.practicum.android.diploma.search.presenter.model.SearchState
import ru.practicum.android.diploma.search.presenter.model.VacancyPreviewUi
import ru.practicum.android.diploma.util.Debouncer
import ru.practicum.android.diploma.util.VacancyFormatter

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val searchViewModel: SearchViewModel by viewModel()
    private val filtersViewModel: FiltersViewModel by sharedViewModel()

    private val adapter by lazy {
        VacanciesAdapter(requireContext(), mutableListOf(), ::onVacancyClick)
    }
    private val debouncer by lazy {
        Debouncer(viewLifecycleOwner.lifecycleScope)
    }

    private var lastAppliedFilters: Map<String, String?> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeViewModels()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.vacanciesRvId.layoutManager = LinearLayoutManager(requireContext())
        binding.vacanciesRvId.adapter = adapter
    }

    private fun setupListeners() {
        binding.editTextId.doOnTextChanged { text, _, _, _ ->
            updateSearchIcon(text)
            if (text.isNullOrBlank()) {
                searchViewModel.clearSearch()
            } else {
                debouncer.searchDebounce {
                    searchViewModel.searchVacancies(text.toString(), lastAppliedFilters)
                }
            }
        }

        binding.searchIcon.setOnClickListener {
            if (binding.editTextId.text.isNotBlank()) {
                binding.editTextId.text.clear()
                hideKeyboard()
            }
        }

        binding.filterButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_filtersFragment)
        }

        binding.vacanciesRvId.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val pos = (binding.vacanciesRvId.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    val itemsCount = adapter.itemCount
                    if (pos >= itemsCount - 1) {
                        searchViewModel.loadNextPage()
                    }
                }
            }
        })

        setFragmentResultListener("filter_request") { _, bundle ->
            val industry = bundle.getString("industry")
            val salary = bundle.getString("salary")
            val onlyWithSalary = bundle.getBoolean("only_with_salary")

            val newFilters = mutableMapOf<String, String?>()
            if (industry != null) newFilters["industry"] = industry
            if (!salary.isNullOrBlank()) newFilters["salary"] = salary
            if (onlyWithSalary) newFilters["only_with_salary"] = "true"

            lastAppliedFilters = newFilters

            if (binding.editTextId.text.isNotBlank()) {
                searchViewModel.searchVacancies(binding.editTextId.text.toString(), lastAppliedFilters)
            }
        }
    }

    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchViewModel.state.collect { state ->
                    renderState(state)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                filtersViewModel.hasActiveFilters.collect { hasFilters ->
                    updateFilterIcon(hasFilters)
                }
            }
        }
    }

    private fun renderState(state: SearchState) {
        hideAll()
        when (state) {
            is SearchState.Loading -> binding.progressBarId.isVisible = true
            is SearchState.Empty -> binding.searchPreviewId.isVisible = true
            is SearchState.NoInternet -> binding.noInternetPreviewId.isVisible = true
            is SearchState.Error -> binding.notFoundPreview.isVisible = true // Можно заменить на свою заглушку
            is SearchState.NotFound -> showNotFound()
            is SearchState.Content -> showContent(state.data, state.found)
            is SearchState.LoadingMore -> {
                showContent(state.data, null)
                adapter.showLoading()
            }
            is SearchState.ContentWithLoadingError -> {
                showContent(state.data, null)
                showSnackbar(getString(R.string.searchvacancy_nointernet_error))
            }
        }
    }

    private fun showContent(data: List<VacancyPreviewUi>, found: Int?) {
        binding.vacanciesRvId.isVisible = true
        adapter.setList(data)
        if (found != null) {
            binding.infoShieldId.text = "Найдено ${VacancyFormatter.changeEnding(found)}"
            binding.infoShieldId.isVisible = true
        }
    }

    private fun showNotFound() {
        binding.notFoundPreview.isVisible = true
        binding.infoShieldId.text = getString(R.string.searchvacancy_notfound)
        binding.infoShieldId.isVisible = true
    }

    private fun onVacancyClick(vacancyId: Int) {
        if (debouncer.clickDebounce()) {
            findNavController().navigate(
                R.id.action_mainFragment_to_vacancyFragment,
                bundleOf("vacancyId" to vacancyId.toString())
            )
        }
    }

    private fun updateSearchIcon(text: CharSequence?) {
        val iconRes = if (text.isNullOrBlank()) R.drawable.search_24px else R.drawable.cross_light
        binding.searchIcon.setImageResource(iconRes)
    }

    private fun updateFilterIcon(hasFilters: Boolean) {
        val iconRes = if (hasFilters) R.drawable.filter_on else R.drawable.filter_off
        binding.filterButton.setImageResource(iconRes)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextId.windowToken, 0)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun hideAll() {
        binding.progressBarId.isVisible = false
        binding.searchPreviewId.isVisible = false
        binding.noInternetPreviewId.isVisible = false
        binding.notFoundPreview.isVisible = false
        binding.vacanciesRvId.isVisible = false
        binding.infoShieldId.isVisible = false
        adapter.hideLoading()
    }
}
