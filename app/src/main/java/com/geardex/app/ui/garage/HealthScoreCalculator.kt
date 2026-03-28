package com.geardex.app.ui.garage

import com.geardex.app.data.local.entity.MaintenanceReminder
import com.geardex.app.data.local.entity.ReminderType
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.Vehicle

object HealthScoreCalculator {

    fun compute(
        vehicle: Vehicle,
        lastServiceLog: ServiceLog?,
        activeReminders: List<MaintenanceReminder>
    ): Int {
        var score = 100

        // Penalty for km since last service
        if (lastServiceLog == null) {
            score -= 20
        } else {
            val kmSince = vehicle.currentKm - lastServiceLog.odometer
            score -= when {
                kmSince >= 7500 -> 50
                kmSince >= 5000 -> 35
                kmSince >= 3000 -> 20
                else -> 0
            }
        }

        // Penalty for overdue reminders (capped per category)
        val now = System.currentTimeMillis()
        val vehicleReminders = activeReminders.filter { it.vehicleId == vehicle.id }

        var kmPenalty = 0
        var datePenalty = 0

        for (reminder in vehicleReminders) {
            when (reminder.type) {
                ReminderType.KM_BASED -> {
                    val target = reminder.targetKm ?: continue
                    if (vehicle.currentKm >= target && kmPenalty < 30) {
                        kmPenalty = minOf(kmPenalty + 15, 30)
                    }
                }
                ReminderType.DATE_BASED -> {
                    val target = reminder.targetDate ?: continue
                    if (target < now && datePenalty < 30) {
                        datePenalty = minOf(datePenalty + 15, 30)
                    }
                }
            }
        }

        score -= kmPenalty
        score -= datePenalty

        return score.coerceIn(0, 100)
    }
}
