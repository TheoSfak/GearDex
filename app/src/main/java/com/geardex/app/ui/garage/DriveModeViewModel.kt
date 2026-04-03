package com.geardex.app.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.DriveSession
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.repository.DriveSessionRepository
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
class DriveModeViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val driveSessionRepository: DriveSessionRepository
) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehicleId = MutableStateFlow(-1L)

    val sessions: StateFlow<List<DriveSession>> = selectedVehicleId.flatMapLatest { id ->
        if (id > 0) driveSessionRepository.getSessionsForVehicle(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Drive timer state
    private val _isDriving = MutableStateFlow(false)
    val isDriving: StateFlow<Boolean> = _isDriving

    private val _startTime = MutableStateFlow(0L)
    val startTime: StateFlow<Long> = _startTime

    fun startDrive() {
        _startTime.value = System.currentTimeMillis()
        _isDriving.value = true
    }

    fun stopDrive() {
        _isDriving.value = false
    }

    fun saveSession(distanceKm: Double, notes: String) {
        val vehicleId = selectedVehicleId.value
        if (vehicleId <= 0) return
        val start = _startTime.value
        val end = System.currentTimeMillis()
        val durationMillis = end - start
        val durationHours = durationMillis / 3_600_000.0
        val avgSpeed = if (durationHours > 0) distanceKm / durationHours else 0.0

        val session = DriveSession(
            vehicleId = vehicleId,
            startTime = start,
            endTime = end,
            distanceKm = distanceKm,
            maxSpeedKmh = 0.0,
            avgSpeedKmh = avgSpeed,
            elevationGainM = 0.0,
            durationMillis = durationMillis,
            estimatedFuelLiters = 0.0,
            estimatedCostEuro = 0.0,
            notes = notes
        )

        viewModelScope.launch {
            driveSessionRepository.startSession(session)
        }
    }

    fun selectVehicle(id: Long) {
        selectedVehicleId.value = id
    }
}
