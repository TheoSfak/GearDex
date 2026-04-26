package com.geardex.app.ui.garage

import com.geardex.app.data.local.entity.Expense
import com.geardex.app.data.local.entity.ExpenseCategory
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.ServicePlanType
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.repository.ServicePlanStatus
import com.geardex.app.data.repository.ServicePlanSummary
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

enum class ForecastDriverType {
    FUEL,
    SERVICE_PLAN,
    RECURRING_EXPENSE,
    ROUTINE_SERVICE
}

data class CostForecastDriver(
    val type: ForecastDriverType,
    val title: String,
    val amount30: Double,
    val amount90: Double,
    val amount365: Double,
    val servicePlanType: ServicePlanType? = null,
    val expenseCategory: ExpenseCategory? = null
)

data class CostForecast(
    val total30: Double,
    val total90: Double,
    val total365: Double,
    val confidence: Int,
    val drivers: List<CostForecastDriver>
)

object CostForecastCalculator {

    private const val DAY_MS = 86_400_000L
    private const val DEFAULT_DAILY_KM = 35.0

    fun build(
        vehicle: Vehicle,
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        expenses: List<Expense>,
        servicePlans: List<ServicePlanSummary>,
        now: Long = System.currentTimeMillis()
    ): CostForecast {
        val drivers = buildList {
            estimateFuel(fuelLogs, expenses, now)?.let { add(it) }
            addAll(estimateRecurringExpenses(expenses, now))
            addAll(estimateServicePlans(vehicle, fuelLogs, serviceLogs, servicePlans, now))
            estimateRoutineService(serviceLogs, expenses, now)?.let { add(it) }
        }.filter { it.amount365 > 0.0 }

        val merged = drivers
            .groupBy { it.type to it.title }
            .map { (_, items) ->
                CostForecastDriver(
                    type = items.first().type,
                    title = items.first().title,
                    amount30 = items.sumOf { it.amount30 },
                    amount90 = items.sumOf { it.amount90 },
                    amount365 = items.sumOf { it.amount365 }
                )
            }
            .sortedByDescending { it.amount365 }

        val confidence = confidence(fuelLogs, serviceLogs, expenses, servicePlans)
        return CostForecast(
            total30 = merged.sumOf { it.amount30 },
            total90 = merged.sumOf { it.amount90 },
            total365 = merged.sumOf { it.amount365 },
            confidence = confidence,
            drivers = merged.take(4)
        )
    }

    private fun estimateFuel(
        fuelLogs: List<FuelLog>,
        expenses: List<Expense>,
        now: Long
    ): CostForecastDriver? {
        val recentFuelLogs = fuelLogs
            .filter { it.date >= now - TimeUnit.DAYS.toMillis(120) }
            .sortedBy { it.date }
        val dailyFromLogs = if (recentFuelLogs.size >= 2) {
            val days = TimeUnit.MILLISECONDS.toDays(recentFuelLogs.last().date - recentFuelLogs.first().date)
                .coerceAtLeast(1)
            recentFuelLogs.sumOf { it.cost } / days.toDouble()
        } else {
            null
        }
        val recentFuelExpenses = expenses.filter {
            it.category == ExpenseCategory.FUEL && it.date >= now - TimeUnit.DAYS.toMillis(120)
        }
        val dailyFromExpenses = if (recentFuelExpenses.size >= 2) {
            val days = TimeUnit.MILLISECONDS.toDays(
                recentFuelExpenses.maxOf { it.date } - recentFuelExpenses.minOf { it.date }
            ).coerceAtLeast(1)
            recentFuelExpenses.sumOf { it.amount } / days.toDouble()
        } else {
            null
        }
        val daily = dailyFromLogs ?: dailyFromExpenses ?: return null
        return dailyDriver(ForecastDriverType.FUEL, "fuel", daily)
    }

    private fun estimateRecurringExpenses(expenses: List<Expense>, now: Long): List<CostForecastDriver> =
        expenses
            .filter { it.isRecurring && it.recurringIntervalMonths != null && it.recurringIntervalMonths > 0 }
            .groupBy { it.category }
            .mapNotNull { (category, categoryExpenses) ->
                val drivers = categoryExpenses.map { expense ->
                    val intervalDays = (expense.recurringIntervalMonths ?: return@map null) * 30L
                    val nextDate = nextOccurrence(expense.date, intervalDays, now)
                    CostForecastDriver(
                        type = ForecastDriverType.RECURRING_EXPENSE,
                        title = category.name,
                        amount30 = projectedOccurrences(expense.amount, nextDate, intervalDays, now, 30),
                        amount90 = projectedOccurrences(expense.amount, nextDate, intervalDays, now, 90),
                        amount365 = projectedOccurrences(expense.amount, nextDate, intervalDays, now, 365),
                        expenseCategory = category
                    )
                }.filterNotNull()
                drivers.takeIf { it.isNotEmpty() }?.let {
                    CostForecastDriver(
                        type = ForecastDriverType.RECURRING_EXPENSE,
                        title = category.name,
                        amount30 = it.sumOf { driver -> driver.amount30 },
                        amount90 = it.sumOf { driver -> driver.amount90 },
                        amount365 = it.sumOf { driver -> driver.amount365 },
                        expenseCategory = category
                    )
                }
            }

    private fun estimateServicePlans(
        vehicle: Vehicle,
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        servicePlans: List<ServicePlanSummary>,
        now: Long
    ): List<CostForecastDriver> {
        val dailyKm = estimateDailyKm(vehicle, fuelLogs, now)
        val averageServiceCost = averageServiceCost(serviceLogs)
        return servicePlans
            .filter { it.status != ServicePlanStatus.OK || occursWithinYear(it, dailyKm) }
            .mapNotNull { summary ->
                val daysUntil = daysUntilPlan(summary, dailyKm)
                if (daysUntil > 365) return@mapNotNull null
                val amount = estimatedPlanCost(summary.plan.type, averageServiceCost)
                CostForecastDriver(
                    type = ForecastDriverType.SERVICE_PLAN,
                    title = summary.plan.title,
                    amount30 = if (daysUntil <= 30) amount else 0.0,
                    amount90 = if (daysUntil <= 90) amount else 0.0,
                    amount365 = amount,
                    servicePlanType = summary.plan.type
                )
            }
    }

    private fun estimateRoutineService(
        serviceLogs: List<ServiceLog>,
        expenses: List<Expense>,
        now: Long
    ): CostForecastDriver? {
        val serviceSpend = serviceLogs
            .filter { it.date >= now - TimeUnit.DAYS.toMillis(365) }
            .sumOf { it.cost } +
            expenses
                .filter { (it.category == ExpenseCategory.SERVICE || it.category == ExpenseCategory.TIRES) && it.date >= now - TimeUnit.DAYS.toMillis(365) }
                .sumOf { it.amount }
        if (serviceSpend <= 0.0) return null
        val daily = serviceSpend / 365.0
        return dailyDriver(ForecastDriverType.ROUTINE_SERVICE, "routine", daily * 0.35)
    }

    private fun dailyDriver(type: ForecastDriverType, title: String, dailyAmount: Double): CostForecastDriver =
        CostForecastDriver(
            type = type,
            title = title,
            amount30 = dailyAmount * 30,
            amount90 = dailyAmount * 90,
            amount365 = dailyAmount * 365
        )

    private fun nextOccurrence(startDate: Long, intervalDays: Long, now: Long): Long {
        if (startDate >= now) return startDate
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(now - startDate)
        val intervals = ceil(elapsedDays.toDouble() / intervalDays.toDouble()).toLong().coerceAtLeast(1)
        return startDate + TimeUnit.DAYS.toMillis(intervals * intervalDays)
    }

    private fun projectedOccurrences(amount: Double, firstDate: Long, intervalDays: Long, now: Long, horizonDays: Int): Double {
        val horizon = now + TimeUnit.DAYS.toMillis(horizonDays.toLong())
        if (firstDate > horizon) return 0.0
        val count = ((TimeUnit.MILLISECONDS.toDays(horizon - firstDate) / intervalDays) + 1).coerceAtLeast(0)
        return amount * count
    }

    private fun estimateDailyKm(vehicle: Vehicle, fuelLogs: List<FuelLog>, now: Long): Double {
        val recent = fuelLogs.filter { it.date >= now - TimeUnit.DAYS.toMillis(180) }.sortedBy { it.date }
        if (recent.size >= 2) {
            val days = TimeUnit.MILLISECONDS.toDays(recent.last().date - recent.first().date).coerceAtLeast(1)
            val km = recent.last().odometer - recent.first().odometer
            if (km > 0) return km / days.toDouble()
        }
        return when {
            vehicle.currentKm >= 120_000 -> 45.0
            vehicle.currentKm >= 20_000 -> DEFAULT_DAILY_KM
            else -> 25.0
        }
    }

    private fun averageServiceCost(serviceLogs: List<ServiceLog>): Double =
        serviceLogs
            .filter { it.cost > 0.0 }
            .takeIf { it.isNotEmpty() }
            ?.map { it.cost }
            ?.average()
            ?: 120.0

    private fun occursWithinYear(summary: ServicePlanSummary, dailyKm: Double): Boolean =
        daysUntilPlan(summary, dailyKm) <= 365

    private fun daysUntilPlan(summary: ServicePlanSummary, dailyKm: Double): Long {
        val kmDays = summary.kmRemaining?.let { remaining ->
            if (remaining <= 0) 0L else ceil(remaining / dailyKm.coerceAtLeast(1.0)).toLong()
        }
        val dateDays = summary.daysRemaining?.coerceAtLeast(0)
        return listOfNotNull(kmDays, dateDays).minOrNull() ?: Long.MAX_VALUE
    }

    private fun estimatedPlanCost(type: ServicePlanType, averageServiceCost: Double): Double = when (type) {
        ServicePlanType.OIL_CHANGE -> 75.0
        ServicePlanType.AIR_FILTER -> 35.0
        ServicePlanType.BRAKE_PADS -> 160.0
        ServicePlanType.TIMING_BELT -> 450.0
        ServicePlanType.CABIN_FILTER -> 30.0
        ServicePlanType.CHAIN_LUBE -> 18.0
        ServicePlanType.VALVE_CLEARANCE -> 220.0
        ServicePlanType.FORK_OIL -> 150.0
        ServicePlanType.TIRE_CHECK -> 25.0
        ServicePlanType.CUSTOM -> averageServiceCost
    }

    private fun confidence(
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        expenses: List<Expense>,
        servicePlans: List<ServicePlanSummary>
    ): Int {
        var score = 35
        if (fuelLogs.size >= 3) score += 20
        if (serviceLogs.size >= 2) score += 15
        if (expenses.any { it.isRecurring }) score += 15
        if (servicePlans.isNotEmpty()) score += 15
        return score.coerceIn(35, 95)
    }
}
