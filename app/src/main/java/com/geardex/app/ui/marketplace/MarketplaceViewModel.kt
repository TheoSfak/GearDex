package com.geardex.app.ui.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.model.MarketplacePart
import com.geardex.app.data.model.PartCategory
import com.geardex.app.data.model.PartCondition
import com.geardex.app.data.remote.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val marketplaceRepository: MarketplaceRepository
) : ViewModel() {

    private val allParts = marketplaceRepository.observeParts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<PartCategory?>(null)

    val filteredParts: StateFlow<List<MarketplacePart>> = combine(
        allParts, searchQuery, selectedCategory
    ) { parts, query, category ->
        parts.filter { part ->
            val matchesQuery = query.isBlank() ||
                    part.partName.contains(query, ignoreCase = true) ||
                    part.vehicleMake.contains(query, ignoreCase = true) ||
                    part.vehicleModel.contains(query, ignoreCase = true)
            val matchesCategory = category == null || part.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun submitPart(
        partName: String,
        category: PartCategory,
        vehicleMake: String,
        vehicleModel: String,
        price: Double,
        condition: PartCondition,
        description: String,
        contactInfo: String,
        region: String
    ) {
        viewModelScope.launch {
            marketplaceRepository.submitPart(
                MarketplacePart(
                    partName = partName,
                    category = category,
                    vehicleMake = vehicleMake,
                    vehicleModel = vehicleModel,
                    price = price,
                    condition = condition,
                    description = description,
                    contactInfo = contactInfo,
                    region = region
                )
            )
        }
    }
}
