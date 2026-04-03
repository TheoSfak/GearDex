package com.geardex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.geardex.app.data.local.entity.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getTripsForVehicle(vehicleId: Long): Flow<List<Trip>>

    @Query("SELECT * FROM trips ORDER BY date DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT SUM(distanceKm) FROM trips WHERE vehicleId = :vehicleId")
    suspend fun getTotalDistanceForVehicle(vehicleId: Long): Int?

    @Query("SELECT SUM(costEuro) FROM trips WHERE vehicleId = :vehicleId")
    suspend fun getTotalCostForVehicle(vehicleId: Long): Double?

    @Query("SELECT SUM(distanceKm) FROM trips WHERE vehicleId = :vehicleId AND purpose = :purpose")
    suspend fun getDistanceByPurpose(vehicleId: Long, purpose: String): Int?

    @Query("SELECT SUM(distanceKm) FROM trips")
    suspend fun getFleetTotalDistance(): Int?

    @Insert
    suspend fun insert(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun delete(tripId: Long)
}
