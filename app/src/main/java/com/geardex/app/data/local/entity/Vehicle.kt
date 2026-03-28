package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VehicleType { CAR, MOTORCYCLE, ATV }

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: VehicleType,
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val currentKm: Int,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
