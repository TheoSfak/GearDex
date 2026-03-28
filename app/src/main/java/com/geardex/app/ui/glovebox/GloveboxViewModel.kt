package com.geardex.app.ui.glovebox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.DocumentType
import com.geardex.app.data.local.entity.GloveboxDocument
import com.geardex.app.data.repository.GloveboxRepository
import com.geardex.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class GloveboxViewModel @Inject constructor(
    private val gloveboxRepository: GloveboxRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    val documents: StateFlow<List<GloveboxDocument>> = gloveboxRepository.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehicles = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveDocument(
        vehicleId: Long,
        inputStream: InputStream,
        fileName: String,
        documentType: DocumentType,
        expiryDate: Long?
    ) {
        viewModelScope.launch {
            gloveboxRepository.saveDocument(vehicleId, inputStream, fileName, documentType, expiryDate)
        }
    }

    fun deleteDocument(document: GloveboxDocument) {
        viewModelScope.launch { gloveboxRepository.deleteDocument(document) }
    }

    suspend fun exportZip(): File = withContext(Dispatchers.IO) {
        gloveboxRepository.exportAllAsZip()
    }
}
