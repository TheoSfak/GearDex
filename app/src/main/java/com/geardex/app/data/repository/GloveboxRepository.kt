package com.geardex.app.data.repository

import android.content.Context
import com.geardex.app.data.local.dao.GloveboxDocumentDao
import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.data.local.entity.GloveboxDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GloveboxRepository @Inject constructor(
    private val dao: GloveboxDocumentDao,
    @ApplicationContext private val context: Context
) {
    // Documents directory is sandboxed inside app's private storage — never synced to cloud
    private val docsDir: File
        get() = File(context.filesDir, "glovebox").also { it.mkdirs() }

    fun getAllDocuments(): Flow<List<GloveboxDocument>> = dao.getAllDocuments()

    fun getDocumentsForVehicle(vehicleId: Long): Flow<List<GloveboxDocument>> =
        dao.getDocumentsForVehicle(vehicleId)

    /**
     * Copies the picked file into the app's private glovebox directory,
     * then saves the metadata to Room. The original URI is NOT stored.
     */
    suspend fun saveDocument(
        vehicleId: Long,
        sourceStream: InputStream,
        originalFileName: String,
        documentType: DocumentType,
        expiryDate: Long?
    ): Long {
        val destFile = File(docsDir, "${System.currentTimeMillis()}_$originalFileName")
        sourceStream.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        }
        val doc = GloveboxDocument(
            vehicleId = vehicleId,
            documentType = documentType,
            localFilePath = destFile.absolutePath,
            fileName = originalFileName,
            expiryDate = expiryDate
        )
        return dao.insertDocument(doc)
    }

    suspend fun deleteDocument(document: GloveboxDocument) {
        // Delete the physical file from local storage securely
        val file = File(document.localFilePath)
        if (file.exists()) file.delete()
        dao.deleteDocument(document)
    }

    /**
     * Creates a ZIP archive of all glovebox documents into the app cache dir.
     * Returns the ZIP file path for the user to share/export manually.
     * No network call is made.
     */
    suspend fun exportAllAsZip(): File {
        val zipFile = File(context.cacheDir, "geardex_glovebox_export.zip")
        val allDocs = dao.getAllDocuments()
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            // We collect synchronously here; fine for export flow
            val files = docsDir.listFiles() ?: emptyArray()
            for (file in files) {
                if (!file.exists()) continue
                FileInputStream(file).use { fis ->
                    zipOut.putNextEntry(ZipEntry(file.name))
                    fis.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        }
        return zipFile
    }
}
