package com.geardex.app.data.local.dao

import androidx.room.*
import com.geardex.app.data.local.entity.ServiceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceLogDao {

    @Query("SELECT * FROM service_logs WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getServiceLogsForVehicle(vehicleId: Long): Flow<List<ServiceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceLog(serviceLog: ServiceLog): Long

    @Delete
    suspend fun deleteServiceLog(serviceLog: ServiceLog)

    @Query("SELECT * FROM service_logs WHERE vehicleId = :vehicleId ORDER BY date DESC LIMIT 1")
    suspend fun getLastServiceLogForVehicle(vehicleId: Long): ServiceLog?

    @Query("SELECT * FROM service_logs ORDER BY date DESC")
    fun getAllServiceLogs(): Flow<List<ServiceLog>>
}
