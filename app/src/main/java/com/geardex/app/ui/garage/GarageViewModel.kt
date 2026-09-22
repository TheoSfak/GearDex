package com.geardex.app.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.data.repository.GloveboxRepository
import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.ReminderRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GarageViewModel @Inject constructor(
    private val repository: VehicleRepository,
    private val logRepository: LogRepository,
    private val reminderRepository: ReminderRepository,
    gloveboxRepository: GloveboxRepository
) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = repository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val onboardingState: StateFlow<OnboardingState> = combine(
        vehicles,
        reminderRepository.getActiveRemindersFlow(),
        gloveboxRepository.getAllDocuments(),
        logRepository.getAllServiceLogs()
    ) { vehicleList, activeReminders, documents, serviceLogs ->
        OnboardingState(
            hasVehicle = vehicleList.isNotEmpty(),
            hasReminder = activeReminders.isNotEmpty(),
            hasDocument = documents.isNotEmpty(),
            hasServiceLog = serviceLogs.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OnboardingState())

    val scores: StateFlow<Map<Long, Int>> = combine(
        vehicles,
        logRepository.getAllServiceLogs(),
        reminderRepository.getActiveRemindersFlow()
    ) { vehicleList, allServiceLogs, activeReminders ->
        // Group once, then look up per vehicle - O(n) instead of O(n*m)
        val serviceByVehicle = allServiceLogs.groupBy { it.vehicleId }
        vehicleList.associate { vehicle ->
            val lastService = serviceByVehicle[vehicle.id]?.maxByOrNull { it.odometer }
            vehicle.id to HealthScoreCalculator.compute(vehicle, lastService, activeReminders)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addVehicle(
        type: VehicleType,
        make: String,
        model: String,
        year: Int,
        plate: String,
        km: Int,
        imagePath: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val vehicle = Vehicle(
                type = type,
                make = make,
                model = model,
                year = year,
                licensePlate = plate,
                currentKm = km,
                imagePath = imagePath
            )
            repository.addVehicle(vehicle)
            onComplete()
        }
    }
}

data class OnboardingState(
    val hasVehicle: Boolean = false,
    val hasReminder: Boolean = false,
    val hasDocument: Boolean = false,
    val hasServiceLog: Boolean = false
) {
    val completedCount: Int
        get() = listOf(hasVehicle, hasReminder, hasDocument, hasServiceLog).count { it }

    val totalCount: Int = 4

    val isComplete: Boolean
        get() = completedCount == totalCount
}
