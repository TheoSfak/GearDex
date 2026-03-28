package com.geardex.app.data.local.dao

import androidx.room.*
import com.geardex.app.data.local.entity.GloveboxDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface GloveboxDocumentDao {

    @Query("SELECT * FROM glovebox_documents WHERE vehicleId = :vehicleId ORDER BY addedAt DESC")
    fun getDocumentsForVehicle(vehicleId: Long): Flow<List<GloveboxDocument>>

    @Query("SELECT * FROM glovebox_documents ORDER BY addedAt DESC")
    fun getAllDocuments(): Flow<List<GloveboxDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: GloveboxDocument): Long

    @Delete
    suspend fun deleteDocument(document: GloveboxDocument)

    @Query("SELECT * FROM glovebox_documents WHERE expiryDate IS NOT NULL AND expiryDate < :thresholdMs")
    suspend fun getExpiredDocuments(thresholdMs: Long): List<GloveboxDocument>
}
