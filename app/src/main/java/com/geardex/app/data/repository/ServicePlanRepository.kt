package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.ServicePlanDao
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.ServicePlan
import com.geardex.app.data.local.entity.ServicePlanType
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class ServicePlanStatus { OK, SOON, DUE, OVERDUE }

data class ServicePlanSummary(
    val plan: ServicePlan,
    val status: ServicePlanStatus,
    val progressPercent: Int,
    val kmRemaining: Int?,
    val daysRemaining: Long?
)

@Singleton
class ServicePlanRepository @Inject constructor(
    private val dao: ServicePlanDao
) {
    fun getPlansForVehicle(vehicleId: Long): Flow<List<ServicePlan>> =
        dao.getPlansForVehicle(vehicleId)

    suspend fun addPlan(plan: ServicePlan): Long = dao.insertPlan(plan)

    suspend fun deletePlan(plan: ServicePlan) = dao.deletePlan(plan)

    suspend fun markCompleted(plan: ServicePlan, currentKm: Int, date: Long = System.currentTimeMillis()) {
        dao.updatePlan(plan.copy(lastDoneKm = currentKm, lastDoneDate = date, enabled = true))
    }

    suspend fun seedDefaultsIfEmpty(vehicle: Vehicle) {
        if (dao.countForVehicle(vehicle.id) > 0) return
        val now = System.currentTimeMillis()
        dao.insertAll(defaultPlans(vehicle, now))
    }

    suspend fun applyServiceLog(log: ServiceLog) {
        val completedTypes = buildSet {
            if (log.oilChange) add(ServicePlanType.OIL_CHANGE)
            if (log.airFilter) add(ServicePlanType.AIR_FILTER)
            if (log.brakePads) add(ServicePlanType.BRAKE_PADS)
            if (log.timingBelt) add(ServicePlanType.TIMING_BELT)
            if (log.cabinFilter) add(ServicePlanType.CABIN_FILTER)
            if (log.chainLube) add(ServicePlanType.CHAIN_LUBE)
            if (log.valveClearance) add(ServicePlanType.VALVE_CLEARANCE)
            if (log.forkOil) add(ServicePlanType.FORK_OIL)
            if (log.tireCheck) add(ServicePlanType.TIRE_CHECK)
        }
        if (completedTypes.isEmpty()) return

        dao.getEnabledPlansForVehicleSync(log.vehicleId)
            .filter { it.type in completedTypes }
            .forEach { plan ->
                dao.updatePlan(plan.copy(lastDoneKm = log.odometer, lastDoneDate = log.date))
            }
    }

    suspend fun getAllEnabledPlansSync(): List<ServicePlan> = dao.getAllEnabledPlansSync()

    fun buildSummaries(
        vehicle: Vehicle,
        plans: List<ServicePlan>,
        now: Long = System.currentTimeMillis()
    ): List<ServicePlanSummary> {
        return plans
            .filter { it.enabled }
            .map { plan -> buildSummary(vehicle, plan, now) }
            .sortedWith(
                compareByDescending<ServicePlanSummary> { it.status.ordinal }
                    .thenBy { it.kmRemaining ?: Int.MAX_VALUE }
                    .thenBy { it.daysRemaining ?: Long.MAX_VALUE }
            )
    }

    fun buildSummary(
        vehicle: Vehicle,
        plan: ServicePlan,
        now: Long = System.currentTimeMillis()
    ): ServicePlanSummary {
        val kmRemaining = plan.intervalKm?.let { interval ->
            val baseline = plan.lastDoneKm ?: vehicle.currentKm
            baseline + interval - vehicle.currentKm
        }
        val daysRemaining = plan.intervalMonths?.let { months ->
            val baselineDate = plan.lastDoneDate ?: plan.createdAt
            val targetDate = addMonths(baselineDate, months)
            TimeUnit.MILLISECONDS.toDays(targetDate - now)
        }
        val kmProgress = plan.intervalKm?.let { interval ->
            val baseline = plan.lastDoneKm ?: vehicle.currentKm
            val used = (vehicle.currentKm - baseline).coerceAtLeast(0)
            ((used.toDouble() / interval.toDouble()) * 100).toInt()
        }
        val dayProgress = plan.intervalMonths?.let { months ->
            val baselineDate = plan.lastDoneDate ?: plan.createdAt
            val targetDate = addMonths(baselineDate, months)
            val total = (targetDate - baselineDate).coerceAtLeast(1L)
            val used = (now - baselineDate).coerceAtLeast(0L)
            ((used.toDouble() / total.toDouble()) * 100).toInt()
        }
        val progress = listOfNotNull(kmProgress, dayProgress).maxOrNull()?.coerceIn(0, 100) ?: 0
        val status = worstStatus(
            kmRemaining?.let { remainingToStatus(it, 500, progress) },
            daysRemaining?.let { remainingToStatus(it, 14, progress) }
        )
        return ServicePlanSummary(plan, status, progress, kmRemaining, daysRemaining)
    }

    private fun defaultPlans(vehicle: Vehicle, now: Long): List<ServicePlan> {
        val base = mutableListOf(
            plan(vehicle, ServicePlanType.OIL_CHANGE, "Oil Change", 6_000, 6, now),
            plan(vehicle, ServicePlanType.AIR_FILTER, "Air Filter", 12_000, 12, now),
            plan(vehicle, ServicePlanType.BRAKE_PADS, "Brake Pads", 20_000, null, now),
            plan(vehicle, ServicePlanType.TIRE_CHECK, "Tire Check", 10_000, 12, now)
        )
        when (vehicle.type) {
            VehicleType.CAR -> {
                base += plan(vehicle, ServicePlanType.CABIN_FILTER, "Cabin Filter", 12_000, 12, now)
                base += plan(vehicle, ServicePlanType.TIMING_BELT, "Timing Belt", 90_000, 60, now)
            }
            VehicleType.MOTORCYCLE -> {
                base += plan(vehicle, ServicePlanType.CHAIN_LUBE, "Chain Lube", 800, 1, now)
                base += plan(vehicle, ServicePlanType.VALVE_CLEARANCE, "Valve Clearance", 24_000, 24, now)
                base += plan(vehicle, ServicePlanType.FORK_OIL, "Fork Oil", 24_000, 24, now)
            }
            VehicleType.ATV -> {
                base += plan(vehicle, ServicePlanType.CHAIN_LUBE, "Chain Lube", 1_000, 2, now)
            }
        }
        return base
    }

    private fun plan(
        vehicle: Vehicle,
        type: ServicePlanType,
        title: String,
        intervalKm: Int?,
        intervalMonths: Int?,
        now: Long
    ) = ServicePlan(
        vehicleId = vehicle.id,
        type = type,
        title = title,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        lastDoneKm = vehicle.currentKm,
        lastDoneDate = now,
        createdAt = now
    )

    private fun addMonths(millis: Long, months: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.MONTH, months)
        }.timeInMillis

    private fun remainingToStatus(remaining: Int, soonThreshold: Int, progress: Int): ServicePlanStatus =
        when {
            remaining < 0 -> ServicePlanStatus.OVERDUE
            remaining == 0 -> ServicePlanStatus.DUE
            remaining <= soonThreshold || progress >= 85 -> ServicePlanStatus.SOON
            else -> ServicePlanStatus.OK
        }

    private fun remainingToStatus(remaining: Long, soonThreshold: Long, progress: Int): ServicePlanStatus =
        when {
            remaining < 0 -> ServicePlanStatus.OVERDUE
            remaining == 0L -> ServicePlanStatus.DUE
            remaining <= soonThreshold || progress >= 85 -> ServicePlanStatus.SOON
            else -> ServicePlanStatus.OK
        }

    private fun worstStatus(vararg statuses: ServicePlanStatus?): ServicePlanStatus =
        statuses.filterNotNull().maxByOrNull { it.ordinal } ?: ServicePlanStatus.OK
}
