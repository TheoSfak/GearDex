package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.ParkingSpotDao
import com.geardex.app.data.local.entity.ParkingSpot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParkingSpotRepository @Inject constructor(
    private val dao: ParkingSpotDao
) {
    fun getAllSpots(): Flow<List<ParkingSpot>> = dao.getAll()

    suspend fun saveSpot(spot: ParkingSpot): Long = dao.insert(spot)

    suspend fun deleteSpot(spot: ParkingSpot) = dao.delete(spot)
}
