package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.WatchlistDao
import com.geardex.app.data.local.entity.WatchlistItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao
) {
    fun getAllItems(): Flow<List<WatchlistItem>> = watchlistDao.getAllItems()

    suspend fun addItem(item: WatchlistItem) = watchlistDao.insert(item)

    suspend fun removeItem(id: Long) = watchlistDao.delete(id)
}
