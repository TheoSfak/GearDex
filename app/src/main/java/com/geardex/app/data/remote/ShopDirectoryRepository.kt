package com.geardex.app.data.remote

import android.util.Log
import com.geardex.app.data.model.ServiceShop
import com.geardex.app.data.model.ShopCategory
import com.geardex.app.data.model.ShopReview
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShopDirectory"
private const val SHOPS_COLLECTION = "service_shops"
private const val REVIEWS_COLLECTION = "shop_reviews"

@Singleton
class ShopDirectoryRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {
    private val shopsCollection
        get() = firebaseManager.firestore?.collection(SHOPS_COLLECTION)

    private val reviewsRoot
        get() = firebaseManager.firestore?.collection(REVIEWS_COLLECTION)

    fun observeShops(): Flow<List<ServiceShop>> {
        val collection = shopsCollection ?: return flowOf(emptyList())
        return callbackFlow {
            val registration = collection
                .orderBy("rating", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listen failed", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val shops = snapshot?.documents?.mapNotNull { doc ->
                        docToShop(doc.id, doc.data)
                    } ?: emptyList()
                    trySend(shops)
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun submitShop(shop: ServiceShop): Boolean {
        val collection = shopsCollection ?: return false
        val user = firebaseManager.currentUser
        val data = mapOf(
            "nameEn" to shop.nameEn,
            "nameEl" to shop.nameEl,
            "category" to shop.category.name,
            "region" to shop.region,
            "address" to shop.address,
            "phone" to shop.phone,
            "rating" to 0.0,
            "reviewCount" to 0,
            "latitude" to shop.latitude,
            "longitude" to shop.longitude,
            "submittedBy" to (user?.email ?: "anonymous"),
            "submittedByUid" to (user?.uid ?: ""),
            "createdAt" to System.currentTimeMillis()
        )
        return runCatching {
            collection.add(data).await()
        }.onFailure {
            Log.e(TAG, "Failed to submit shop", it)
        }.isSuccess
    }

    fun observeReviews(shopId: String): Flow<List<ShopReview>> {
        val root = reviewsRoot ?: return flowOf(emptyList())
        return callbackFlow {
            val registration = root.document(shopId).collection("reviews")
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

    suspend fun submitReview(shopId: String, rating: Float, comment: String): Boolean {
        val root = reviewsRoot ?: return false
        val user = firebaseManager.currentUser
        val data = mapOf(
            "shopId" to shopId,
            "rating" to rating.toDouble(),
            "comment" to comment,
            "userName" to (user?.email?.substringBefore("@") ?: "anonymous"),
            "userUid" to (user?.uid ?: ""),
            "createdAt" to System.currentTimeMillis()
        )
        return runCatching {
            root.document(shopId).collection("reviews").add(data).await()
        }.onFailure {
            Log.e(TAG, "Failed to submit review", it)
        }.isSuccess
    }

    private fun docToShop(docId: String, data: Map<String, Any>?): ServiceShop? {
        if (data == null) return null
        return runCatching {
            val category = runCatching {
                ShopCategory.valueOf(data["category"] as? String ?: "GENERAL")
            }.getOrDefault(ShopCategory.GENERAL)
            ServiceShop(
                id = docId,
                nameEn = data["nameEn"] as? String ?: "",
                nameEl = data["nameEl"] as? String ?: "",
                category = category,
                region = data["region"] as? String ?: "",
                address = data["address"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                rating = (data["rating"] as? Double)?.toFloat() ?: 0f,
                reviewCount = (data["reviewCount"] as? Long)?.toInt() ?: 0,
                latitude = data["latitude"] as? Double ?: 0.0,
                longitude = data["longitude"] as? Double ?: 0.0,
                submittedBy = data["submittedBy"] as? String ?: "",
                submittedByUid = data["submittedByUid"] as? String ?: "",
                createdAt = data["createdAt"] as? Long ?: 0L
            )
        }.onFailure {
            Log.e(TAG, "Failed to parse shop $docId", it)
        }.getOrNull()
    }

    private fun docToReview(docId: String, data: Map<String, Any>?): ShopReview? {
        if (data == null) return null
        return runCatching {
            ShopReview(
                id = docId,
                shopId = data["shopId"] as? String ?: "",
                rating = (data["rating"] as? Double)?.toFloat() ?: 0f,
                comment = data["comment"] as? String ?: "",
                userName = data["userName"] as? String ?: "",
                userUid = data["userUid"] as? String ?: "",
                createdAt = data["createdAt"] as? Long ?: 0L
            )
        }.getOrNull()
    }
}
