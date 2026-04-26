package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_routes")
data class CustomRoute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val nameEl: String,
    val region: String,
    val tags: String,
    val difficulty: String,
    val distanceKm: Int,
    val descriptionEn: String,
    val descriptionEl: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val startLocation: String = "",
    val endLocation: String = "",
    val waypoints: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
