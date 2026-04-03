package com.geardex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.geardex.app.data.local.entity.SavedRoute
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRouteDao {

    @Query("SELECT * FROM saved_routes ORDER BY savedAt DESC")
    fun getAllSavedRoutes(): Flow<List<SavedRoute>>

    @Query("SELECT routeKey FROM saved_routes")
    fun getAllSavedKeys(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_routes WHERE routeKey = :key)")
    suspend fun isSaved(key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(route: SavedRoute)

    @Query("DELETE FROM saved_routes WHERE routeKey = :key")
    suspend fun unsave(key: String)
}
