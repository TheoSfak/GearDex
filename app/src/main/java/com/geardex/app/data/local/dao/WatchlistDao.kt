package com.geardex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.geardex.app.data.local.entity.WatchlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist_items ORDER BY addedAt DESC")
    fun getAllItems(): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistItem)

    @Query("DELETE FROM watchlist_items WHERE id = :id")
    suspend fun delete(id: Long)
}
