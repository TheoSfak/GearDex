package com.geardex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.geardex.app.data.local.entity.LocalRouteReview
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalRouteReviewDao {

    @Query("SELECT * FROM local_route_reviews WHERE routeKey = :routeKey ORDER BY createdAt DESC")
    fun observeForRoute(routeKey: String): Flow<List<LocalRouteReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: LocalRouteReview): Long

    @Query("DELETE FROM local_route_reviews WHERE routeKey = :routeKey")
    suspend fun deleteForRoute(routeKey: String)
}
