package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_logs",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class ServiceLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val date: Long,
    val odometer: Int,
    val cost: Double,
    val mechanicName: String = "",
    val notes: String = "",
    // Common
    val oilChange: Boolean = false,
    val airFilter: Boolean = false,
    val brakePads: Boolean = false,
    // Car-specific
    val timingBelt: Boolean = false,
    val cabinFilter: Boolean = false,
    // Motorcycle-specific
    val chainLube: Boolean = false,
    val valveClearance: Boolean = false,
    val forkOil: Boolean = false,
    val tireCheck: Boolean = false
)
