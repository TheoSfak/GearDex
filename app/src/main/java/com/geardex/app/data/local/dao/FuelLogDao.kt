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

    @Query("SELECT * FROM fuel_logs ORDER BY date DESC")
    suspend fun getAllFuelLogsSync(): List<FuelLog>

    @Query("SELECT * FROM fuel_logs WHERE vehicleId = :vehicleId ORDER BY date DESC")
    suspend fun getFuelLogsForVehicleSync(vehicleId: Long): List<FuelLog>

    @Query("SELECT AVG(fuelEconomy) FROM fuel_logs WHERE vehicleId = :vehicleId AND fuelEconomy IS NOT NULL")
    suspend fun getAverageFuelEconomy(vehicleId: Long): Double?

    @Query("DELETE FROM fuel_logs")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<FuelLog>)

    @Transaction
    suspend fun replaceAll(logs: List<FuelLog>) {
        clearAll()
        insertAll(logs)
    }
}
