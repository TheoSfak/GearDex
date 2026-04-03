package com.geardex.app.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.animation.AnimationUtils
import com.geardex.app.R
import com.geardex.app.databinding.FragmentExpensesBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExpensesViewModel by viewModels()
    private var expenseAdapter: ExpenseAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        expenseAdapter = ExpenseAdapter(
            onDelete = { expense ->
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirm_delete))
                    .setPositiveButton(getString(R.string.delete)) { _, _ -> viewModel.deleteExpense(expense) }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        )
        binding.recyclerExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExpenses.adapter = expenseAdapter
        binding.recyclerExpenses.layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_slide_up)

        binding.btnSetBudget.setOnClickListener { showBudgetDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.vehicles.collect { vehicles ->
                        if (vehicles.isNotEmpty() && viewModel.selectedVehicleId.value < 0) {
                            viewModel.selectVehicle(vehicles[0].id)
                        }
                    }
                }
                launch {
                    viewModel.expenses.collect { expenses ->
                        expenseAdapter?.submitList(expenses)
                        binding.layoutExpensesEmpty.visibility =
                            if (expenses.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerExpenses.visibility =
                            if (expenses.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.summary.collect { summary ->
                        binding.tvMonthTotal.text = "€${"%.2f".format(summary.monthTotal)}"

                        if (summary.budget != null) {
                            val remaining = summary.budgetRemaining ?: 0.0
                            binding.tvBudgetRemaining.text = "€${"%.2f".format(remaining)}"
                            binding.tvBudgetLabel.text = if (remaining >= 0)
                                getString(R.string.expenses_budget_remaining)
                            else
                                getString(R.string.expenses_budget_exceeded)
                            binding.tvBudgetRemaining.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    if (remaining >= 0) R.color.color_success else R.color.color_error
                                )
                            )
                            binding.progressBudget.progress = summary.budgetPercent.coerceIn(0, 100)
                            binding.progressBudget.visibility = View.VISIBLE
                        } else {
                            binding.tvBudgetRemaining.text = "—"
                            binding.tvBudgetLabel.text = getString(R.string.expenses_no_budget)
                            binding.progressBudget.visibility = View.GONE
                        }

                        // Category breakdown chips
                        binding.chipGroupCategories.removeAllViews()
                        summary.categoryBreakdown
                            .sortedByDescending { it.total }
                            .forEach { catTotal ->
                                val chip = Chip(requireContext()).apply {
                                    text = "${categoryName(requireContext(), catTotal.category)}: €${"%.0f".format(catTotal.total)}"
                                    isClickable = false
                                    setChipBackgroundColorResource(R.color.accent_expense_bg)
                                    setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_expense))
                                    textSize = 12f
                                }
                                binding.chipGroupCategories.addView(chip)
                            }
                    }
                }
            }
        }
    }

    private fun showBudgetDialog() {
        val vehicleId = viewModel.selectedVehicleId.value
        if (vehicleId < 0) return
        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = getString(R.string.expenses_budget_hint)
            val current = viewModel.getBudget(vehicleId)
            if (current != null) setText("%.0f".format(current))
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.expenses_set_budget))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val amount = input.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    viewModel.setBudget(vehicleId, amount)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    fun updateVehicle(vehicleId: Long) {
        viewModel.selectVehicle(vehicleId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
