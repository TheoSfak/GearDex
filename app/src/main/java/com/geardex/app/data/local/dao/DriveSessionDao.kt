package com.geardex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.geardex.app.data.local.entity.DriveSession
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveSessionDao {

    @Query("SELECT * FROM drive_sessions WHERE vehicleId = :vehicleId ORDER BY startTime DESC")
    fun getSessionsForVehicle(vehicleId: Long): Flow<List<DriveSession>>

    @Query("SELECT * FROM drive_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<DriveSession>>

    @Insert
    suspend fun insert(session: DriveSession): Long

    @Update
    suspend fun update(session: DriveSession)

    @Query("DELETE FROM drive_sessions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM drive_sessions WHERE id = :id")
    suspend fun getById(id: Long): DriveSession?
}
