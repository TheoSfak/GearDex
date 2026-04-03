package com.geardex.app.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.geardex.app.R
import com.geardex.app.data.local.entity.ExpenseCategory
import com.geardex.app.databinding.FragmentAddExpenseBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExpensesViewModel by viewModels()

    private var vehicles = emptyList<com.geardex.app.data.local.entity.Vehicle>()
    private var selectedVehicleIndex = 0
    private var selectedCategory: ExpenseCategory = ExpenseCategory.OTHER
    private var selectedDate: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val categories = ExpenseCategory.entries.toList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set default date to today
        binding.etExpenseDate.setText(dateFormat.format(Date(selectedDate)))

        // Category dropdown
        val categoryNames = categories.map { categoryName(requireContext(), it) }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.spinnerExpenseCategory.setAdapter(categoryAdapter)
        binding.spinnerExpenseCategory.setOnItemClickListener { _, _, pos, _ ->
            if (pos in categories.indices) selectedCategory = categories[pos]
        }
        // Default to OTHER
        binding.spinnerExpenseCategory.setText(categoryNames.last(), false)

        // Date picker
        binding.etExpenseDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.expense_date))
                .setSelection(selectedDate)
                .build()
            picker.addOnPositiveButtonClickListener { ms ->
                selectedDate = ms
                binding.etExpenseDate.setText(dateFormat.format(Date(ms)))
            }
            picker.show(parentFragmentManager, "expense_date_picker")
        }

        // Recurring toggle
        binding.switchRecurring.setOnCheckedChangeListener { _, isChecked ->
            binding.tilExpenseInterval.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Vehicle dropdown
        binding.spinnerExpenseVehicle.setOnItemClickListener { _, _, pos, _ ->
            selectedVehicleIndex = pos
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vehicles.collect { list ->
                    vehicles = list
                    val names = list.map { "${it.make} ${it.model}" }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                    binding.spinnerExpenseVehicle.setAdapter(adapter)
                    if (names.isNotEmpty()) {
                        binding.spinnerExpenseVehicle.setText(names[0], false)
                        selectedVehicleIndex = 0
                    }
                }
            }
        }

        binding.btnSaveExpense.setOnClickListener { validateAndSave() }
    }

    private fun validateAndSave() {
        val amount = binding.etExpenseAmount.text?.toString()?.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilExpenseAmount.error = getString(R.string.error_invalid_number)
            return
        }
        binding.tilExpenseAmount.error = null

        if (vehicles.isEmpty()) {
            Snackbar.make(binding.root, getString(R.string.logs_no_vehicle), Snackbar.LENGTH_SHORT).show()
            return
        }

        val vehicleId = vehicles[selectedVehicleIndex].id
        val description = binding.etExpenseDescription.text?.toString()?.trim() ?: ""
        val isRecurring = binding.switchRecurring.isChecked
        val interval = if (isRecurring)
            binding.etExpenseInterval.text?.toString()?.toIntOrNull()
        else null

        viewModel.addExpense(
            vehicleId = vehicleId,
            category = selectedCategory,
            amount = amount,
            date = selectedDate,
            description = description,
            isRecurring = isRecurring,
            intervalMonths = interval
        )
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
