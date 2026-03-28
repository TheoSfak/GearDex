package com.geardex.app.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.geardex.app.R
import com.geardex.app.data.local.entity.ReminderType
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.ReminderRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class MaintenanceNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val vehicleRepository: VehicleRepository,
    private val logRepository: LogRepository,
    private val reminderRepository: ReminderRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val vehicles = vehicleRepository.getAllVehiclesSync()
        var notificationId = 200

        // Existing: notify when km since last service exceeds threshold
        vehicles.forEach { vehicle ->
            val lastService = logRepository.getLastServiceLog(vehicle.id) ?: return@forEach
            val kmSinceService = vehicle.currentKm - lastService.odometer
            if (kmSinceService >= NotificationHelper.KM_SERVICE_THRESHOLD) {
                val title = "${vehicle.make} ${vehicle.model}"
                val message = "Service due: $kmSinceService km since last visit (limit: ${NotificationHelper.KM_SERVICE_THRESHOLD} km)"
                showNotification(notificationId++, title, message)
            }
        }

        // New: check custom reminders
        val vehicleMap = vehicles.associateBy { it.id }
        val nowMs = System.currentTimeMillis()
        val sevenDaysMs = TimeUnit.DAYS.toMillis(7)

        reminderRepository.getAllActiveReminders().forEach { reminder ->
            val vehicle = vehicleMap[reminder.vehicleId] ?: return@forEach
            val vehicleName = "${vehicle.make} ${vehicle.model}"
            when (reminder.type) {
                ReminderType.DATE_BASED -> {
                    val targetDate = reminder.targetDate ?: return@forEach
                    val daysLeft = (targetDate - nowMs) / TimeUnit.DAYS.toMillis(1)
                    if (targetDate >= nowMs && daysLeft <= 7) {
                        showNotification(
                            notificationId++,
                            "$vehicleName — ${reminder.title}",
                            "Due in $daysLeft day(s)"
                        )
                    } else if (targetDate < nowMs) {
                        showNotification(
                            notificationId++,
                            "$vehicleName — ${reminder.title}",
                            "Overdue!"
                        )
                    }
                }
                ReminderType.KM_BASED -> {
                    val targetKm = reminder.targetKm ?: return@forEach
                    if (vehicle.currentKm >= targetKm) {
                        showNotification(
                            notificationId++,
                            "$vehicleName — ${reminder.title}",
                            "Target reached: ${vehicle.currentKm} / $targetKm km"
                        )
                    }
                }
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

