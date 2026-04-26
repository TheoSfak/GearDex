package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_route_reviews")
data class LocalRouteReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeKey: String,
    val rating: Float,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
