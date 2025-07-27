package ru.practicum.android.diploma.search.presenter.filter.ui.fields

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.databinding.FragmentFieldsBinding
import ru.practicum.android.diploma.search.domain.model.Industry
import ru.practicum.android.diploma.search.presenter.filter.FiltersViewModel
import ru.practicum.android.diploma.search.presenter.filter.model.FieldsState
import ru.practicum.android.diploma.search.presenter.filter.ui.fields.viewmodel.FieldsViewModel
import ru.practicum.android.diploma.util.Debouncer

class FieldsFragment : Fragment() {

    private var _binding: FragmentFieldsBinding? = null
    private val binding get() = _binding!!

    private val fieldsViewModel: FieldsViewModel by viewModel()
    private val filtersViewModel: FiltersViewModel by sharedViewModel()

    private val adapter by lazy {
        IndustriesAdapter { industry ->
            fieldsViewModel.onIndustrySelected(industry)
        }
    }
    private val debouncer by lazy {
        Debouncer(viewLifecycleOwner.lifecycleScope)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fieldsRecyclerView.adapter = adapter
        fieldsViewModel.init(filtersViewModel.selectedIndustry.value)
        setupListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        debouncer.cancelDebounce()
        _binding = null
    }

    private fun setupListeners() {
        binding.backButtonId.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.selectButton.setOnClickListener {
            val currentState = fieldsViewModel.state.value
            if (currentState is FieldsState.Content) {
                filtersViewModel.updateIndustry(currentState.selectedIndustry)
            }
            findNavController().popBackStack()
        }

        binding.fieldEdittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSearchIcon(s)
                debouncer.searchDebounce {
                    fieldsViewModel.filter(s.toString())
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.searchIcon.setOnClickListener {
            if (binding.fieldEdittext.text.isNotBlank()) {
                binding.fieldEdittext.text.clear()
            }
        }
    }

    private fun updateSearchIcon(text: CharSequence?) {
        val iconRes = if (text.isNullOrBlank()) R.drawable.search_24px else R.drawable.cross_light
        binding.searchIcon.setImageResource(iconRes)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            fieldsViewModel.state.collect { state ->
                renderState(state)
            }
        }
    }

    private fun renderState(state: FieldsState) {
        binding.progressBar.isVisible = state is FieldsState.Loading
        binding.fieldsRecyclerView.isVisible = state is FieldsState.Content
        binding.placeholderEmpty.isVisible = state is FieldsState.Empty
        binding.placeholderError.isVisible = state is FieldsState.Error
        binding.selectButton.isVisible = state is FieldsState.Content && state.selectedIndustry != null

        if (state is FieldsState.Content) {
            adapter.submitList(state.industries)
            adapter.updateSelection(state.selectedIndustry)
        }
    }
}
