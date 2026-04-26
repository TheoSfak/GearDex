@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.geardex.app.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.ServicePlanRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logRepository: LogRepository,
    private val vehicleRepository: VehicleRepository,
    private val servicePlanRepository: ServicePlanRepository
) : ViewModel() {

    val vehicles = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehicleId = MutableStateFlow<Long>(-1L)

    val fuelLogs: StateFlow<List<FuelLog>> = selectedVehicleId
        .flatMapLatest { id ->
            if (id < 0) flowOf(emptyList()) else logRepository.getFuelLogs(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serviceLogs: StateFlow<List<ServiceLog>> = selectedVehicleId
        .flatMapLatest { id ->
            if (id < 0) flowOf(emptyList()) else logRepository.getServiceLogs(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectVehicle(id: Long) { selectedVehicleId.value = id }

    fun addFuelLog(
        vehicleId: Long,
        odometer: Int,
        liters: Double,
        cost: Double,
        date: Long,
        notes: String
    ) {
        viewModelScope.launch {
            val last = logRepository.getLastFuelLog(vehicleId)
            val fuelEconomy = if (last != null && odometer > last.odometer) {
                (liters / (odometer - last.odometer)) * 100.0
            } else null
            logRepository.addFuelLog(
                FuelLog(vehicleId = vehicleId, date = date, odometer = odometer,
                    liters = liters, cost = cost, fuelEconomy = fuelEconomy, notes = notes)
            )
            vehicleRepository.updateKm(vehicleId, odometer)
        }
    }

    fun addServiceLog(
        vehicleId: Long,
        odometer: Int,
        cost: Double,
        date: Long,
        mechanicName: String,
        notes: String,
        vehicleType: VehicleType,
        checks: Map<String, Boolean>
    ) {
        viewModelScope.launch {
            val log = ServiceLog(
                vehicleId = vehicleId, date = date, odometer = odometer,
                cost = cost, mechanicName = mechanicName, notes = notes,
                oilChange = checks["oilChange"] ?: false,
                airFilter = checks["airFilter"] ?: false,
                brakePads = checks["brakePads"] ?: false,
                timingBelt = checks["timingBelt"] ?: false,
                cabinFilter = checks["cabinFilter"] ?: false,
                chainLube = checks["chainLube"] ?: false,
                valveClearance = checks["valveClearance"] ?: false,
                forkOil = checks["forkOil"] ?: false,
                tireCheck = checks["tireCheck"] ?: false
            )
            logRepository.addServiceLog(log)
            servicePlanRepository.applyServiceLog(log)
            vehicleRepository.updateKm(vehicleId, odometer)
        }
    }
}
