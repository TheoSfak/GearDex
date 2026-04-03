package com.geardex.app.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VehicleRepository
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle["vehicleId"])

    val vehicle: StateFlow<Vehicle?> = repository.getVehicleByIdFlow(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun deleteVehicle(vehicle: Vehicle, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
            onComplete()
        }
    }

    fun updateKm(km: Int) {
        viewModelScope.launch { repository.updateKm(vehicleId, km) }
    }
}
