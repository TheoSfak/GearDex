package com.geardex.app.data.remote

import android.util.Log
import com.geardex.app.data.repository.GloveboxRepository
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.ReminderRepository
import com.geardex.app.data.repository.VehicleRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncManager"

@Singleton
class SyncManager @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val logRepository: LogRepository,
    private val reminderRepository: ReminderRepository,
    private val gloveboxRepository: GloveboxRepository,
    private val firestoreSync: FirestoreSyncRepository
) {

    /**
     * Called after login. Downloads all remote data.
     * - If Firestore has vehicles: merge remote data into local data.
     * - If Firestore is empty: upload all local data to Firestore.
     */
    suspend fun syncAfterLogin() {
        val remoteVehicles = firestoreSync.downloadVehicles()
        if (remoteVehicles.isNotEmpty()) {
            // Remote has data — merge it locally without deleting offline-only rows.
            vehicleRepository.upsertAllLocal(remoteVehicles)

            val remoteFuel = firestoreSync.downloadFuelLogs()
            if (remoteFuel.isNotEmpty()) logRepository.upsertFuelLogs(remoteFuel)

            val remoteService = firestoreSync.downloadServiceLogs()
            if (remoteService.isNotEmpty()) logRepository.upsertServiceLogs(remoteService)

            val remoteReminders = firestoreSync.downloadReminders()
            if (remoteReminders.isNotEmpty()) reminderRepository.upsertReminders(remoteReminders)

            val remoteDocs = firestoreSync.downloadDocuments()
            if (remoteDocs.isNotEmpty()) gloveboxRepository.replaceAllDocuments(remoteDocs)

            Log.i(TAG, "Full download complete: ${remoteVehicles.size} vehicles, ${remoteFuel.size} fuel, ${remoteService.size} service, ${remoteReminders.size} reminders, ${remoteDocs.size} docs")
        } else {
            // Remote is empty — upload everything
            uploadAll()
        }
    }

    /**
     * Upload all local data to Firestore (manual sync).
     */
    suspend fun uploadAll() {
        val vehicles = vehicleRepository.getAllVehiclesSync()
        if (vehicles.isNotEmpty()) firestoreSync.uploadAll(vehicles)

        val fuelLogs = logRepository.getAllFuelLogsSync()
        if (fuelLogs.isNotEmpty()) firestoreSync.uploadAllFuelLogs(fuelLogs)

        val serviceLogs = logRepository.getAllServiceLogsSync()
        if (serviceLogs.isNotEmpty()) firestoreSync.uploadAllServiceLogs(serviceLogs)

        val reminders = reminderRepository.getAllRemindersSync()
        if (reminders.isNotEmpty()) firestoreSync.uploadAllReminders(reminders)

        val documents = gloveboxRepository.getAllDocumentsSync()
        if (documents.isNotEmpty()) firestoreSync.uploadAllDocuments(documents)

        Log.i(TAG, "Full upload complete: ${vehicles.size} vehicles, ${fuelLogs.size} fuel, ${serviceLogs.size} service, ${reminders.size} reminders, ${documents.size} docs")
    }
}
