package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist_items")
data class WatchlistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleMake: String,
    val vehicleModel: String,
    val vehicleYear: Int,
    val partName: String,
    val category: String,       // PartCategory.name
    val targetPrice: Double = 0.0,
    val firestorePartId: String = "",
    val notes: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
