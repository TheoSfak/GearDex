package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.MaintenanceReminderDao
import com.geardex.app.data.local.entity.MaintenanceReminder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val dao: MaintenanceReminderDao
) {
    fun getRemindersForVehicle(vehicleId: Long): Flow<List<MaintenanceReminder>> =
        dao.getRemindersForVehicle(vehicleId)

    suspend fun getAllActiveReminders(): List<MaintenanceReminder> =
        dao.getAllActiveReminders()

    suspend fun addReminder(reminder: MaintenanceReminder): Long =
        dao.insert(reminder)

    suspend fun markDone(id: Long) = dao.markDone(id)

    suspend fun deleteReminder(reminder: MaintenanceReminder) = dao.delete(reminder)
}
