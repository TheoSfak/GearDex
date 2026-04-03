package com.geardex.app.ui.shops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geardex.app.data.local.entity.FavoriteShop
import com.geardex.app.data.model.ServiceShop
import com.geardex.app.data.model.ShopCategory
import com.geardex.app.data.remote.ShopDirectoryRepository
import com.geardex.app.data.repository.FavoriteShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopDirectoryViewModel @Inject constructor(
    private val shopDirectoryRepository: ShopDirectoryRepository,
    private val favoriteShopRepository: FavoriteShopRepository
) : ViewModel() {

    private val allShops = shopDirectoryRepository.observeShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<ShopCategory?>(null)

    val filteredShops: StateFlow<List<ServiceShop>> = combine(
        allShops, searchQuery, selectedCategory
    ) { shops, query, category ->
        shops.filter { shop ->
            val matchesQuery = query.isBlank() ||
                    shop.nameEn.contains(query, ignoreCase = true) ||
                    shop.nameEl.contains(query, ignoreCase = true) ||
                    shop.region.contains(query, ignoreCase = true)
            val matchesCategory = category == null || shop.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = favoriteShopRepository.getAllFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavorite(shop: ServiceShop) {
        viewModelScope.launch {
            val fav = FavoriteShop(
                firestoreId = shop.id,
                nameEn = shop.nameEn,
                nameEl = shop.nameEl,
                category = shop.category.name,
                region = shop.region,
                address = shop.address,
                phone = shop.phone,
                rating = shop.rating,
                latitude = shop.latitude,
                longitude = shop.longitude
            )
            favoriteShopRepository.toggleFavorite(fav)
        }
    }

    suspend fun submitShop(shop: ServiceShop): Boolean {
        return shopDirectoryRepository.submitShop(shop)
    }
}
