package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "drive_sessions",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class DriveSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val startTime: Long,
    val endTime: Long = 0L,
    val distanceKm: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val durationMillis: Long = 0L,
    val estimatedFuelLiters: Double = 0.0,
    val estimatedCostEuro: Double = 0.0,
    val notes: String = ""
)
