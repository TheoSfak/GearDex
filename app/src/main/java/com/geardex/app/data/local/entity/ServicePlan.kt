package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ServicePlanType {
    OIL_CHANGE,
    AIR_FILTER,
    BRAKE_PADS,
    TIMING_BELT,
    CABIN_FILTER,
    CHAIN_LUBE,
    VALVE_CLEARANCE,
    FORK_OIL,
    TIRE_CHECK,
    CUSTOM
}

@Entity(
    tableName = "service_plans",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class ServicePlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val type: ServicePlanType,
    val title: String,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val lastDoneKm: Int? = null,
    val lastDoneDate: Long? = null,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
