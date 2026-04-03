package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.TripDao
import com.geardex.app.data.local.entity.Trip
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao
) {
    fun getTripsForVehicle(vehicleId: Long): Flow<List<Trip>> =
        tripDao.getTripsForVehicle(vehicleId)

    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()

    suspend fun getTotalDistanceForVehicle(vehicleId: Long): Int =
        tripDao.getTotalDistanceForVehicle(vehicleId) ?: 0

    suspend fun getTotalCostForVehicle(vehicleId: Long): Double =
        tripDao.getTotalCostForVehicle(vehicleId) ?: 0.0

    suspend fun getDistanceByPurpose(vehicleId: Long, purpose: String): Int =
        tripDao.getDistanceByPurpose(vehicleId, purpose) ?: 0

    suspend fun getFleetTotalDistance(): Int =
        tripDao.getFleetTotalDistance() ?: 0

    suspend fun addTrip(trip: Trip) = tripDao.insert(trip)

    suspend fun deleteTrip(tripId: Long) = tripDao.delete(tripId)
}
