@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.geardex.app.ui.logs

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.dao.CategoryTotal
import com.geardex.app.data.local.entity.Expense
import com.geardex.app.data.local.entity.ExpenseCategory
import com.geardex.app.data.repository.ExpenseRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ExpenseSummary(
    val monthTotal: Double,
    val budget: Double?,
    val budgetRemaining: Double?,
    val budgetPercent: Int,
    val categoryBreakdown: List<CategoryTotal>
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val vehicleRepository: VehicleRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    val vehicles = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehicleId = MutableStateFlow(-1L)

    val expenses: StateFlow<List<Expense>> = selectedVehicleId
        .flatMapLatest { id ->
            if (id < 0) flowOf(emptyList()) else expenseRepository.getExpensesForVehicle(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _summary = MutableStateFlow(ExpenseSummary(0.0, null, null, 0, emptyList()))
    val summary: StateFlow<ExpenseSummary> = _summary

    fun selectVehicle(id: Long) {
        selectedVehicleId.value = id
        refreshSummary(id)
    }

    fun refreshSummary(vehicleId: Long = selectedVehicleId.value) {
        if (vehicleId < 0) return
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis
            val monthEnd = System.currentTimeMillis()

            val total = expenseRepository.getTotalSpendInRange(vehicleId, monthStart, monthEnd) ?: 0.0
            val breakdown = expenseRepository.getSpendByCategory(vehicleId, monthStart, monthEnd)
            val budget = getBudget(vehicleId)
            val remaining = budget?.let { it - total }
            val percent = budget?.let { if (it > 0) ((total / it) * 100).toInt().coerceIn(0, 200) else 0 } ?: 0

            _summary.value = ExpenseSummary(total, budget, remaining, percent, breakdown)
        }
    }

    fun addExpense(
        vehicleId: Long,
        category: ExpenseCategory,
        amount: Double,
        date: Long,
        description: String,
        isRecurring: Boolean,
        intervalMonths: Int?
    ) {
        viewModelScope.launch {
            expenseRepository.addExpense(
                Expense(
                    vehicleId = vehicleId,
                    category = category,
                    amount = amount,
                    date = date,
                    description = description,
                    isRecurring = isRecurring,
                    recurringIntervalMonths = intervalMonths
                )
            )
            refreshSummary(vehicleId)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
            refreshSummary()
        }
    }

    fun setBudget(vehicleId: Long, amount: Double) {
        prefs.edit() {putFloat("budget_$vehicleId", amount.toFloat())}
        refreshSummary(vehicleId)
    }

    fun getBudget(vehicleId: Long): Double? {
        val value = prefs.getFloat("budget_$vehicleId", -1f)
        return if (value < 0) null else value.toDouble()
    }
}
