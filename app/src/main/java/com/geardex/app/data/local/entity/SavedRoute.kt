package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_routes")
data class SavedRoute(
    @PrimaryKey
    val routeKey: String,           // "builtin_<id>" or firestore doc ID
    val nameEn: String,
    val nameEl: String,
    val region: String,             // EkdromeRegion.name
    val tags: String,               // comma-separated EkdromeTag names
    val difficulty: String,         // EkdromeDifficulty.name
    val distanceKm: Int,
    val rating: Float,
    val descriptionEn: String,
    val descriptionEl: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val startLocation: String = "",
    val endLocation: String = "",
    val waypoints: String = "",     // comma-separated waypoint names
    val firestoreId: String = "",
    val reviewCount: Int = 0,
    val savedAt: Long = System.currentTimeMillis()
)
