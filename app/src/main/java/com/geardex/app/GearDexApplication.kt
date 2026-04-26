package com.geardex.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.geardex.app.notifications.MaintenanceNotificationWorker
import com.geardex.app.notifications.NotificationHelper
import com.geardex.app.notifications.UpdateCheckWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class GearDexApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        initFirebase()
        createNotificationChannels()
        scheduleMaintenanceCheck()
        scheduleUpdateCheck()
    }

    private fun initFirebase() {
        if (!BuildConfig.FIREBASE_ENABLED) return
        val appId = BuildConfig.FIREBASE_APP_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY
        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        if (appId.isEmpty() || apiKey.isEmpty() || projectId.isEmpty()) return
        val options = FirebaseOptions.Builder()
            .setApplicationId(appId)
            .setApiKey(apiKey)
            .setProjectId(projectId)
            .build()
        runCatching { FirebaseApp.initializeApp(this, options) }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.MAINTENANCE_CHANNEL_ID,
                NotificationHelper.MAINTENANCE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Alerts when your vehicle is due for maintenance" }
        )
        if (BuildConfig.ENABLE_UPDATE_CHECK) {
            manager.createNotificationChannel(
                NotificationChannel(
                    NotificationHelper.UPDATE_CHANNEL_ID,
                    NotificationHelper.UPDATE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Notifies when a new app version is available" }
            )
        }
    }

    private fun scheduleMaintenanceCheck() {
        val request = PeriodicWorkRequestBuilder<MaintenanceNotificationWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "maintenance_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleUpdateCheck() {
        if (!BuildConfig.ENABLE_UPDATE_CHECK) return
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UpdateCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
