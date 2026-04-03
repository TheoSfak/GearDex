package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.FavoriteShopDao
import com.geardex.app.data.local.entity.FavoriteShop
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteShopRepository @Inject constructor(
    private val favoriteShopDao: FavoriteShopDao
) {
    fun getAllFavorites(): Flow<List<FavoriteShop>> = favoriteShopDao.getAllFavorites()

    fun getAllFavoriteIds(): Flow<List<String>> = favoriteShopDao.getAllFavoriteIds()

    suspend fun toggleFavorite(shop: FavoriteShop) {
        if (favoriteShopDao.isFavorite(shop.firestoreId)) {
            favoriteShopDao.unsave(shop.firestoreId)
        } else {
            favoriteShopDao.save(shop)
        }
    }

    suspend fun isFavorite(id: String): Boolean = favoriteShopDao.isFavorite(id)
}
