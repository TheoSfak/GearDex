package com.geardex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.geardex.app.data.local.entity.ServicePlan
import kotlinx.coroutines.flow.Flow

@Dao
interface ServicePlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: ServicePlan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<ServicePlan>)

    @Update
    suspend fun updatePlan(plan: ServicePlan)

    @Delete
    suspend fun deletePlan(plan: ServicePlan)

    @Query("SELECT * FROM service_plans WHERE vehicleId = :vehicleId ORDER BY enabled DESC, createdAt DESC")
    fun getPlansForVehicle(vehicleId: Long): Flow<List<ServicePlan>>

    @Query("SELECT * FROM service_plans WHERE vehicleId = :vehicleId AND enabled = 1")
    suspend fun getEnabledPlansForVehicleSync(vehicleId: Long): List<ServicePlan>

    @Query("SELECT * FROM service_plans WHERE enabled = 1")
    suspend fun getAllEnabledPlansSync(): List<ServicePlan>

    @Query("SELECT COUNT(*) FROM service_plans WHERE vehicleId = :vehicleId")
    suspend fun countForVehicle(vehicleId: Long): Int
}
