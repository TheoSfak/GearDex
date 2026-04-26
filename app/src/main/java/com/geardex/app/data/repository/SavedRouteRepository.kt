package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.SavedRouteDao
import com.geardex.app.data.local.entity.SavedRoute
import com.geardex.app.data.model.EkdromeDifficulty
import com.geardex.app.data.model.EkdromeRegion
import com.geardex.app.data.model.EkdromeRoute
import com.geardex.app.data.model.EkdromeTag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedRouteRepository @Inject constructor(
    private val dao: SavedRouteDao
) {

    fun getAllSavedRoutes(): Flow<List<SavedRoute>> = dao.getAllSavedRoutes()

    fun getAllSavedKeys(): Flow<List<String>> = dao.getAllSavedKeys()

    suspend fun isSaved(route: EkdromeRoute): Boolean =
        dao.isSaved(routeKey(route))

    suspend fun toggleSave(route: EkdromeRoute) {
        val key = routeKey(route)
        if (dao.isSaved(key)) {
            dao.unsave(key)
        } else {
            dao.save(toEntity(route))
        }
    }

    suspend fun save(route: EkdromeRoute) = dao.save(toEntity(route))

    suspend fun unsave(route: EkdromeRoute) = dao.unsave(routeKey(route))

    fun routeKey(route: EkdromeRoute): String =
        if (route.firestoreId.isNotBlank()) route.firestoreId else "builtin_${route.id}"

    private fun toEntity(route: EkdromeRoute): SavedRoute = SavedRoute(
        routeKey = routeKey(route),
        nameEn = route.nameEn,
        nameEl = route.nameEl,
        region = route.region.name,
        tags = route.tags.joinToString(",") { it.name },
        difficulty = route.difficulty.name,
        distanceKm = route.distanceKm,
        rating = route.rating,
        descriptionEn = route.descriptionEn,
        descriptionEl = route.descriptionEl,
        latitude = route.latitude,
        longitude = route.longitude,
        startLocation = route.startLocation,
        endLocation = route.endLocation,
        waypoints = route.waypoints.joinToString(","),
        firestoreId = route.firestoreId,
        reviewCount = route.reviewCount
    )

    companion object {
        fun toModel(entity: SavedRoute): EkdromeRoute {
            val region = runCatching { EkdromeRegion.valueOf(entity.region) }
                .getOrDefault(EkdromeRegion.CRETE)
            val tags = entity.tags.split(",").mapNotNull { s ->
                runCatching { EkdromeTag.valueOf(s.trim()) }.getOrNull()
            }
            val difficulty = runCatching { EkdromeDifficulty.valueOf(entity.difficulty) }
                .getOrDefault(EkdromeDifficulty.MEDIUM)
            val waypoints = if (entity.waypoints.isBlank()) emptyList()
            else entity.waypoints.split(",").map { it.trim() }

            val stableId = if (entity.routeKey.startsWith("builtin_")) {
                entity.routeKey.removePrefix("builtin_").toIntOrNull() ?: entity.routeKey.hashCode()
            } else {
                entity.routeKey.hashCode()
            }

            return EkdromeRoute(
                id = stableId,
                nameEn = entity.nameEn,
                nameEl = entity.nameEl,
                region = region,
                tags = tags,
                difficulty = difficulty,
                distanceKm = entity.distanceKm,
                rating = entity.rating,
                descriptionEn = entity.descriptionEn,
                descriptionEl = entity.descriptionEl,
                latitude = entity.latitude,
                longitude = entity.longitude,
                startLocation = entity.startLocation,
                endLocation = entity.endLocation,
                waypoints = waypoints,
                firestoreId = entity.firestoreId,
                reviewCount = entity.reviewCount
            )
        }
    }
}
