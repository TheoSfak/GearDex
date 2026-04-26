@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.geardex.app.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.Trip
import com.geardex.app.data.repository.TripRepository
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
class TripsViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    val vehicles = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehicleId = MutableStateFlow(-1L)

    val trips: StateFlow<List<Trip>> = selectedVehicleId
        .flatMapLatest { id ->
            if (id < 0) flowOf(emptyList()) else tripRepository.getTripsForVehicle(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _totalDistance = MutableStateFlow(0)
    val totalDistance: StateFlow<Int> = _totalDistance

    fun selectVehicle(id: Long) {
        selectedVehicleId.value = id
        refreshStats(id)
    }

    private fun refreshStats(vehicleId: Long) {
        viewModelScope.launch {
            _totalDistance.value = tripRepository.getTotalDistanceForVehicle(vehicleId)
        }
    }

    fun addTrip(
        vehicleId: Long,
        startOdometer: Int,
        endOdometer: Int,
        date: Long,
        purpose: String,
        notes: String,
        fuelUsedLiters: Double?,
        costEuro: Double?
    ) {
        viewModelScope.launch {
            val distance = endOdometer - startOdometer
            tripRepository.addTrip(
                Trip(
                    vehicleId = vehicleId,
                    startOdometer = startOdometer,
                    endOdometer = endOdometer,
                    date = date,
                    purpose = purpose,
                    notes = notes,
                    distanceKm = distance,
                    fuelUsedLiters = fuelUsedLiters ?: 0.0,
                    costEuro = costEuro ?: 0.0
                )
            )
            vehicleRepository.updateKm(vehicleId, endOdometer)
            refreshStats(vehicleId)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            tripRepository.deleteTrip(trip.id)
            refreshStats(selectedVehicleId.value)
        }
    }
}
