package com.geardex.app.data.repository

import com.geardex.app.data.local.dao.LocalRouteReviewDao
import com.geardex.app.data.local.entity.LocalRouteReview
import com.geardex.app.data.model.RouteReview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRouteReviewRepository @Inject constructor(
    private val dao: LocalRouteReviewDao
) {

    fun observeReviews(routeKey: String): Flow<List<RouteReview>> =
        dao.observeForRoute(routeKey).map { reviews ->
            reviews.map { review ->
                RouteReview(
                    id = review.id.toString(),
                    routeId = review.routeKey,
                    rating = review.rating,
                    comment = review.comment,
                    userName = "You",
                    createdAt = review.createdAt
                )
            }
        }

    suspend fun addReview(routeKey: String, rating: Float, comment: String): Long {
        return dao.insert(
            LocalRouteReview(
                routeKey = routeKey,
                rating = rating,
                comment = comment
            )
        )
    }
}
