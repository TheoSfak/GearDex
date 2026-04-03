package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.DriveSessionDao
import com.geardex.app.data.local.entity.DriveSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveSessionRepository @Inject constructor(
    private val driveSessionDao: DriveSessionDao
) {
    fun getSessionsForVehicle(vehicleId: Long): Flow<List<DriveSession>> =
        driveSessionDao.getSessionsForVehicle(vehicleId)

    fun getAllSessions(): Flow<List<DriveSession>> =
        driveSessionDao.getAllSessions()

    suspend fun startSession(session: DriveSession): Long =
        driveSessionDao.insert(session)

    suspend fun updateSession(session: DriveSession) =
        driveSessionDao.update(session)

    suspend fun getById(id: Long): DriveSession? =
        driveSessionDao.getById(id)

    suspend fun deleteSession(id: Long) =
        driveSessionDao.delete(id)
}
