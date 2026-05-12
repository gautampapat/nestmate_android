package com.nestmate.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.model.ListingStatus
import com.nestmate.app.data.model.ListingType
import com.nestmate.app.data.model.ProviderListing
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val col get() = firestore.collection(COLLECTION)

    companion object {
        const val COLLECTION = "providerListings"
        private const val TAG = "ProviderRepository"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Provider (write) operations
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun createListing(listing: ProviderListing): Result<String> = runCatching {
        val ref = col.document()
        val now = System.currentTimeMillis()
        val toWrite = listing.copy(id = ref.id, createdAt = now, updatedAt = now)
        ref.set(toWrite).await()
        ref.id
    }

    suspend fun updateListing(listing: ProviderListing): Result<Unit> = runCatching {
        val updated = listing.copy(updatedAt = System.currentTimeMillis())
        col.document(listing.id).set(updated).await()
    }

    /** Soft delete — sets status to DELETED so history is preserved. */
    suspend fun deleteListing(listingId: String): Result<Unit> = runCatching {
        col.document(listingId).update(
            mapOf(
                "status" to ListingStatus.DELETED.name,
                "updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    /** Toggles between ACTIVE and PAUSED. Returns the new status string. */
    suspend fun togglePause(listingId: String, currentStatus: String): Result<String> = runCatching {
        val newStatus = if (currentStatus == ListingStatus.ACTIVE.name)
            ListingStatus.PAUSED.name else ListingStatus.ACTIVE.name
        col.document(listingId).update(
            mapOf("status" to newStatus, "updatedAt" to System.currentTimeMillis()),
        ).await()
        newStatus
    }

    /** Real-time flow of provider's own listings (excluding DELETED).
     *  Emits empty list on permission errors instead of crashing — rules may not be
     *  deployed yet; the dashboard will show an empty state gracefully. */
    fun getMyListings(ownerId: String): Flow<List<ProviderListing>> = callbackFlow {
        val reg = col
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "getMyListings failed: ${err.message}")
                    trySend(emptyList()) // ← graceful: don't crash, show empty state
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(ProviderListing::class.java).orEmpty()
                    .filter { it.status != ListingStatus.DELETED.name }
                    .sortedByDescending { it.updatedAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Student browse queries
    // ─────────────────────────────────────────────────────────────────────────

    /** Active housing listings for student Housing screen. */
    fun getHousingListings(collegeId: String): Flow<List<ProviderListing>> = callbackFlow {
        val reg = col
            .whereEqualTo("listingType", ListingType.FLAT_PG_HOSTEL.name)
            .whereEqualTo("status", ListingStatus.ACTIVE.name)
            .whereEqualTo("collegeId", collegeId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "getHousingListings failed: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(ProviderListing::class.java).orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Active mess listings for student Mess screen. */
    fun getMessListings(collegeId: String): Flow<List<ProviderListing>> = callbackFlow {
        val reg = col
            .whereEqualTo("listingType", ListingType.MESS.name)
            .whereEqualTo("status", ListingStatus.ACTIVE.name)
            .whereEqualTo("collegeId", collegeId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "getMessListings failed: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(ProviderListing::class.java).orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Active service listings for student Services Hub. */
    fun getServiceListings(collegeId: String): Flow<List<ProviderListing>> = callbackFlow {
        val serviceTypes = listOf(
            ListingType.LAUNDRY.name,
            ListingType.XEROX.name,
            ListingType.GROCERY.name,
            ListingType.TUTOR.name,
            ListingType.OTHER_SERVICE.name,
        )
        val reg = col
            .whereIn("listingType", serviceTypes)
            .whereEqualTo("status", ListingStatus.ACTIVE.name)
            .whereEqualTo("collegeId", collegeId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "getServiceListings failed: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(ProviderListing::class.java).orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getListingById(listingId: String): Result<ProviderListing?> = runCatching {
        col.document(listingId).get().await().toObject(ProviderListing::class.java)
    }

    /** Fire-and-forget view count increment. */
    fun incrementViewCount(listingId: String) {
        col.document(listingId).update("viewCount", FieldValue.increment(1))
    }

    fun incrementInquiryCount(listingId: String) {
        col.document(listingId).update("inquiryCount", FieldValue.increment(1))
    }
}
