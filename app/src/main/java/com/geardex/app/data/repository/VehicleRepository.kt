package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.VehicleDao
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.remote.FirestoreSyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val firestoreSync: FirestoreSyncRepository
) {

    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()

    fun getVehicleByIdFlow(id: Long): Flow<Vehicle?> = vehicleDao.getVehicleByIdFlow(id)

    suspend fun getVehicleById(id: Long): Vehicle? = vehicleDao.getVehicleById(id)

    suspend fun addVehicle(vehicle: Vehicle): Long {
        val id = vehicleDao.insertVehicle(vehicle)
        firestoreSync.uploadVehicle(vehicle.copy(id = id))
        return id
    }

    suspend fun updateVehicle(vehicle: Vehicle) {
        vehicleDao.updateVehicle(vehicle)
        firestoreSync.uploadVehicle(vehicle)
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        vehicleDao.deleteVehicle(vehicle)
        firestoreSync.deleteVehicle(vehicle)
    }

    suspend fun updateKm(vehicleId: Long, km: Int) {
        vehicleDao.updateKm(vehicleId, km)
        val vehicle = vehicleDao.getVehicleById(vehicleId) ?: return
        firestoreSync.uploadVehicle(vehicle)
    }

    suspend fun getAllVehiclesSync(): List<Vehicle> = vehicleDao.getAllVehiclesSync()

    /**
     * Called after login. Downloads remote vehicles.
     * - If Firestore has data: replace local with remote (Firestore wins).
     * - If Firestore is empty: upload all local vehicles to Firestore.
     */
    suspend fun syncAfterLogin() {
        val remoteVehicles = firestoreSync.downloadVehicles()
        if (remoteVehicles.isNotEmpty()) {
            vehicleDao.clearAll()
            vehicleDao.insertAll(remoteVehicles)
        } else {
            val localVehicles = vehicleDao.getAllVehiclesSync()
            if (localVehicles.isNotEmpty()) {
                firestoreSync.uploadAll(localVehicles)
            }
        }
    }
}

