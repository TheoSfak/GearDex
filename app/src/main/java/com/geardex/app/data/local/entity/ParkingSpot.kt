package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parking_spots")
data class ParkingSpot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long = -1L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val notes: String = "",
    val meterExpiryMs: Long = 0L,
    val savedAt: Long = System.currentTimeMillis()
)
