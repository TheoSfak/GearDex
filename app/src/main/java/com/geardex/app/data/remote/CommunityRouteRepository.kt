package com.geardex.app.data.remote

import android.util.Log
import com.geardex.app.data.model.EkdromeDifficulty
import com.geardex.app.data.model.EkdromeRegion
import com.geardex.app.data.model.EkdromeRoute
import com.geardex.app.data.model.EkdromeTag
import com.geardex.app.data.model.RouteReview
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CommunityRoutes"
private const val COLLECTION = "community_routes"
private const val REVIEWS_COLLECTION = "route_reviews"

@Singleton
class CommunityRouteRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {

    private val routesCollection
        get() = firebaseManager.firestore?.collection(COLLECTION)

    private val reviewsRoot
        get() = firebaseManager.firestore?.collection(REVIEWS_COLLECTION)

    fun observeRoutes(): Flow<List<EkdromeRoute>> {
        val collection = routesCollection ?: return flowOf(emptyList())
        return callbackFlow {
            val registration = collection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listen failed", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val routes = snapshot?.documents?.mapNotNull { doc ->
                        docToRoute(doc.id, doc.data)
                    } ?: emptyList()
                    trySend(routes)
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun submitRoute(
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
    ): Boolean {
        val collection = routesCollection ?: return false
        val user = firebaseManager.currentUser
        val data = mapOf(
            "nameEn" to nameEn,
            "nameEl" to nameEl,
            "region" to region.name,
            "tags" to tags.map { it.name },
            "difficulty" to difficulty.name,
            "distanceKm" to distanceKm,
            "descriptionEn" to descriptionEn,
            "descriptionEl" to descriptionEl,
            "latitude" to latitude,
            "longitude" to longitude,
            "startLocation" to startLocation,
            "endLocation" to endLocation,
            "waypoints" to waypoints,
            "submittedBy" to (user?.email ?: "anonymous"),
            "submittedByUid" to (user?.uid ?: ""),
            "createdAt" to System.currentTimeMillis(),
            "rating" to 0.0,
            "reviewCount" to 0
        )
        return runCatching {
            collection.add(data).await()
        }.onFailure {
            Log.e(TAG, "Failed to submit route", it)
        }.isSuccess
    }

    fun observeReviews(routeId: String): Flow<List<RouteReview>> {
        val root = reviewsRoot ?: return flowOf(emptyList())
        return callbackFlow {
            val registration = root.document(routeId).collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Reviews listen failed", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val reviews = snapshot?.documents?.mapNotNull { doc ->
                        docToReview(doc.id, doc.data)
                    } ?: emptyList()
                    trySend(reviews)
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun submitReview(
        routeId: String,
        rating: Float,
        comment: String
    ): Boolean {
        val root = reviewsRoot ?: return false
        val user = firebaseManager.currentUser
        val data = mapOf(
            "routeId" to routeId,
            "rating" to rating.toDouble(),
            "comment" to comment,
            "userName" to (user?.email?.substringBefore("@") ?: "anonymous"),
            "userUid" to (user?.uid ?: ""),
            "createdAt" to System.currentTimeMillis()
        )
        return runCatching {
            root.document(routeId).collection("reviews").add(data).await()
        }.onFailure {
            Log.e(TAG, "Failed to submit review", it)
        }.isSuccess
    }

    private fun docToRoute(docId: String, data: Map<String, Any>?): EkdromeRoute? {
        if (data == null) return null
        return runCatching {
            val region = runCatching {
                EkdromeRegion.valueOf(data["region"] as? String ?: "CRETE")
            }.getOrDefault(EkdromeRegion.CRETE)

            val tags = (data["tags"] as? List<*>)?.mapNotNull { tagStr ->
                runCatching { EkdromeTag.valueOf(tagStr as String) }.getOrNull()
            } ?: emptyList()

            val difficulty = runCatching {
                EkdromeDifficulty.valueOf(data["difficulty"] as? String ?: "MEDIUM")
            }.getOrDefault(EkdromeDifficulty.MEDIUM)

            val waypoints = (data["waypoints"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            EkdromeRoute(
                id = docId.hashCode(),
                nameEn = data["nameEn"] as? String ?: "",
                nameEl = data["nameEl"] as? String ?: "",
                region = region,
                tags = tags,
                difficulty = difficulty,
                distanceKm = (data["distanceKm"] as? Long)?.toInt() ?: 0,
                rating = (data["rating"] as? Double)?.toFloat() ?: 0f,
                descriptionEn = data["descriptionEn"] as? String ?: "",
                descriptionEl = data["descriptionEl"] as? String ?: "",
                latitude = data["latitude"] as? Double ?: 0.0,
                longitude = data["longitude"] as? Double ?: 0.0,
                startLocation = data["startLocation"] as? String ?: "",
                endLocation = data["endLocation"] as? String ?: "",
                waypoints = waypoints,
                firestoreId = docId,
                reviewCount = (data["reviewCount"] as? Long)?.toInt() ?: 0
            )
        }.onFailure {
            Log.e(TAG, "Failed to parse community route $docId", it)
        }.getOrNull()
    }

    private fun docToReview(docId: String, data: Map<String, Any>?): RouteReview? {
        if (data == null) return null
        return runCatching {
            RouteReview(
                id = docId,
                routeId = data["routeId"] as? String ?: "",
                rating = (data["rating"] as? Double)?.toFloat() ?: 0f,
                comment = data["comment"] as? String ?: "",
                userName = data["userName"] as? String ?: "",
                userUid = data["userUid"] as? String ?: "",
                createdAt = data["createdAt"] as? Long ?: 0L
            )
        }.getOrNull()
    }
}
