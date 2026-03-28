package com.geardex.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.geardex.app.data.local.dao.FuelLogDao
import com.geardex.app.data.local.dao.GloveboxDocumentDao
import com.geardex.app.data.local.dao.ServiceLogDao
import com.geardex.app.data.local.dao.VehicleDao
import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.data.local.entity.FuelLog
import com.geardex.app.data.local.entity.GloveboxDocument
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
}

@Database(
    entities = [Vehicle::class, FuelLog::class, ServiceLog::class, GloveboxDocument::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GearDexDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun serviceLogDao(): ServiceLogDao
    abstract fun gloveboxDocumentDao(): GloveboxDocumentDao
}
