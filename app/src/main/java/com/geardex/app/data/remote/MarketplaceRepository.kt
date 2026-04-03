package com.geardex.app.data.remote

import android.util.Log
import com.geardex.app.data.model.MarketplacePart
import com.geardex.app.data.model.PartCategory
import com.geardex.app.data.model.PartCondition
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MarketplaceRepo"
private const val COLLECTION = "marketplace_parts"

@Singleton
class MarketplaceRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {
    private val partsCollection
        get() = firebaseManager.firestore?.collection(COLLECTION)

    fun observeParts(): Flow<List<MarketplacePart>> {
        val collection = partsCollection ?: return flowOf(emptyList())
        return callbackFlow {
            val registration = collection
                .whereEqualTo("isAvailable", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listen failed", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val parts = snapshot?.documents?.mapNotNull { doc ->
                        docToPart(doc.id, doc.data)
                    } ?: emptyList()
                    trySend(parts)
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun submitPart(part: MarketplacePart): Boolean {
        val collection = partsCollection ?: return false
        val user = firebaseManager.currentUser
        val data = mapOf(
            "partName" to part.partName,
            "category" to part.category.name,
            "vehicleMake" to part.vehicleMake,
            "vehicleModel" to part.vehicleModel,
            "vehicleYear" to part.vehicleYear,
            "price" to part.price,
            "condition" to part.condition.name,
            "description" to part.description,
            "sellerName" to (user?.email?.substringBefore("@") ?: "anonymous"),
            "sellerUid" to (user?.uid ?: ""),
            "contactInfo" to part.contactInfo,
            "region" to part.region,
            "createdAt" to System.currentTimeMillis(),
            "isAvailable" to true
        )
        return runCatching {
            collection.add(data).await()
        }.onFailure {
            Log.e(TAG, "Failed to submit part", it)
        }.isSuccess
    }

    private fun docToPart(docId: String, data: Map<String, Any>?): MarketplacePart? {
        if (data == null) return null
        return runCatching {
            val category = runCatching {
                PartCategory.valueOf(data["category"] as? String ?: "OTHER")
            }.getOrDefault(PartCategory.OTHER)
            val condition = runCatching {
                PartCondition.valueOf(data["condition"] as? String ?: "USED_GOOD")
            }.getOrDefault(PartCondition.USED_GOOD)

            MarketplacePart(
                id = docId,
                partName = data["partName"] as? String ?: "",
                category = category,
                vehicleMake = data["vehicleMake"] as? String ?: "",
                vehicleModel = data["vehicleModel"] as? String ?: "",
                vehicleYear = (data["vehicleYear"] as? Long)?.toInt() ?: 0,
                price = data["price"] as? Double ?: 0.0,
                condition = condition,
                description = data["description"] as? String ?: "",
                sellerName = data["sellerName"] as? String ?: "",
                sellerUid = data["sellerUid"] as? String ?: "",
                contactInfo = data["contactInfo"] as? String ?: "",
                region = data["region"] as? String ?: "",
                createdAt = data["createdAt"] as? Long ?: 0L,
                isAvailable = data["isAvailable"] as? Boolean ?: true
            )
        }.onFailure {
            Log.e(TAG, "Failed to parse part $docId", it)
        }.getOrNull()
    }
}
