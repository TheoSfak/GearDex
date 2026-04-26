package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.CustomRouteDao
import com.geardex.app.data.local.entity.CustomRoute
import com.geardex.app.data.model.EkdromeDifficulty
import com.geardex.app.data.model.EkdromeRegion
import com.geardex.app.data.model.EkdromeRoute
import com.geardex.app.data.model.EkdromeTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomRouteRepository @Inject constructor(
    private val dao: CustomRouteDao
) {

    fun getRoutes(): Flow<List<EkdromeRoute>> = dao.getAll().map { routes ->
        routes.map { toModel(it) }
    }

    suspend fun addRoute(
        nameEn: String,
        nameEl: String,
        region: EkdromeRegion,
        tags: List<EkdromeTag>,
        difficulty: EkdromeDifficulty,
        distanceKm: Int,
        descriptionEn: String,
        descriptionEl: String,
        latitude: Double,
        longitude: Double,
        startLocation: String,
        endLocation: String,
        waypoints: List<String>
    ): Long {
        return dao.insert(
            CustomRoute(
                nameEn = nameEn,
                nameEl = nameEl,
                region = region.name,
                tags = tags.joinToString(",") { it.name },
                difficulty = difficulty.name,
                distanceKm = distanceKm,
                descriptionEn = descriptionEn,
                descriptionEl = descriptionEl,
                latitude = latitude,
                longitude = longitude,
                startLocation = startLocation,
                endLocation = endLocation,
                waypoints = waypoints.joinToString(",")
            )
        )
    }

    companion object {
        fun localKey(id: Long): String = "local_$id"

        fun toModel(entity: CustomRoute): EkdromeRoute {
            val tags = entity.tags.split(",").mapNotNull { tag ->
                runCatching { EkdromeTag.valueOf(tag.trim()) }.getOrNull()
            }
            return EkdromeRoute(
                id = localKey(entity.id).hashCode(),
                nameEn = entity.nameEn,
                nameEl = entity.nameEl,
                region = runCatching { EkdromeRegion.valueOf(entity.region) }
                    .getOrDefault(EkdromeRegion.CRETE),
                tags = tags,
                difficulty = runCatching { EkdromeDifficulty.valueOf(entity.difficulty) }
                    .getOrDefault(EkdromeDifficulty.MEDIUM),
                distanceKm = entity.distanceKm,
                rating = 0f,
                descriptionEn = entity.descriptionEn,
                descriptionEl = entity.descriptionEl,
                latitude = entity.latitude,
                longitude = entity.longitude,
                startLocation = entity.startLocation,
                endLocation = entity.endLocation,
                waypoints = if (entity.waypoints.isBlank()) emptyList()
                else entity.waypoints.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                firestoreId = localKey(entity.id),
                reviewCount = 0
            )
        }
    }
}
