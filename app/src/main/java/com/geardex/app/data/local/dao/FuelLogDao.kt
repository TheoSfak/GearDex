package com.geardex.app.data.local.dao

import androidx.room.*
import com.geardex.app.data.local.entity.FuelLog
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelLogDao {

    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getFuelLogsForVehicle(vehicleId: Long): Flow<List<FuelLog>>

    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY odometer DESC LIMIT 1")
    suspend fun getLastFuelLog(vehicleId: Long): FuelLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLog(fuelLog: FuelLog): Long

    @Delete
    suspend fun deleteFuelLog(fuelLog: FuelLog)
}
