package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.FuelLogDao
import com.geardex.app.data.local.dao.ServiceLogDao
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.ServiceLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val fuelLogDao: FuelLogDao,
    private val serviceLogDao: ServiceLogDao
) {
    fun getFuelLogs(vehicleId: Long): Flow<List<FuelLog>> = fuelLogDao.getFuelLogsForVehicle(vehicleId)
    fun getServiceLogs(vehicleId: Long): Flow<List<ServiceLog>> = serviceLogDao.getServiceLogsForVehicle(vehicleId)
    suspend fun getLastFuelLog(vehicleId: Long): FuelLog? = fuelLogDao.getLastFuelLog(vehicleId)
    suspend fun addFuelLog(log: FuelLog): Long = fuelLogDao.insertFuelLog(log)
    suspend fun addServiceLog(log: ServiceLog): Long = serviceLogDao.insertServiceLog(log)
    suspend fun deleteFuelLog(log: FuelLog) = fuelLogDao.deleteFuelLog(log)
    suspend fun deleteServiceLog(log: ServiceLog) = serviceLogDao.deleteServiceLog(log)

    suspend fun getLastServiceLog(vehicleId: Long): ServiceLog? = serviceLogDao.getLastServiceLogForVehicle(vehicleId)
    fun getAllServiceLogs(): Flow<List<ServiceLog>> = serviceLogDao.getAllServiceLogs()

    suspend fun getAverageFuelEconomy(vehicleId: Long): Double? = fuelLogDao.getAverageFuelEconomy(vehicleId)

    // Sync operations
    suspend fun getAllFuelLogsSync(): List<FuelLog> = fuelLogDao.getAllFuelLogsSync()
    suspend fun getAllServiceLogsSync(): List<ServiceLog> = serviceLogDao.getAllServiceLogsSync()

    suspend fun getFuelLogsSync(vehicleId: Long): List<FuelLog> = fuelLogDao.getFuelLogsForVehicleSync(vehicleId)
    suspend fun getServiceLogsSync(vehicleId: Long): List<ServiceLog> = serviceLogDao.getServiceLogsForVehicleSync(vehicleId)
    suspend fun replaceAllFuelLogs(logs: List<FuelLog>) { fuelLogDao.replaceAll(logs) }
    suspend fun replaceAllServiceLogs(logs: List<ServiceLog>) { serviceLogDao.replaceAll(logs) }
    suspend fun upsertFuelLogs(logs: List<FuelLog>) { fuelLogDao.insertAll(logs) }
    suspend fun upsertServiceLogs(logs: List<ServiceLog>) { serviceLogDao.insertAll(logs) }
}
