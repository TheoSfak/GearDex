package com.geardex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.geardex.app.data.local.entity.MaintenanceReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MaintenanceReminder): Long

    @Delete
    suspend fun deleteReminder(reminder: MaintenanceReminder)

    @Query("SELECT * FROM maintenance_reminders WHERE vehicleId = :vehicleId ORDER BY createdAt DESC")
    fun getRemindersForVehicle(vehicleId: Long): Flow<List<MaintenanceReminder>>

    @Query("SELECT * FROM maintenance_reminders WHERE isDone = 0")
    suspend fun getAllActiveReminders(): List<MaintenanceReminder>

    @Query("UPDATE maintenance_reminders SET isDone = 1 WHERE id = :id")
    suspend fun markDone(id: Long)

    @Query("SELECT * FROM maintenance_reminders WHERE isDone = 0")
    fun getActiveRemindersFlow(): Flow<List<MaintenanceReminder>>

    @Query("SELECT * FROM maintenance_reminders ORDER BY createdAt DESC")
    suspend fun getAllRemindersSync(): List<MaintenanceReminder>

    @Query("DELETE FROM maintenance_reminders")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<MaintenanceReminder>)

    @Transaction
    suspend fun replaceAll(reminders: List<MaintenanceReminder>) {
        clearAll()
        insertAll(reminders)
    }
}
