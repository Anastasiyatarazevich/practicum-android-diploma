package ru.practicum.android.diploma.search.presenter.filter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.databinding.FragmentFiltersBinding
import ru.practicum.android.diploma.search.domain.model.Industry

class FiltersFragment : Fragment() {
    private var _binding: FragmentFiltersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FiltersViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupListeners()
        updateOnlyWithSalaryAndSalary()
        setupSalaryInputClearButton()
        showCrossIc()
    }

    private fun observeViewModel() {
        viewModel.selectedIndustry.onEach { industry ->
            updateIndustryField(industry)
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.expectedSalary.onEach { salary ->
            if (binding.editTextId.text.toString() != salary) {
                binding.editTextId.setText(salary)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.noSalaryOnly.onEach { isChecked ->
            if (binding.noSalaryCheckbox.isChecked != isChecked) {
                binding.noSalaryCheckbox.isChecked = isChecked
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.hasActiveFilters.onEach { hasFilters ->
            binding.buttonsContainer.visibility = if (hasFilters) View.VISIBLE else View.GONE
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupListeners() {
        binding.backButtonId.setOnClickListener {
            setFragmentResult(
                getString(R.string.filter_request),
                bundleOf(getString(R.string.filters_applied) to false)
            )
            findNavController().popBackStack()
        }

        binding.applyButton.setOnClickListener {
            val bundle = bundleOf(
                "industry" to viewModel.selectedIndustry.value?.id,
                "salary" to viewModel.expectedSalary.value,
                "only_with_salary" to viewModel.noSalaryOnly.value,
                "filters_applied" to true
            )
            Log.d("FiltersFragment", "Отправка результата: $bundle")
            viewModel.saveFilters()
            setFragmentResult("filter_request", bundle)
            findNavController().popBackStack()
        }

        binding.resetButton.setOnClickListener {
            viewModel.clearFilters()
            setFragmentResult(
                getString(R.string.filter_request),
                bundleOf(getString(R.string.filters_applied) to false)
            )
        }

        binding.fieldId.setOnClickListener {
            findNavController().navigate(R.id.action_filtersFragment_to_fieldsFragment)
        }
    }

    private fun updateOnlyWithSalaryAndSalary() {
        binding.noSalaryCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateNoSalaryOnly(isChecked)
            viewModel.saveFilters()
        }
    }

    private fun updateIndustryField(industry: Industry?) {
        if (industry != null) {
            binding.filterField.text = industry.name
            binding.filterField.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } else {
            binding.filterField.text = getString(R.string.filter_field)
            binding.filterField.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSalaryInputClearButton() {
        binding.editTextId.doAfterTextChanged { text ->
            toggleClearButtonVisibility(text.toString())
            if (text.isNullOrEmpty()) {
                viewModel.updateSalary(null)
            } else {
                viewModel.updateSalary(text.toString())
            }
            viewModel.saveFilters()
        }

        binding.searchIcon.setOnClickListener {
            binding.editTextId.text?.clear()
            viewModel.updateSalary(null)
            viewModel.saveFilters()
        }
    }

    private fun toggleClearButtonVisibility(text: String?) {
        binding.searchIcon.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    private fun showCrossIc() {
        if (!binding.editTextId.text.toString().isNullOrEmpty()) {
            binding.searchIcon.visibility = View.VISIBLE
        }
    }
}
