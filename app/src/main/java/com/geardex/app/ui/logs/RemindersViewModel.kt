@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.geardex.app.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.MaintenanceReminder
import com.geardex.app.data.local.entity.ReminderType
import com.geardex.app.data.repository.ReminderRepository
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
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    val vehicles = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehicleId = MutableStateFlow(-1L)

    val reminders: StateFlow<List<MaintenanceReminder>> = selectedVehicleId
        .flatMapLatest { id ->
            if (id < 0) flowOf(emptyList()) else reminderRepository.getRemindersForVehicle(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectVehicle(id: Long) { selectedVehicleId.value = id }

    fun addReminder(
        vehicleId: Long,
        title: String,
        type: ReminderType,
        targetKm: Int?,
        targetDate: Long?
    ) {
        viewModelScope.launch {
            reminderRepository.addReminder(
                MaintenanceReminder(
                    vehicleId = vehicleId,
                    title = title,
                    type = type,
                    targetKm = targetKm,
                    targetDate = targetDate
                )
            )
        }
    }

    fun markDone(reminder: MaintenanceReminder) {
        viewModelScope.launch { reminderRepository.markDone(reminder.id) }
    }

    fun deleteReminder(reminder: MaintenanceReminder) {
        viewModelScope.launch { reminderRepository.deleteReminder(reminder) }
    }
}
