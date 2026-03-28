package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ReminderType { KM_BASED, DATE_BASED }

@Entity(
    tableName = "maintenance_reminders",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class MaintenanceReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    val type: ReminderType,
    val targetKm: Int? = null,
    val targetDate: Long? = null,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
