package com.geardex.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.geardex.app.notifications.MaintenanceNotificationWorker
import com.geardex.app.notifications.NotificationHelper
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
        createNotificationChannel()
        scheduleMaintenanceCheck()
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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NotificationHelper.MAINTENANCE_CHANNEL_ID,
            NotificationHelper.MAINTENANCE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when your vehicle is due for maintenance"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
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
}
