package com.geardex.app.data.local.dao

import androidx.room.*
import com.geardex.app.data.local.entity.ParkingSpot
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingSpotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spot: ParkingSpot): Long

    @Delete
    suspend fun delete(spot: ParkingSpot)

    @Query("SELECT * FROM parking_spots ORDER BY savedAt DESC")
    fun getAll(): Flow<List<ParkingSpot>>

    @Query("SELECT * FROM parking_spots WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ParkingSpot?
}
