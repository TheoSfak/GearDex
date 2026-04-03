package com.geardex.app.ui.garage

import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.data.local.entity.Expense
import com.geardex.app.data.local.entity.ExpenseCategory
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.GloveboxDocument
import com.geardex.app.data.local.entity.MaintenanceReminder
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.Vehicle
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ── Timeline ────────────────────────────────────────────────────────────────

enum class TimelineEventType { FUEL, SERVICE, REMINDER, DOCUMENT }

sealed class TimelineEvent(val timestamp: Long, val eventType: TimelineEventType) {
    data class Fuel(val log: FuelLog) : TimelineEvent(log.date, TimelineEventType.FUEL)
    data class Service(val log: ServiceLog) : TimelineEvent(log.date, TimelineEventType.SERVICE)
    data class Reminder(val reminder: MaintenanceReminder) : TimelineEvent(reminder.createdAt, TimelineEventType.REMINDER)
    data class Document(val doc: GloveboxDocument) : TimelineEvent(doc.addedAt, TimelineEventType.DOCUMENT)
}

// ── Insights ────────────────────────────────────────────────────────────────

enum class InsightSeverity { HIGH, MEDIUM, LOW }

sealed class DashboardInsight {
    abstract val severity: InsightSeverity

    data class FuelEconomyDrop(val percentDrop: Int, val recentAvg: Double) : DashboardInsight() {
        override val severity = if (percentDrop > 20) InsightSeverity.HIGH else InsightSeverity.MEDIUM
    }

    data class ServiceDueSoon(val estimatedDaysLeft: Int, val kmSinceLastService: Int) : DashboardInsight() {
        override val severity = when {
            estimatedDaysLeft <= 0 -> InsightSeverity.HIGH
            estimatedDaysLeft <= 14 -> InsightSeverity.MEDIUM
            else -> InsightSeverity.LOW
        }
    }

    data class DocumentExpiring(val documentType: DocumentType, val daysLeft: Int) : DashboardInsight() {
        override val severity = when {
            daysLeft <= 0 -> InsightSeverity.HIGH
            daysLeft <= 7 -> InsightSeverity.MEDIUM
            else -> InsightSeverity.LOW
        }
    }

    data class HighMonthlySpend(val currentMonthCost: Double, val avgMonthCost: Double) : DashboardInsight() {
        override val severity = InsightSeverity.MEDIUM
    }

    data class MileageMilestone(val nextMilestone: Int, val currentKm: Int) : DashboardInsight() {
        override val severity = InsightSeverity.LOW
    }

    data class ExpenseCategorySpike(
        val category: ExpenseCategory,
        val recentAmount: Double,
        val avgAmount: Double
    ) : DashboardInsight() {
        override val severity = InsightSeverity.MEDIUM
    }

    data class PredictedCostAlert(
        val predictedMonthly: Double,
        val historicalAvg: Double
    ) : DashboardInsight() {
        override val severity = if (predictedMonthly > historicalAvg * 1.5) InsightSeverity.HIGH else InsightSeverity.MEDIUM
    }
}

// ── Cost Summary ────────────────────────────────────────────────────────────

data class CostSummary(
    val totalFuelCost: Double,
    val totalServiceCost: Double,
    val fuelCostPerKm: Double,
    val serviceCostPerKm: Double,
    val totalCostPerKm: Double,
    val monthlyAvgSpend: Double
)

// ── Engine ───────────────────────────────────────────────────────────────────

object PredictionEngine {

    private const val SERVICE_INTERVAL_KM = 5000
    private const val FUEL_ECONOMY_DROP_THRESHOLD = 10
    private const val DOCUMENT_WARN_DAYS = 30
    private const val MONTHLY_SPEND_MULTIPLIER = 1.5

    fun buildTimeline(
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        reminders: List<MaintenanceReminder>,
        documents: List<GloveboxDocument>
    ): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        fuelLogs.mapTo(events) { TimelineEvent.Fuel(it) }
        serviceLogs.mapTo(events) { TimelineEvent.Service(it) }
        reminders.mapTo(events) { TimelineEvent.Reminder(it) }
        documents.mapTo(events) { TimelineEvent.Document(it) }
        return events.sortedByDescending { it.timestamp }
    }

    fun generateInsights(
        vehicle: Vehicle,
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>,
        documents: List<GloveboxDocument>,
        expenses: List<Expense> = emptyList()
    ): List<DashboardInsight> {
        val insights = mutableListOf<DashboardInsight>()
        analyzeFuelEconomy(fuelLogs)?.let { insights.add(it) }
        analyzeServicePrediction(vehicle, serviceLogs, fuelLogs)?.let { insights.add(it) }
        insights.addAll(analyzeDocumentExpiry(documents))
        analyzeMonthlySpend(fuelLogs, serviceLogs)?.let { insights.add(it) }
        analyzeMilestone(vehicle)?.let { insights.add(it) }
        insights.addAll(analyzeExpenseSpikes(expenses))
        return insights.sortedBy { it.severity.ordinal }
    }

    fun computeCostSummary(
        vehicle: Vehicle,
        fuelLogs: List<FuelLog>,
        serviceLogs: List<ServiceLog>
    ): CostSummary {
        val totalFuel = fuelLogs.sumOf { it.cost }
        val totalService = serviceLogs.sumOf { it.cost }
        val totalKm = if (fuelLogs.size >= 2) {
            (fuelLogs.maxOf { it.odometer } - fuelLogs.minOf { it.odometer }).coerceAtLeast(1)
        } else 0
        val fuelPerKm = if (totalKm > 0) totalFuel / totalKm else 0.0
        val servicePerKm = if (totalKm > 0) totalService / totalKm else 0.0

        val allDates = fuelLogs.map { it.date } + serviceLogs.map { it.date }
        val monthlyAvg = if (allDates.size >= 2) {
            val months = ((allDates.max() - allDates.min()) / (30.0 * 86_400_000)).coerceAtLeast(1.0)
            (totalFuel + totalService) / months
        } else {
            totalFuel + totalService
        }

        return CostSummary(totalFuel, totalService, fuelPerKm, servicePerKm, fuelPerKm + servicePerKm, monthlyAvg)
    }

    // ── Private analyzers ───────────────────────────────────────────────────

    private fun analyzeFuelEconomy(fuelLogs: List<FuelLog>): DashboardInsight.FuelEconomyDrop? {
        val withEconomy = fuelLogs.filter { it.fuelEconomy != null }.sortedByDescending { it.date }
        if (withEconomy.size < 6) return null
        val recentAvg = withEconomy.take(3).map { it.fuelEconomy!! }.average()
        val previousAvg = withEconomy.drop(3).take(3).map { it.fuelEconomy!! }.average()
        if (previousAvg <= 0) return null
        // Higher L/100km = worse economy, so positive change means worsening
        val percentChange = ((recentAvg - previousAvg) / previousAvg * 100).toInt()
        return if (percentChange >= FUEL_ECONOMY_DROP_THRESHOLD) {
            DashboardInsight.FuelEconomyDrop(percentChange, recentAvg)
        } else null
    }

    private fun analyzeServicePrediction(
        vehicle: Vehicle,
        serviceLogs: List<ServiceLog>,
        fuelLogs: List<FuelLog>
    ): DashboardInsight.ServiceDueSoon? {
        val lastService = serviceLogs.maxByOrNull { it.date } ?: return null
        val kmSinceLast = vehicle.currentKm - lastService.odometer
        val kmUntilNext = SERVICE_INTERVAL_KM - kmSinceLast
        if (kmUntilNext > 2000) return null

        val sortedFuel = fuelLogs.sortedBy { it.date }
        val dailyKm = if (sortedFuel.size >= 2) {
            val days = TimeUnit.MILLISECONDS.toDays(sortedFuel.last().date - sortedFuel.first().date).coerceAtLeast(1)
            (sortedFuel.last().odometer - sortedFuel.first().odometer).toDouble() / days
        } else 50.0

        val daysLeft = if (dailyKm > 0) (kmUntilNext / dailyKm).toInt() else 30
        return DashboardInsight.ServiceDueSoon(daysLeft, kmSinceLast)
    }

    private fun analyzeDocumentExpiry(documents: List<GloveboxDocument>): List<DashboardInsight.DocumentExpiring> {
        val now = System.currentTimeMillis()
        val threshold = now + TimeUnit.DAYS.toMillis(DOCUMENT_WARN_DAYS.toLong())
        return documents
            .filter { it.expiryDate != null && it.expiryDate <= threshold }
            .map { DashboardInsight.DocumentExpiring(it.documentType, TimeUnit.MILLISECONDS.toDays(it.expiryDate!! - now).toInt()) }
    }

    private fun analyzeMonthlySpend(fuelLogs: List<FuelLog>, serviceLogs: List<ServiceLog>): DashboardInsight.HighMonthlySpend? {
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val currentCost = fuelLogs.filter { it.date >= monthStart }.sumOf { it.cost } +
                serviceLogs.filter { it.date >= monthStart }.sumOf { it.cost }
        val allDates = fuelLogs.map { it.date } + serviceLogs.map { it.date }
        if (allDates.size < 3) return null
        val total = fuelLogs.sumOf { it.cost } + serviceLogs.sumOf { it.cost }
        val months = ((allDates.max() - allDates.min()) / (30.0 * 86_400_000)).coerceAtLeast(1.0)
        val avg = total / months
        return if (avg > 0 && currentCost > avg * MONTHLY_SPEND_MULTIPLIER) {
            DashboardInsight.HighMonthlySpend(currentCost, avg)
        } else null
    }

    private fun analyzeMilestone(vehicle: Vehicle): DashboardInsight.MileageMilestone? {
        val milestones = listOf(10_000, 25_000, 50_000, 75_000, 100_000, 150_000, 200_000, 250_000, 300_000)
        val next = milestones.firstOrNull { it > vehicle.currentKm && it - vehicle.currentKm <= 1000 }
            ?: return null
        return DashboardInsight.MileageMilestone(next, vehicle.currentKm)
    }

    private fun analyzeExpenseSpikes(expenses: List<Expense>): List<DashboardInsight> {
        if (expenses.size < 5) return emptyList()
        val now = System.currentTimeMillis()
        val thirtyDays = 30L * 86_400_000L
        val results = mutableListOf<DashboardInsight>()

        // Group by category, compare recent 30 days vs. monthly avg per category
        val byCategory = expenses.groupBy { it.category }
        for ((cat, logs) in byCategory) {
            if (logs.size < 3) continue
            val recentSpend = logs.filter { it.date >= now - thirtyDays }.sumOf { it.amount }
            val dates = logs.map { it.date }
            val months = ((dates.max() - dates.min()) / (30.0 * 86_400_000)).coerceAtLeast(1.0)
            val avgMonthly = logs.sumOf { it.amount } / months
            if (avgMonthly > 0 && recentSpend > avgMonthly * 1.8) {
                results.add(DashboardInsight.ExpenseCategorySpike(cat, recentSpend, avgMonthly))
            }
        }
        return results
    }
}
