package com.geardex.app.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.ServicePlan
import com.geardex.app.data.local.entity.ServicePlanType
import com.geardex.app.data.local.entity.Vehicle
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
import javax.inject.Inject

@HiltViewModel
class ServicePlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val servicePlanRepository: ServicePlanRepository
) : ViewModel() {

    private val vehicleId: Long = checkNotNull(savedStateHandle["vehicleId"])

    val vehicle: StateFlow<Vehicle?> = vehicleRepository.getVehicleByIdFlow(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val plans = servicePlanRepository.getPlansForVehicle(vehicleId)

    val summaries: StateFlow<List<ServicePlanSummary>> = combine(
        vehicle.filterNotNull(), plans
    ) { vehicle, plans ->
        servicePlanRepository.buildSummaries(vehicle, plans)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            vehicleRepository.getVehicleById(vehicleId)?.let { vehicle ->
                servicePlanRepository.seedDefaultsIfEmpty(vehicle)
            }
        }
    }

    fun addPlan(type: ServicePlanType, title: String, intervalKm: Int?, intervalMonths: Int?) {
        val currentVehicle = vehicle.value ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            servicePlanRepository.addPlan(
                ServicePlan(
                    vehicleId = currentVehicle.id,
                    type = type,
                    title = title,
                    intervalKm = intervalKm,
                    intervalMonths = intervalMonths,
                    lastDoneKm = currentVehicle.currentKm,
                    lastDoneDate = now,
                    createdAt = now
                )
            )
        }
    }

    fun markDone(plan: ServicePlan) {
        val currentVehicle = vehicle.value ?: return
        viewModelScope.launch {
            servicePlanRepository.markCompleted(plan, currentVehicle.currentKm)
        }
    }

    fun deletePlan(plan: ServicePlan) {
        viewModelScope.launch { servicePlanRepository.deletePlan(plan) }
    }
}
