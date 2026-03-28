package com.geardex.app.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.geardex.app.R
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MaintenanceNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val vehicleRepository: VehicleRepository,
    private val logRepository: LogRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val vehicles = vehicleRepository.getAllVehiclesSync()
        var notificationId = 200

        vehicles.forEach { vehicle ->
            val lastService = logRepository.getLastServiceLog(vehicle.id) ?: return@forEach
            val kmSinceService = vehicle.currentKm - lastService.odometer
            if (kmSinceService >= NotificationHelper.KM_SERVICE_THRESHOLD) {
                val title = "${vehicle.make} ${vehicle.model}"
                val message = "Service due: $kmSinceService km since last visit (limit: ${NotificationHelper.KM_SERVICE_THRESHOLD} km)"
                showNotification(notificationId++, title, message)
            }
        }
        return Result.success()
    }

    private fun showNotification(id: Int, title: String, message: String) {
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationHelper.MAINTENANCE_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_nav_garage)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = NotificationManagerCompat.from(applicationContext)
        if (manager.areNotificationsEnabled()) {
            manager.notify(id, notification)
        }
    }
}
