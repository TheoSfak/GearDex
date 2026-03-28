package com.geardex.app.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GarageViewModel @Inject constructor(
    private val repository: VehicleRepository
) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = repository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
