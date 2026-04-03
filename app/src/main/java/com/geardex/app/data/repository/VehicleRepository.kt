package com.geardex.app.data.repository

import android.util.Log
import com.geardex.app.data.local.dao.VehicleDao
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.remote.FirestoreSyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VehicleRepository"

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
        runCatching { firestoreSync.uploadVehicle(vehicle.copy(id = id)) }
            .onFailure { Log.w(TAG, "Firestore upload failed for new vehicle", it) }
        return id
    }

    suspend fun updateVehicle(vehicle: Vehicle) {
        vehicleDao.updateVehicle(vehicle)
        runCatching { firestoreSync.uploadVehicle(vehicle) }
            .onFailure { Log.w(TAG, "Firestore upload failed for vehicle update", it) }
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        vehicleDao.deleteVehicle(vehicle)
        runCatching { firestoreSync.deleteVehicle(vehicle) }
            .onFailure { Log.w(TAG, "Firestore delete failed for vehicle", it) }
    }

    suspend fun updateKm(vehicleId: Long, km: Int) {
        vehicleDao.updateKm(vehicleId, km)
        val vehicle = vehicleDao.getVehicleById(vehicleId) ?: return
        runCatching { firestoreSync.uploadVehicle(vehicle) }
            .onFailure { Log.w(TAG, "Firestore upload failed for km update", it) }
    }

    suspend fun getAllVehiclesSync(): List<Vehicle> = vehicleDao.getAllVehiclesSync()

    suspend fun replaceAllLocal(vehicles: List<Vehicle>) {
        vehicleDao.replaceAll(vehicles)
    }
}

