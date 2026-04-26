package com.geardex.app.notifications

object NotificationHelper {
    const val MAINTENANCE_CHANNEL_ID = "maintenance_alerts"
    const val MAINTENANCE_CHANNEL_NAME = "Maintenance Alerts"
    /** Matches HealthScoreCalculator.KM_HIGH — triggers notification at same point score drops significantly */
    const val KM_SERVICE_THRESHOLD = 5000

    const val UPDATE_CHANNEL_ID = "app_updates"
    const val UPDATE_CHANNEL_NAME = "App Updates"
    const val UPDATE_NOTIF_ID = 9001
}
