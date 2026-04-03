package com.geardex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.geardex.app.data.local.entity.FavoriteShop
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteShopDao {

    @Query("SELECT * FROM favorite_shops ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteShop>>

    @Query("SELECT firestoreId FROM favorite_shops")
    fun getAllFavoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(shop: FavoriteShop)

    @Query("DELETE FROM favorite_shops WHERE firestoreId = :id")
    suspend fun unsave(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_shops WHERE firestoreId = :id)")
    suspend fun isFavorite(id: String): Boolean
}
