package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.CategoryTotal
import com.geardex.app.data.local.dao.ExpenseDao
import com.geardex.app.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {
    fun getExpensesForVehicle(vehicleId: Long): Flow<List<Expense>> =
        expenseDao.getExpensesForVehicle(vehicleId)

    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesForVehicleInRange(vehicleId: Long, fromDate: Long, toDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesForVehicleInRange(vehicleId, fromDate, toDate)

    suspend fun getTotalSpendInRange(vehicleId: Long, fromDate: Long, toDate: Long): Double? =
        expenseDao.getTotalSpendInRange(vehicleId, fromDate, toDate)

    suspend fun getSpendByCategory(vehicleId: Long, fromDate: Long, toDate: Long): List<CategoryTotal> =
        expenseDao.getSpendByCategory(vehicleId, fromDate, toDate)

    suspend fun addExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun getAllExpensesSync(): List<Expense> = expenseDao.getAllExpensesSync()

    suspend fun replaceAllExpenses(expenses: List<Expense>) = expenseDao.replaceAll(expenses)
}
