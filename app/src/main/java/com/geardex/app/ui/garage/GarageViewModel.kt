package com.geardex.app.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.ReminderRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GarageViewModel @Inject constructor(
    private val repository: VehicleRepository,
    private val logRepository: LogRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = repository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scores: StateFlow<Map<Long, Int>> = combine(
        vehicles,
        logRepository.getAllServiceLogs(),
        reminderRepository.getActiveRemindersFlow()
    ) { vehicleList, allServiceLogs, activeReminders ->
        vehicleList.associate { vehicle ->
            val lastService = allServiceLogs
                .filter { it.vehicleId == vehicle.id }
                .maxByOrNull { it.odometer }
            vehicle.id to HealthScoreCalculator.compute(vehicle, lastService, activeReminders)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addVehicle(
        type: VehicleType,
        make: String,
        model: String,
        year: Int,
        plate: String,
        km: Int
    ) {
        viewModelScope.launch {
            val vehicle = Vehicle(
                type = type,
                make = make,
                model = model,
                year = year,
                licensePlate = plate,
                currentKm = km
            )
            repository.addVehicle(vehicle)
        }
    }
}
