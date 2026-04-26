package com.geardex.app.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.repository.ExpenseRepository
import com.geardex.app.data.repository.GloveboxRepository
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.PdfReportGenerator
import com.geardex.app.data.repository.ReminderRepository
import com.geardex.app.data.repository.ServicePlanRepository
import com.geardex.app.data.repository.ServicePlanSummary
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val logRepository: LogRepository,
    private val reminderRepository: ReminderRepository,
    private val gloveboxRepository: GloveboxRepository,
    private val expenseRepository: ExpenseRepository,
    private val servicePlanRepository: ServicePlanRepository,
    private val pdfReportGenerator: PdfReportGenerator
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle["vehicleId"])

    val vehicle: StateFlow<Vehicle?> = vehicleRepository.getVehicleByIdFlow(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val fuelLogs = logRepository.getFuelLogs(vehicleId)
    private val serviceLogs = logRepository.getServiceLogs(vehicleId)
    private val reminders = reminderRepository.getRemindersForVehicle(vehicleId)
    private val documents = gloveboxRepository.getDocumentsForVehicle(vehicleId)
    private val expenses = expenseRepository.getExpensesForVehicle(vehicleId)
    private val servicePlans = servicePlanRepository.getPlansForVehicle(vehicleId)

    init {
        viewModelScope.launch {
            vehicleRepository.getVehicleById(vehicleId)?.let { vehicle ->
                servicePlanRepository.seedDefaultsIfEmpty(vehicle)
            }
        }
    }

    val timeline: StateFlow<List<TimelineEvent>> = combine(
        fuelLogs, serviceLogs, reminders, documents
    ) { fuel, service, rem, docs ->
        PredictionEngine.buildTimeline(fuel, service, rem, docs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insights: StateFlow<List<DashboardInsight>> = combine(
        vehicle.filterNotNull(), fuelLogs, serviceLogs, documents, expenses
    ) { v, fuel, service, docs, exp ->
        PredictionEngine.generateInsights(v, fuel, service, docs, exp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val costSummary: StateFlow<CostSummary?> = combine(
        vehicle.filterNotNull(), fuelLogs, serviceLogs
    ) { v, fuel, service ->
        PredictionEngine.computeCostSummary(v, fuel, service)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val healthScore: StateFlow<Int> = combine(
        vehicle.filterNotNull(), serviceLogs, reminders, fuelLogs, expenses
    ) { v, services, rems, fuel, exp ->
        val lastService = services.maxByOrNull { it.date }
        HealthScoreCalculator.computeBreakdown(v, lastService, rems, fuel, exp).overall
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val healthBreakdown: StateFlow<HealthScoreBreakdown?> = combine(
        vehicle.filterNotNull(), serviceLogs, reminders, fuelLogs, expenses
    ) { v, services, rems, fuel, exp ->
        val lastService = services.maxByOrNull { it.date }
        HealthScoreCalculator.computeBreakdown(v, lastService, rems, fuel, exp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val servicePlanSummaries: StateFlow<List<ServicePlanSummary>> = combine(
        vehicle.filterNotNull(), servicePlans
    ) { v, plans ->
        servicePlanRepository.buildSummaries(v, plans)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateKm(km: Int) {
        viewModelScope.launch { vehicleRepository.updateKm(vehicleId, km) }
    }

    fun deleteVehicle(vehicle: Vehicle, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            vehicleRepository.deleteVehicle(vehicle)
            onComplete()
        }
    }

    suspend fun fuelLogsSnapshot(): List<FuelLog> = logRepository.getFuelLogsSync(vehicleId)

    suspend fun serviceLogsSnapshot(): List<ServiceLog> = logRepository.getServiceLogsSync(vehicleId)

    suspend fun exportVehiclePdf(): File? {
        val vehicle = vehicle.value ?: return null
        val fuelLogs = fuelLogsSnapshot()
        val serviceLogs = serviceLogsSnapshot()
        val health = healthScore.value
        val cost = costSummary.value
        return pdfReportGenerator.generateVehicleReport(
            vehicle, fuelLogs, serviceLogs, health,
            cost?.totalCostPerKm, cost?.totalFuelCost, cost?.monthlyAvgSpend
        )
    }
}
