package com.geardex.app.data.remote

import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {

    private fun vehiclesCollection(uid: String) =
        firebaseManager.firestore
            ?.collection("users")
            ?.document(uid)
            ?.collection("vehicles")

    suspend fun uploadVehicle(vehicle: Vehicle) {
        val uid = firebaseManager.currentUser?.uid ?: return
        vehiclesCollection(uid)
            ?.document(vehicle.id.toString())
            ?.set(vehicle.toMap())
            ?.await()
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        val uid = firebaseManager.currentUser?.uid ?: return
        vehiclesCollection(uid)
            ?.document(vehicle.id.toString())
            ?.delete()
            ?.await()
    }

    /**
     * Downloads all remote vehicles for the current user.
     * Returns empty list if not logged in or Firestore is unavailable.
     */
    suspend fun downloadVehicles(): List<Vehicle> {
        val uid = firebaseManager.currentUser?.uid ?: return emptyList()
        val snapshot = vehiclesCollection(uid)?.get()?.await() ?: return emptyList()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                Vehicle(
                    id = doc.getLong("id") ?: 0L,
                    type = VehicleType.valueOf(doc.getString("type") ?: "CAR"),
                    make = doc.getString("make") ?: "",
                    model = doc.getString("model") ?: "",
                    year = (doc.getLong("year")?.toInt()) ?: 2020,
                    licensePlate = doc.getString("licensePlate") ?: "",
                    currentKm = (doc.getLong("currentKm")?.toInt()) ?: 0,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }.getOrNull()
        }
    }

    /**
     * Upload all provided vehicles to Firestore (replaces remote state).
     */
    suspend fun uploadAll(vehicles: List<Vehicle>) {
        val uid = firebaseManager.currentUser?.uid ?: return
        val col = vehiclesCollection(uid) ?: return
        vehicles.forEach { vehicle ->
            col.document(vehicle.id.toString()).set(vehicle.toMap()).await()
        }
    }

    private fun Vehicle.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type.name,
        "make" to make,
        "model" to model,
        "year" to year,
        "licensePlate" to licensePlate,
        "currentKm" to currentKm,
        "createdAt" to createdAt
    )
}
