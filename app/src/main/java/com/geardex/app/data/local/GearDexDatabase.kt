package com.geardex.app.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.geardex.app.data.local.dao.FuelLogDao
import com.geardex.app.data.local.dao.GloveboxDocumentDao
import com.geardex.app.data.local.dao.MaintenanceReminderDao
import com.geardex.app.data.local.dao.ServiceLogDao
import com.geardex.app.data.local.dao.VehicleDao
import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.GloveboxDocument
import com.geardex.app.data.local.entity.MaintenanceReminder
import com.geardex.app.data.local.entity.ReminderType
import com.geardex.app.data.local.entity.ServiceLog
import com.geardex.app.data.local.entity.Vehicle
import com.geardex.app.data.local.entity.VehicleType

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
}

@Database(
    entities = [Vehicle::class, FuelLog::class, ServiceLog::class, GloveboxDocument::class, MaintenanceReminder::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(Converters::class)
abstract class GearDexDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun serviceLogDao(): ServiceLogDao
    abstract fun gloveboxDocumentDao(): GloveboxDocumentDao
    abstract fun maintenanceReminderDao(): MaintenanceReminderDao
}
