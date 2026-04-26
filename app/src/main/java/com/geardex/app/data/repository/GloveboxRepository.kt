package com.geardex.app.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.geardex.app.data.local.dao.GloveboxDocumentDao
import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.data.local.entity.GloveboxDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GloveboxRepository @Inject constructor(
    private val dao: GloveboxDocumentDao,
    @ApplicationContext private val context: Context
) {
    // Documents directory is sandboxed inside app's private storage and encrypted per file.
    private val docsDir: File
        get() = File(context.filesDir, "glovebox").also { it.mkdirs() }

    private val previewDir: File
        get() = File(context.cacheDir, "glovebox_preview").also { it.mkdirs() }

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
    ): Long = withContext(Dispatchers.IO) {
        val displayName = originalFileName.ifBlank { "document_${System.currentTimeMillis()}" }
        val destFile = File(docsDir, "${System.currentTimeMillis()}_${sanitizeFileName(displayName)}")
        encryptToFile(sourceStream, destFile)
        val doc = GloveboxDocument(
            vehicleId = vehicleId,
            documentType = documentType,
            localFilePath = destFile.absolutePath,
            fileName = displayName,
            expiryDate = expiryDate
        )
        dao.insertDocument(doc)
    }

    suspend fun deleteDocument(document: GloveboxDocument) = withContext(Dispatchers.IO) {
        val file = File(document.localFilePath)
        if (file.exists()) file.delete()
        cleanupCachedDocument(document.id)
        dao.deleteDocument(document)
    }

    suspend fun migratePlaintextDocumentsToEncrypted() = withContext(Dispatchers.IO) {
        dao.getAllDocumentsSync().forEach { document ->
            val file = File(document.localFilePath)
            if (file.exists()) ensureEncryptedAtRest(file)
        }
    }

    suspend fun prepareDocumentForViewing(document: GloveboxDocument): File = withContext(Dispatchers.IO) {
        val source = File(document.localFilePath)
        require(source.exists()) { "Document file is missing" }
        ensureEncryptedAtRest(source)

        cleanupCachedDocument(document.id)
        val preview = File(previewDir, "${document.id}_${sanitizeFileName(document.fileName)}")
        openDocumentInputStream(source).use { input ->
            FileOutputStream(preview).use { output -> input.copyTo(output) }
        }
        preview
    }

    /**
     * Creates a ZIP archive of all glovebox documents into the app cache dir.
     * Returns the ZIP file path for the user to share/export manually.
     * No network call is made.
     */
    suspend fun exportAllAsZip(): File = withContext(Dispatchers.IO) {
        val zipFile = File(context.cacheDir, "geardex_glovebox_export.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            val usedNames = mutableSetOf<String>()
            for (document in dao.getAllDocumentsSync()) {
                val file = File(document.localFilePath)
                if (!file.exists()) continue
                ensureEncryptedAtRest(file)
                openDocumentInputStream(file).use { input ->
                    zipOut.putNextEntry(ZipEntry(uniqueZipEntryName(document, usedNames)))
                    input.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        }
        zipFile
    }

    private fun encryptToFile(sourceStream: InputStream, destFile: File) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        sourceStream.use { input ->
            FileOutputStream(destFile).use { fileOut ->
                fileOut.write(ENCRYPTED_MAGIC)
                fileOut.write(cipher.iv.size)
                fileOut.write(cipher.iv)
                CipherOutputStream(fileOut, cipher).use { cipherOut ->
                    input.copyTo(cipherOut)
                }
            }
        }
    }

    private fun ensureEncryptedAtRest(file: File) {
        val backup = plaintextBackupFile(file)
        if (!file.exists() && backup.exists()) {
            backup.renameTo(file)
        }
        if (!file.exists() || isEncryptedFile(file)) return

        if (backup.exists()) backup.delete()
        if (!file.renameTo(backup)) return

        try {
            FileInputStream(backup).use { input -> encryptToFile(input, file) }
            backup.delete()
        } catch (error: Exception) {
            if (file.exists()) file.delete()
            backup.renameTo(file)
            throw error
        }
    }

    private fun openDocumentInputStream(file: File): InputStream {
        if (!isEncryptedFile(file)) return FileInputStream(file)

        val input = FileInputStream(file)
        val magic = ByteArray(ENCRYPTED_MAGIC.size)
        if (input.read(magic) != ENCRYPTED_MAGIC.size || !magic.contentEquals(ENCRYPTED_MAGIC)) {
            input.close()
            return FileInputStream(file)
        }

        val ivLength = input.read()
        require(ivLength > 0) { "Invalid encrypted document header" }
        val iv = ByteArray(ivLength)
        require(input.read(iv) == ivLength) { "Invalid encrypted document IV" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return CipherInputStream(input, cipher)
    }

    private fun isEncryptedFile(file: File): Boolean {
        if (file.length() < ENCRYPTED_MAGIC.size + 1L) return false
        return FileInputStream(file).use { input ->
            val magic = ByteArray(ENCRYPTED_MAGIC.size)
            input.read(magic) == ENCRYPTED_MAGIC.size && magic.contentEquals(ENCRYPTED_MAGIC)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun cleanupCachedDocument(documentId: Long) {
        previewDir.listFiles()
            ?.filter { it.name.startsWith("${documentId}_") }
            ?.forEach { it.delete() }
    }

    private fun uniqueZipEntryName(document: GloveboxDocument, usedNames: MutableSet<String>): String {
        val baseName = sanitizeFileName(document.fileName).ifBlank { "document_${document.id}" }
        if (usedNames.add(baseName)) return baseName
        val dot = baseName.lastIndexOf('.')
        val candidate = if (dot > 0) {
            "${baseName.substring(0, dot)}_${document.id}${baseName.substring(dot)}"
        } else {
            "${baseName}_${document.id}"
        }
        usedNames.add(candidate)
        return candidate
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|]"""), "_")

    private fun plaintextBackupFile(file: File): File =
        File(file.parentFile, "${file.name}.plaintext_backup")

    // Sync operations
    suspend fun getAllDocumentsSync(): List<GloveboxDocument> = dao.getAllDocumentsSync()
    suspend fun replaceAllDocuments(documents: List<GloveboxDocument>) { dao.replaceAll(documents) }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "geardex_glovebox_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private val ENCRYPTED_MAGIC = byteArrayOf(0x47, 0x44, 0x58, 0x31)
    }
}
