package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_shops")
data class FavoriteShop(
    @PrimaryKey
    val firestoreId: String,
    val nameEn: String,
    val nameEl: String,
    val category: String,       // ShopCategory.name
    val region: String,
    val address: String = "",
    val phone: String = "",
    val rating: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val savedAt: Long = System.currentTimeMillis()
)
