package com.geardex.app.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.repository.ExpenseRepository
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FleetStats(
    val totalVehicles: Int = 0,
    val totalKm: Int = 0,
    val totalCost: Double = 0.0,
    val cheapestVehicle: String = "—",
    val vehicleRankings: List<FleetVehicleItem> = emptyList()
)

@HiltViewModel
class FleetDashboardViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val logRepository: LogRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    val vehicles = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _stats = MutableStateFlow(FleetStats())
    val stats: StateFlow<FleetStats> = _stats

    fun refreshStats(vehicles: List<Vehicle>) {
        viewModelScope.launch {
            if (vehicles.isEmpty()) return@launch

            val totalKm = vehicles.sumOf { it.currentKm }
            var totalCost = 0.0
            val rankings = mutableListOf<FleetVehicleItem>()

            for (vehicle in vehicles) {
                val fuelLogs = logRepository.getFuelLogsSync(vehicle.id)
                val fuelCost = fuelLogs.sumOf { it.cost }
                val serviceLogs = logRepository.getServiceLogsSync(vehicle.id)
                val serviceCost = serviceLogs.sumOf { it.cost }
                val vehicleTotalCost = fuelCost + serviceCost
                totalCost += vehicleTotalCost

                val costPerKm = if (vehicle.currentKm > 0) vehicleTotalCost / vehicle.currentKm else 0.0

                rankings.add(
                    FleetVehicleItem(
                        vehicleId = vehicle.id,
                        name = "${vehicle.make} ${vehicle.model}",
                        totalKm = vehicle.currentKm,
                        totalCost = vehicleTotalCost,
                        costPerKm = costPerKm,
                        rank = 0
                    )
                )
            }

            val sorted = rankings.sortedBy { it.costPerKm }
                .mapIndexed { index, item -> item.copy(rank = index + 1) }

            _stats.value = FleetStats(
                totalVehicles = vehicles.size,
                totalKm = totalKm,
                totalCost = totalCost,
                cheapestVehicle = sorted.firstOrNull()?.name ?: "—",
                vehicleRankings = sorted
            )
        }
    }
}
