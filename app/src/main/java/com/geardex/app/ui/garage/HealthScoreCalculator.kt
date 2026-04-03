package com.geardex.app.ui.garage

import com.geardex.app.data.local.entity.Expense
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.MaintenanceReminder
import com.geardex.app.data.local.entity.ReminderType
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.Vehicle
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Breakdown of the 5 health-score pillars, each 0–100.
 */
data class HealthScoreBreakdown(
    val maintenanceCompliance: Int,  // 30% weight
    val fuelEfficiency: Int,         // 20% weight
    val serviceRegularity: Int,      // 20% weight
    val expensePattern: Int,         // 15% weight
    val vehicleCondition: Int,       // 15% weight
    val overall: Int,                // weighted average 0–100
    val predictedMonthlyCost: Double? = null
)

object HealthScoreCalculator {

    // Weights (must sum to 100)
    private const val W_MAINTENANCE = 30
    private const val W_FUEL = 20
    private const val W_SERVICE = 20
    private const val W_EXPENSE = 15
    private const val W_CONDITION = 15

    /** Legacy method — kept for backward compatibility */
    fun compute(
        vehicle: Vehicle,
        lastServiceLog: ServiceLog?,
        activeReminders: List<MaintenanceReminder>
    ): Int = computeBreakdown(vehicle, lastServiceLog, activeReminders, emptyList(), emptyList()).overall

    /** Full breakdown with all 5 pillars. */
    fun computeBreakdown(
        vehicle: Vehicle,
        lastServiceLog: ServiceLog?,
        activeReminders: List<MaintenanceReminder>,
        fuelLogs: List<FuelLog>,
        expenses: List<Expense>
    ): HealthScoreBreakdown {
        val maintenance = scoreMaintenance(vehicle, activeReminders)
        val fuel = scoreFuelEfficiency(fuelLogs)
        val service = scoreServiceRegularity(vehicle, lastServiceLog)
        val expense = scoreExpensePattern(expenses)
        val condition = scoreVehicleCondition(vehicle)

        val overall = (
            maintenance * W_MAINTENANCE +
            fuel * W_FUEL +
            service * W_SERVICE +
            expense * W_EXPENSE +
            condition * W_CONDITION
        ) / 100

        val predicted = predictMonthlyCost(fuelLogs, expenses)

        return HealthScoreBreakdown(
            maintenanceCompliance = maintenance,
            fuelEfficiency = fuel,
            serviceRegularity = service,
            expensePattern = expense,
            vehicleCondition = condition,
            overall = overall.coerceIn(0, 100),
            predictedMonthlyCost = predicted
        )
    }

    // ── Pillar 1: Maintenance Compliance (30%) ────────────────────────────

    private fun scoreMaintenance(vehicle: Vehicle, reminders: List<MaintenanceReminder>): Int {
        val vehicleReminders = reminders.filter { it.vehicleId == vehicle.id }
        if (vehicleReminders.isEmpty()) return 80 // no reminders = neutral-good

        val now = System.currentTimeMillis()
        var overdue = 0
        var total = 0

        for (r in vehicleReminders) {
            total++
            if (r.isDone) continue
            when (r.type) {
                ReminderType.KM_BASED -> {
                    val target = r.targetKm ?: continue
                    if (vehicle.currentKm >= target) overdue++
                }
                ReminderType.DATE_BASED -> {
                    val target = r.targetDate ?: continue
                    if (target < now) overdue++
                }
            }
        }

        if (total == 0) return 80
        val ratio = overdue.toDouble() / total
        return when {
            ratio <= 0.0 -> 100
            ratio <= 0.15 -> 85
            ratio <= 0.3 -> 65
            ratio <= 0.5 -> 40
            else -> 15
        }
    }

    // ── Pillar 2: Fuel Efficiency Trend (20%) ─────────────────────────────

    private fun scoreFuelEfficiency(fuelLogs: List<FuelLog>): Int {
        val sorted = fuelLogs.filter { it.fuelEconomy != null }.sortedByDescending { it.date }
        if (sorted.size < 4) return 75 // not enough data = neutral

        val recentAvg = sorted.take(3).map { it.fuelEconomy!! }.average()
        val olderAvg = sorted.drop(3).take(5).map { it.fuelEconomy!! }.average()
        if (olderAvg <= 0) return 75

        // Positive percentChange = economy worsening (higher L/100km)
        val percentChange = ((recentAvg - olderAvg) / olderAvg * 100)
        return when {
            percentChange <= -5 -> 100  // improving
            percentChange <= 0 -> 90    // stable/slightly better
            percentChange <= 5 -> 80    // very slight increase
            percentChange <= 10 -> 65   // noticeable
            percentChange <= 20 -> 40   // concerning
            else -> 15                  // severe
        }
    }

    // ── Pillar 3: Service Regularity (20%) ────────────────────────────────

    private fun scoreServiceRegularity(vehicle: Vehicle, lastService: ServiceLog?): Int {
        if (lastService == null) return 40 // no service record at all
        val kmSince = vehicle.currentKm - lastService.odometer
        return when {
            kmSince < 3000 -> 100
            kmSince < 5000 -> 80
            kmSince < 7500 -> 55
            kmSince < 10000 -> 30
            else -> 10
        }
    }

    // ── Pillar 4: Expense Pattern (15%) ───────────────────────────────────

    private fun scoreExpensePattern(expenses: List<Expense>): Int {
        if (expenses.size < 3) return 80 // not enough data

        // Compare last 30 days' spend to overall monthly average
        val now = System.currentTimeMillis()
        val thirtyDays = 30L * 86_400_000L
        val recentSpend = expenses.filter { it.date >= now - thirtyDays }.sumOf { it.amount }

        val allDates = expenses.map { it.date }
        val monthSpan = ((allDates.max() - allDates.min()) / (30.0 * 86_400_000)).coerceAtLeast(1.0)
        val monthlyAvg = expenses.sumOf { it.amount } / monthSpan

        if (monthlyAvg <= 0) return 80
        val ratio = recentSpend / monthlyAvg
        return when {
            ratio <= 0.8 -> 100  // spending less than avg
            ratio <= 1.2 -> 85   // normal range
            ratio <= 1.5 -> 65   // elevated
            ratio <= 2.0 -> 40   // high spike
            else -> 15           // extreme spike
        }
    }

    // ── Pillar 5: Vehicle Condition (age + mileage) (15%) ─────────────────

    private fun scoreVehicleCondition(vehicle: Vehicle): Int {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val age = (currentYear - vehicle.year).coerceAtLeast(0)
        val km = vehicle.currentKm

        // Age score (newer = better)
        val ageScore = when {
            age <= 2 -> 100
            age <= 5 -> 85
            age <= 10 -> 65
            age <= 15 -> 45
            else -> 25
        }

        // Mileage score
        val kmScore = when {
            km < 30_000 -> 100
            km < 75_000 -> 85
            km < 150_000 -> 65
            km < 250_000 -> 40
            else -> 20
        }

        return (ageScore + kmScore) / 2
    }

    // ── Predicted Monthly Cost ────────────────────────────────────────────

    private fun predictMonthlyCost(fuelLogs: List<FuelLog>, expenses: List<Expense>): Double? {
        val allCosts = mutableListOf<Pair<Long, Double>>()
        fuelLogs.forEach { allCosts.add(it.date to it.cost) }
        expenses.forEach { allCosts.add(it.date to it.amount) }
        if (allCosts.size < 3) return null

        val dates = allCosts.map { it.first }
        val monthSpan = ((dates.max() - dates.min()) / (30.0 * 86_400_000)).coerceAtLeast(1.0)
        val total = allCosts.sumOf { it.second }

        // Weight recent 3 months more (60%) vs. overall (40%)
        val threeMonthsAgo = System.currentTimeMillis() - 90L * 86_400_000L
        val recentCosts = allCosts.filter { it.first >= threeMonthsAgo }
        if (recentCosts.size < 2) return total / monthSpan

        val recentDates = recentCosts.map { it.first }
        val recentMonths = ((recentDates.max() - recentDates.min()) / (30.0 * 86_400_000)).coerceAtLeast(1.0)
        val recentMonthly = recentCosts.sumOf { it.second } / recentMonths
        val overallMonthly = total / monthSpan

        return (recentMonthly * 0.6 + overallMonthly * 0.4)
    }
}
