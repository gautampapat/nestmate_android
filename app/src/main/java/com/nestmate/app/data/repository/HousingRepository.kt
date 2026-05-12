package com.nestmate.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.nestmate.app.data.local.ListingDao
import com.nestmate.app.data.local.ListingEntity
import com.nestmate.app.data.model.Listing
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class HousingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val listingDao: ListingDao
) {
    // Expose cached listings to the ViewModel seamlessly
    val cachedListings: Flow<List<Listing>> = listingDao.getAllCachedListings().map { entities ->
        entities.map { entity ->
            Listing(
                listingId = entity.listingId,
                title = entity.title,
                type = entity.type,
                rent = entity.rent,
                deposit = entity.deposit,
                address = entity.address,
                realityScore = entity.realityScore,
                greenScore = entity.greenScore,
                isBachelorFriendly = entity.isBachelorFriendly,
                isFemaleOnly = entity.isFemaleOnly,
                bhkType = entity.bhkType,
                photos = listOf(entity.photoUrl) // Placeholder recreation
            )
        }
    }

    suspend fun getListings(
        limit: Long = 10,
        lastVisible: DocumentSnapshot? = null
    ): Result<Pair<List<Listing>, DocumentSnapshot?>> {
        return try {
            var query = firestore.collection(FirebaseConstants.COLLECTION_LISTINGS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)

            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            val listings = snapshot.documents.mapNotNull { it.toObject(Listing::class.java) }
            val lastDoc = if (snapshot.documents.isNotEmpty()) snapshot.documents.last() else null

            // Cache mapping
            if (listings.isNotEmpty() && lastVisible == null) {
                val entities = listings.map {
                    ListingEntity(
                        it.listingId, it.type, it.title, it.rent, it.deposit,
                        it.address, it.realityScore, it.greenScore,
                        it.isBachelorFriendly, it.isFemaleOnly, it.bhkType,
                        it.photos.firstOrNull() ?: ""
                    )
                }
                listingDao.clearAll()
                listingDao.insertCachedListings(entities)
            }

            Result.success(Pair(listings, lastDoc))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListingDetails(listingId: String): Result<Listing?> {
        return try {
            val doc = firestore.collection(FirebaseConstants.COLLECTION_LISTINGS)
                .document(listingId)
                .get()
                .await()
            Result.success(doc.toObject(Listing::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addListing(listing: Listing): Result<Unit> {
        return try {
            val docRef = firestore.collection(FirebaseConstants.COLLECTION_LISTINGS).document()
            val finalListing = listing.copy(listingId = docRef.id)
            docRef.set(finalListing).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
