package com.geardex.app.di

import com.geardex.app.data.repository.LogRepository
import com.geardex.app.data.repository.ReminderRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun vehicleRepository(): VehicleRepository
    fun reminderRepository(): ReminderRepository
    fun logRepository(): LogRepository
}
