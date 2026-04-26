package com.geardex.app.data.remote

import android.util.Log
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.GloveboxDocument
import com.geardex.app.data.local.entity.MaintenanceReminder
import com.geardex.app.data.local.entity.ReminderType
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirestoreSync"
private const val BATCH_LIMIT = 500

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {

    private fun userDoc(uid: String) =
        firebaseManager.firestore
            ?.collection("users")
            ?.document(uid)

    private fun vehiclesCollection(uid: String) = userDoc(uid)?.collection("vehicles")
    private fun fuelLogsCollection(uid: String) = userDoc(uid)?.collection("fuel_logs")
    private fun serviceLogsCollection(uid: String) = userDoc(uid)?.collection("service_logs")
    private fun remindersCollection(uid: String) = userDoc(uid)?.collection("reminders")
    // ── Vehicle sync (existing) ─────────────────────────────────────────────

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

    suspend fun downloadVehicles(): List<Vehicle> {
        val uid = firebaseManager.currentUser?.uid ?: return emptyList()
        val snapshot = vehiclesCollection(uid)?.get()?.await() ?: return emptyList()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                val typeStr = doc.getString("type") ?: "CAR"
                val type = runCatching { VehicleType.valueOf(typeStr) }.getOrElse {
                    Log.w(TAG, "Unknown vehicle type '$typeStr' in doc ${doc.id}, defaulting to CAR")
                    VehicleType.CAR
                }
                Vehicle(
                    id = doc.getLong("id") ?: 0L,
                    type = type,
                    make = doc.getString("make") ?: "",
                    model = doc.getString("model") ?: "",
                    year = (doc.getLong("year")?.toInt()) ?: 2020,
                    licensePlate = doc.getString("licensePlate") ?: "",
                    currentKm = (doc.getLong("currentKm")?.toInt()) ?: 0,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    imagePath = doc.getString("imagePath")
                )
            }.onFailure {
                Log.e(TAG, "Failed to parse vehicle from doc ${doc.id}", it)
            }.getOrNull()
        }
    }

    suspend fun uploadAll(vehicles: List<Vehicle>) {
        val uid = firebaseManager.currentUser?.uid ?: return
        val col = vehiclesCollection(uid) ?: return
        val firestore = firebaseManager.firestore ?: return
        vehicles.chunked(BATCH_LIMIT).forEach { chunk ->
            firestore.batch().apply {
                chunk.forEach { vehicle -> set(col.document(vehicle.id.toString()), vehicle.toMap()) }
            }.commit().await()
        }
    }

    // ── Fuel Log sync ───────────────────────────────────────────────────────

    suspend fun uploadAllFuelLogs(logs: List<FuelLog>) {
        val uid = firebaseManager.currentUser?.uid ?: return
        val col = fuelLogsCollection(uid) ?: return
        val firestore = firebaseManager.firestore ?: return
        logs.chunked(BATCH_LIMIT).forEach { chunk ->
            firestore.batch().apply {
                chunk.forEach { log -> set(col.document(log.id.toString()), log.toMap()) }
            }.commit().await()
        }
    }

    suspend fun downloadFuelLogs(): List<FuelLog> {
        val uid = firebaseManager.currentUser?.uid ?: return emptyList()
        val snapshot = fuelLogsCollection(uid)?.get()?.await() ?: return emptyList()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                FuelLog(
                    id = doc.getLong("id") ?: 0L,
                    vehicleId = doc.getLong("vehicleId") ?: 0L,
                    date = doc.getLong("date") ?: 0L,
                    odometer = (doc.getLong("odometer")?.toInt()) ?: 0,
                    liters = doc.getDouble("liters") ?: 0.0,
                    cost = doc.getDouble("cost") ?: 0.0,
                    fuelEconomy = doc.getDouble("fuelEconomy"),
                    notes = doc.getString("notes") ?: ""
                )
            }.onFailure { Log.e(TAG, "Failed to parse fuel log ${doc.id}", it) }.getOrNull()
        }
    }

    // ── Service Log sync ────────────────────────────────────────────────────

    suspend fun uploadAllServiceLogs(logs: List<ServiceLog>) {
        val uid = firebaseManager.currentUser?.uid ?: return
        val col = serviceLogsCollection(uid) ?: return
        val firestore = firebaseManager.firestore ?: return
        logs.chunked(BATCH_LIMIT).forEach { chunk ->
            firestore.batch().apply {
                chunk.forEach { log -> set(col.document(log.id.toString()), log.toMap()) }
            }.commit().await()
        }
    }

    suspend fun downloadServiceLogs(): List<ServiceLog> {
        val uid = firebaseManager.currentUser?.uid ?: return emptyList()
        val snapshot = serviceLogsCollection(uid)?.get()?.await() ?: return emptyList()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                ServiceLog(
                    id = doc.getLong("id") ?: 0L,
                    vehicleId = doc.getLong("vehicleId") ?: 0L,
                    date = doc.getLong("date") ?: 0L,
                    odometer = (doc.getLong("odometer")?.toInt()) ?: 0,
                    cost = doc.getDouble("cost") ?: 0.0,
                    mechanicName = doc.getString("mechanicName") ?: "",
                    notes = doc.getString("notes") ?: "",
                    oilChange = doc.getBoolean("oilChange") ?: false,
                    airFilter = doc.getBoolean("airFilter") ?: false,
                    brakePads = doc.getBoolean("brakePads") ?: false,
                    timingBelt = doc.getBoolean("timingBelt") ?: false,
                    cabinFilter = doc.getBoolean("cabinFilter") ?: false,
                    chainLube = doc.getBoolean("chainLube") ?: false,
                    valveClearance = doc.getBoolean("valveClearance") ?: false,
                    forkOil = doc.getBoolean("forkOil") ?: false,
                    tireCheck = doc.getBoolean("tireCheck") ?: false
                )
            }.onFailure { Log.e(TAG, "Failed to parse service log ${doc.id}", it) }.getOrNull()
        }
    }

    // ── Reminder sync ───────────────────────────────────────────────────────

    suspend fun uploadAllReminders(reminders: List<MaintenanceReminder>) {
        val uid = firebaseManager.currentUser?.uid ?: return
        val col = remindersCollection(uid) ?: return
        val firestore = firebaseManager.firestore ?: return
        reminders.chunked(BATCH_LIMIT).forEach { chunk ->
            firestore.batch().apply {
                chunk.forEach { r -> set(col.document(r.id.toString()), r.toMap()) }
            }.commit().await()
        }
    }

    suspend fun downloadReminders(): List<MaintenanceReminder> {
        val uid = firebaseManager.currentUser?.uid ?: return emptyList()
        val snapshot = remindersCollection(uid)?.get()?.await() ?: return emptyList()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                val typeStr = doc.getString("type") ?: "KM_BASED"
                val type = runCatching { ReminderType.valueOf(typeStr) }.getOrElse { ReminderType.KM_BASED }
                MaintenanceReminder(
                    id = doc.getLong("id") ?: 0L,
                    vehicleId = doc.getLong("vehicleId") ?: 0L,
                    title = doc.getString("title") ?: "",
                    type = type,
                    targetKm = doc.getLong("targetKm")?.toInt(),
                    targetDate = doc.getLong("targetDate"),
                    isDone = doc.getBoolean("isDone") ?: false,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }.onFailure { Log.e(TAG, "Failed to parse reminder ${doc.id}", it) }.getOrNull()
        }
    }

    // ── Document sync ───────────────────────────────────────────────────────
    //
    // Glovebox documents intentionally stay local-only. Syncing metadata would
    // either leak device-local paths or create rows that cannot open on another
    // device because the encrypted file bytes are not in Firestore.

    suspend fun uploadAllDocuments(documents: List<GloveboxDocument>) = Unit

    suspend fun downloadDocuments(): List<GloveboxDocument> = emptyList()

    // ── toMap extensions ────────────────────────────────────────────────────

    private fun Vehicle.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type.name,
        "make" to make,
        "model" to model,
        "year" to year,
        "licensePlate" to licensePlate,
        "currentKm" to currentKm,
        "createdAt" to createdAt,
        "imagePath" to imagePath
    )

    private fun FuelLog.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "vehicleId" to vehicleId,
        "date" to date,
        "odometer" to odometer,
        "liters" to liters,
        "cost" to cost,
        "fuelEconomy" to fuelEconomy,
        "notes" to notes
    )

    private fun ServiceLog.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "vehicleId" to vehicleId,
        "date" to date,
        "odometer" to odometer,
        "cost" to cost,
        "mechanicName" to mechanicName,
        "notes" to notes,
        "oilChange" to oilChange,
        "airFilter" to airFilter,
        "brakePads" to brakePads,
        "timingBelt" to timingBelt,
        "cabinFilter" to cabinFilter,
        "chainLube" to chainLube,
        "valveClearance" to valveClearance,
        "forkOil" to forkOil,
        "tireCheck" to tireCheck
    )

    private fun MaintenanceReminder.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "vehicleId" to vehicleId,
        "title" to title,
        "type" to type.name,
        "targetKm" to targetKm,
        "targetDate" to targetDate,
        "isDone" to isDone,
        "createdAt" to createdAt
    )

}
