package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TripPurpose {
    COMMUTE, LEISURE, BUSINESS, OTHER
}

@Entity(
    tableName = "trips",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val startOdometer: Int,
    val endOdometer: Int,
    val date: Long,
    val purpose: String,          // TripPurpose.name
    val notes: String = "",
    val distanceKm: Int,          // endOdometer - startOdometer
    val fuelUsedLiters: Double = 0.0,
    val costEuro: Double = 0.0
)
