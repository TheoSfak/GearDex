package com.geardex.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DocumentType { KTEO, INSURANCE, ROAD_TAX, RECEIPT, OTHER }

@Entity(
    tableName = "glovebox_documents",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class GloveboxDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val documentType: DocumentType,
    val localFilePath: String,    // Stored locally on device ONLY
    val fileName: String,
    val expiryDate: Long? = null, // Nullable — not all docs have expiry
    val addedAt: Long = System.currentTimeMillis()
)
