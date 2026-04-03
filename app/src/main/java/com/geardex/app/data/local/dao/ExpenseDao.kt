package com.geardex.app.data.local.dao

import androidx.room.*
import com.geardex.app.data.local.entity.Expense
import com.geardex.app.data.local.entity.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getExpensesForVehicle(vehicleId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId AND date >= :fromDate AND date <= :toDate ORDER BY date DESC")
    fun getExpensesForVehicleInRange(vehicleId: Long, fromDate: Long, toDate: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE vehicleId = :vehicleId AND date >= :fromDate AND date <= :toDate")
    suspend fun getTotalSpendInRange(vehicleId: Long, fromDate: Long, toDate: Long): Double?

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE vehicleId = :vehicleId AND date >= :fromDate AND date <= :toDate GROUP BY category")
    suspend fun getSpendByCategory(vehicleId: Long, fromDate: Long, toDate: Long): List<CategoryTotal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesSync(): List<Expense>

    @Query("DELETE FROM expenses")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Transaction
    suspend fun replaceAll(expenses: List<Expense>) {
        clearAll()
        insertAll(expenses)
    }
}

data class CategoryTotal(
    val category: ExpenseCategory,
    val total: Double
)
