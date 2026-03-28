package com.geardex.app.di

import android.content.Context
import androidx.room.Room
import com.geardex.app.data.local.GearDexDatabase
import com.geardex.app.data.local.dao.FuelLogDao
import com.geardex.app.data.local.dao.GloveboxDocumentDao
import com.geardex.app.data.local.dao.MaintenanceReminderDao
import com.geardex.app.data.local.dao.ServiceLogDao
import com.geardex.app.data.local.dao.VehicleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GearDexDatabase =
        Room.databaseBuilder(context, GearDexDatabase::class.java, "geardex.db")
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()

    @Provides
    fun provideVehicleDao(db: GearDexDatabase): VehicleDao = db.vehicleDao()

    @Provides
    fun provideFuelLogDao(db: GearDexDatabase): FuelLogDao = db.fuelLogDao()

    @Provides
    fun provideServiceLogDao(db: GearDexDatabase): ServiceLogDao = db.serviceLogDao()

    @Provides
    fun provideGloveboxDocumentDao(db: GearDexDatabase): GloveboxDocumentDao = db.gloveboxDocumentDao()

    @Provides
    fun provideMaintenanceReminderDao(db: GearDexDatabase): MaintenanceReminderDao = db.maintenanceReminderDao()
}
