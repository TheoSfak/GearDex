package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExpenseCategory {
    FUEL, SERVICE, INSURANCE, ROAD_TAX, PARKING, TOLLS, FINES, TIRES, MODIFICATIONS, OTHER
}

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val category: ExpenseCategory,
    val amount: Double,
    val date: Long,
    val description: String = "",
    val isRecurring: Boolean = false,
    val recurringIntervalMonths: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
