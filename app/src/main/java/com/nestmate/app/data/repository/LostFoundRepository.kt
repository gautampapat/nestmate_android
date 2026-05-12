package com.nestmate.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nestmate.app.data.model.LostFoundCategory
import com.nestmate.app.data.model.LostFoundItem
import com.nestmate.app.data.model.LostFoundStatus
import com.nestmate.app.data.model.LostFoundType
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LostFoundRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val collection = firestore.collection(FirebaseConstants.COLLECTION_LOST_FOUND)

    suspend fun postItem(item: LostFoundItem): Result<String> = runCatching {
        val docRef = collection.document()
        val newItem = item.copy(id = docRef.id)
        docRef.set(newItem).await()
        docRef.id
    }

    fun getItems(
        collegeId: String,
        type: LostFoundType? = null,
        category: LostFoundCategory? = null
    ): Flow<List<LostFoundItem>> = callbackFlow {
        var query: Query = collection
            .whereEqualTo("collegeId", collegeId)
            .whereEqualTo("status", LostFoundStatus.OPEN.name)

        if (type != null) {
            query = query.whereEqualTo("type", type.name)
        }
        if (category != null) {
            query = query.whereEqualTo("category", category.name)
        }

        query = query.orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { it.toObject(LostFoundItem::class.java) }
                
                // Client-side auto-expiry logic
                val now = System.currentTimeMillis()
                val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L
                val expiredItems = items.filter { it.createdAt < now - thirtyDaysMs }
                
                if (expiredItems.isNotEmpty()) {
                    expireItems(expiredItems.map { it.id })
                }
                
                // Yield only non-expired items to the UI immediately
                val activeItems = items.filter { it.createdAt >= now - thirtyDaysMs }
                trySend(activeItems)
            }
        }
        awaitClose { listener.remove() }
    }

    private fun expireItems(itemIds: List<String>) {
        firestore.runBatch { batch ->
            itemIds.forEach { id ->
                val ref = collection.document(id)
                batch.update(ref, "status", LostFoundStatus.EXPIRED.name)
                batch.update(ref, "updatedAt", System.currentTimeMillis())
            }
        }.addOnFailureListener {
            // Log failure, not critical if it fails this time
        }
    }

    fun getMyItems(userId: String): Flow<List<LostFoundItem>> = callbackFlow {
        val query = collection
            .whereEqualTo("reportedByUserId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { it.toObject(LostFoundItem::class.java) }
                trySend(items)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun markResolved(itemId: String, resolvedWithUserId: String? = null): Result<Unit> = runCatching {
        collection.document(itemId).update(
            mapOf(
                "status" to LostFoundStatus.RESOLVED.name,
                "resolvedAt" to System.currentTimeMillis(),
                "resolvedWithUserId" to resolvedWithUserId,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    fun incrementViewCount(itemId: String) {
        val docRef = collection.document(itemId)
        docRef.update("viewCount", com.google.firebase.firestore.FieldValue.increment(1))
            .addOnFailureListener {
                // Fire and forget
            }
    }

    suspend fun submitClaim(itemId: String, claimantId: String, claimantName: String, message: String): Result<Unit> = runCatching {
        val docRef = collection.document(itemId).collection("claims").document()
        val claim = LostFoundClaim(
            claimId = docRef.id,
            itemId = itemId,
            claimantId = claimantId,
            claimantName = claimantName,
            message = message,
            createdAt = System.currentTimeMillis()
        )
        firestore.runBatch { batch ->
            batch.set(docRef, claim)
            batch.update(collection.document(itemId), "claimCount", com.google.firebase.firestore.FieldValue.increment(1))
        }.await()
    }

    suspend fun deleteItem(itemId: String): Result<Unit> = runCatching {
        collection.document(itemId).delete().await()
    }
}

data class LostFoundClaim(
    val claimId: String = "",
    val itemId: String = "",
    val claimantId: String = "",
    val claimantName: String = "",
    val message: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING"   // PENDING | ACCEPTED | REJECTED
)
