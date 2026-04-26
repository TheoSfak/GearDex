package com.geardex.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.geardex.app.data.local.GearDexDatabase
import com.geardex.app.data.local.dao.CustomRouteDao
import com.geardex.app.data.local.dao.DriveSessionDao
import com.geardex.app.data.local.dao.ExpenseDao
import com.geardex.app.data.local.dao.FavoriteShopDao
import com.geardex.app.data.local.dao.ParkingSpotDao
import com.geardex.app.data.local.dao.FuelLogDao
import com.geardex.app.data.local.dao.GloveboxDocumentDao
import com.geardex.app.data.local.dao.LocalRouteReviewDao
import com.geardex.app.data.local.dao.MaintenanceReminderDao
import com.geardex.app.data.local.dao.SavedRouteDao
import com.geardex.app.data.local.dao.ServiceLogDao
import com.geardex.app.data.local.dao.TripDao
import com.geardex.app.data.local.dao.VehicleDao
import com.geardex.app.data.local.dao.WatchlistDao
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
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("geardex_prefs", Context.MODE_PRIVATE)

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

    @Provides
    fun provideExpenseDao(db: GearDexDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideSavedRouteDao(db: GearDexDatabase): SavedRouteDao = db.savedRouteDao()

    @Provides
    fun provideTripDao(db: GearDexDatabase): TripDao = db.tripDao()

    @Provides
    fun provideWatchlistDao(db: GearDexDatabase): WatchlistDao = db.watchlistDao()

    @Provides
    fun provideDriveSessionDao(db: GearDexDatabase): DriveSessionDao = db.driveSessionDao()

    @Provides
    fun provideFavoriteShopDao(db: GearDexDatabase): FavoriteShopDao = db.favoriteShopDao()

    @Provides
    fun provideParkingSpotDao(db: GearDexDatabase): ParkingSpotDao = db.parkingSpotDao()

    @Provides
    fun provideCustomRouteDao(db: GearDexDatabase): CustomRouteDao = db.customRouteDao()

    @Provides
    fun provideLocalRouteReviewDao(db: GearDexDatabase): LocalRouteReviewDao = db.localRouteReviewDao()
}
