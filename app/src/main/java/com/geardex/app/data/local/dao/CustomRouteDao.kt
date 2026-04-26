package com.geardex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.geardex.app.data.local.entity.CustomRoute
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRouteDao {

    @Query("SELECT * FROM custom_routes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CustomRoute>>

    @Query("SELECT * FROM custom_routes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomRoute?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: CustomRoute): Long

    @Query("DELETE FROM custom_routes WHERE id = :id")
    suspend fun delete(id: Long)
}
