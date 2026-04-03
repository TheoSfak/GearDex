package com.geardex.app.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditVehicleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VehicleRepository
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle["vehicleId"])

    val vehicle: StateFlow<Vehicle?> = repository.getAllVehicles()
        .map { list -> list.find { it.id == vehicleId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveChanges(
        type: VehicleType, make: String, model: String,
        year: Int, plate: String, km: Int,
        onComplete: () -> Unit = {}
    ) {
        val current = vehicle.value ?: return
        viewModelScope.launch {
            repository.updateVehicle(
                current.copy(
                    type = type,
                    make = make,
                    model = model,
                    year = year,
                    licensePlate = plate,
                    currentKm = km
                )
            )
            onComplete()
        }
    }
}
