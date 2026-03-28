package com.geardex.app.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class AnalyticsStats(
    val avgEconomy: Double?,
    val totalFuelCost: Double,
    val totalServiceCost: Double,
    val totalRecordedKm: Int,
    // List of (index, L/100km) for economy trend line
    val economyPoints: List<Pair<Float, Float>>,
    // Map of "MM/YY" -> total spend (fuel + service) for bar chart
    val monthlySpend: Map<String, Double>
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val logRepository: LogRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    val vehicles = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehicleId = MutableStateFlow(-1L)

    private val fuelLogs: StateFlow<List<FuelLog>> = selectedVehicleId
        .flatMapLatest { id ->
            if (id < 0) flowOf(emptyList()) else logRepository.getFuelLogs(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val serviceLogs: StateFlow<List<ServiceLog>> = selectedVehicleId
        .flatMapLatest { id ->
            if (id < 0) flowOf(emptyList()) else logRepository.getServiceLogs(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<AnalyticsStats> = combine(fuelLogs, serviceLogs) { fuel, service ->
        val validEconomy = fuel.mapNotNull { it.fuelEconomy }
        val avgEconomy = if (validEconomy.isNotEmpty()) validEconomy.average() else null
        val totalFuelCost = fuel.sumOf { it.cost }
        val totalServiceCost = service.sumOf { it.cost }

        // Total km = difference between max and min odometer readings
        val fuelKm = if (fuel.size >= 2) {
            (fuel.maxOfOrNull { it.odometer } ?: 0) - (fuel.minOfOrNull { it.odometer } ?: 0)
        } else 0

        // Economy trend: reversed so oldest = leftmost (index 0)
        val economyPoints = fuel.reversed()
            .mapIndexedNotNull { idx, log ->
                log.fuelEconomy?.let { idx.toFloat() to it.toFloat() }
            }

        // Monthly spend aggregation
        val cal = Calendar.getInstance()
        val monthlySpend = mutableMapOf<String, Double>()
        fuel.forEach { log ->
            cal.timeInMillis = log.date
            val key = "%02d/%02d".format(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR) % 100)
            monthlySpend[key] = (monthlySpend[key] ?: 0.0) + log.cost
        }
        service.forEach { log ->
            cal.timeInMillis = log.date
            val key = "%02d/%02d".format(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR) % 100)
            monthlySpend[key] = (monthlySpend[key] ?: 0.0) + log.cost
        }

        AnalyticsStats(
            avgEconomy = avgEconomy,
            totalFuelCost = totalFuelCost,
            totalServiceCost = totalServiceCost,
            totalRecordedKm = fuelKm,
            economyPoints = economyPoints,
            monthlySpend = monthlySpend.toSortedMap()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsStats(null, 0.0, 0.0, 0, emptyList(), emptyMap()))

    fun selectVehicle(id: Long) { selectedVehicleId.value = id }
}
