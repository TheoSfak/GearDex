package com.geardex.app.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.geardex.app.BuildConfig
import com.geardex.app.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val prefs: SharedPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!BuildConfig.ENABLE_UPDATE_CHECK) return@withContext Result.success()

        val latestTag = fetchLatestTag() ?: return@withContext Result.retry()
        val latestVersion = latestTag.trimStart('v')
        val currentVersion = BuildConfig.VERSION_NAME

        if (isNewer(latestVersion, currentVersion)) {
            val lastNotified = prefs.getString(PREF_LAST_NOTIFIED_VERSION, "")
            if (lastNotified != latestVersion) {
                showUpdateNotification(latestVersion)
                prefs.edit { putString(PREF_LAST_NOTIFIED_VERSION, latestVersion) }
            }
        }

        Result.success()
    }

    private fun fetchLatestTag(): String? {
        val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
        var connection: HttpURLConnection? = null
        return try {
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.bufferedReader().readText()
            JSONObject(body).optString("tag_name").takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** Returns true if [candidate] is strictly newer than [current] (major.minor.patch). */
    private fun isNewer(candidate: String, current: String): Boolean {
        val c = candidate.split(".").mapNotNull { it.toIntOrNull() }
        val cur = current.split(".").mapNotNull { it.toIntOrNull() }
        val size = maxOf(c.size, cur.size)
        for (i in 0 until size) {
            val cv = c.getOrElse(i) { 0 }
            val curv = cur.getOrElse(i) { 0 }
            if (cv > curv) return true
            if (cv < curv) return false
        }
        return false
    }

    private fun showUpdateNotification(newVersion: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            "https://github.com/${BuildConfig.GITHUB_REPO}/releases/latest".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.update_notification_title)
        val message = context.getString(R.string.update_notification_message, newVersion)

        val notification = NotificationCompat.Builder(context, NotificationHelper.UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_garage)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            manager.notify(NotificationHelper.UPDATE_NOTIF_ID, notification)
        }
    }

    companion object {
        const val WORK_NAME = "update_check"
        private const val PREF_LAST_NOTIFIED_VERSION = "last_notified_update_version"
    }
}
