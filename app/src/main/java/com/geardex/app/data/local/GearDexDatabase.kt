package com.geardex.app.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.geardex.app.data.local.dao.DriveSessionDao
import com.geardex.app.data.local.dao.ExpenseDao
import com.geardex.app.data.local.dao.FavoriteShopDao
import com.geardex.app.data.local.dao.ParkingSpotDao
import com.geardex.app.data.local.dao.FuelLogDao
import com.geardex.app.data.local.dao.GloveboxDocumentDao
import com.geardex.app.data.local.dao.MaintenanceReminderDao
import com.geardex.app.data.local.dao.SavedRouteDao
import com.geardex.app.data.local.dao.ServiceLogDao
import com.geardex.app.data.local.dao.TripDao
import com.geardex.app.data.local.dao.VehicleDao
import com.geardex.app.data.local.dao.WatchlistDao
import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.data.local.entity.DriveSession
import com.geardex.app.data.local.entity.Expense
import com.geardex.app.data.local.entity.ExpenseCategory
import com.geardex.app.data.local.entity.FavoriteShop
import com.geardex.app.data.local.entity.ParkingSpot
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.GloveboxDocument
import com.geardex.app.data.local.entity.MaintenanceReminder
import com.geardex.app.data.local.entity.ReminderType
import com.geardex.app.data.local.entity.SavedRoute
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.Trip
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType
import com.geardex.app.data.local.entity.WatchlistItem

class Converters {
    @TypeConverter
    fun fromVehicleType(value: VehicleType): String = value.name

    @TypeConverter
    fun toVehicleType(value: String): VehicleType = VehicleType.valueOf(value)

    @TypeConverter
    fun fromDocumentType(value: DocumentType): String = value.name

    @TypeConverter
    fun toDocumentType(value: String): DocumentType = DocumentType.valueOf(value)

    @TypeConverter
    fun fromReminderType(value: ReminderType): String = value.name

    @TypeConverter
    fun toReminderType(value: String): ReminderType = ReminderType.valueOf(value)

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)
}

@Database(
    entities = [Vehicle::class, FuelLog::class, ServiceLog::class, GloveboxDocument::class, MaintenanceReminder::class, Expense::class, SavedRoute::class, Trip::class, WatchlistItem::class, DriveSession::class, FavoriteShop::class, ParkingSpot::class],
    version = 7,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6), AutoMigration(from = 6, to = 7)]
)
@TypeConverters(Converters::class)
abstract class GearDexDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun serviceLogDao(): ServiceLogDao
    abstract fun gloveboxDocumentDao(): GloveboxDocumentDao
    abstract fun maintenanceReminderDao(): MaintenanceReminderDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun savedRouteDao(): SavedRouteDao
    abstract fun tripDao(): TripDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun driveSessionDao(): DriveSessionDao
    abstract fun favoriteShopDao(): FavoriteShopDao
    abstract fun parkingSpotDao(): ParkingSpotDao
}
