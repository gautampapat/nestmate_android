package com.nestmate.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.nestmate.app.data.model.Restaurant
import com.nestmate.app.data.model.RestaurantRating
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestaurantRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val restaurantsRef = firestore.collection("restaurants")
    private val ratingsRef = firestore.collection("restaurantRatings")

    fun getAllRestaurants(): Flow<List<Restaurant>> = callbackFlow {
        val listener = restaurantsRef.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val list = snap?.documents?.mapNotNull { it.toObject(Restaurant::class.java)?.copy(id = it.id) } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun getRestaurantById(id: String): Flow<Restaurant?> = callbackFlow {
        val listener = restaurantsRef.document(id).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(Restaurant::class.java)?.copy(id = snap.id))
        }
        awaitClose { listener.remove() }
    }

    suspend fun submitRating(rating: RestaurantRating): Result<Unit> = runCatching {
        val ratingId = "${rating.userId}_${rating.restaurantId}"
        val ratingRef = ratingsRef.document(ratingId)
        val restaurantRef = restaurantsRef.document(rating.restaurantId)

        firestore.runTransaction { tx ->
            val snap = tx.get(restaurantRef)
            val currentRating = snap.getDouble("overallRating")?.toFloat() ?: 0f
            val currentCount = snap.getLong("ratingCount")?.toInt() ?: 0

            // Check if user already rated — if so, remove old rating from average
            val existingSnap = tx.get(ratingRef)
            val newCount: Int
            val newRating: Float
            if (existingSnap.exists()) {
                val oldRating = existingSnap.getDouble("rating")?.toFloat() ?: 0f
                val totalWithoutOld = currentRating * currentCount - oldRating
                newCount = currentCount
                newRating = if (newCount > 0) (totalWithoutOld + rating.rating) / newCount else rating.rating
            } else {
                newCount = currentCount + 1
                newRating = (currentRating * currentCount + rating.rating) / newCount
            }

            tx.set(ratingRef, rating.copy(id = ratingId))
            tx.update(restaurantRef, mapOf(
                "overallRating" to newRating,
                "ratingCount" to newCount,
            ))
        }.await()
    }

    fun getUserRatingForRestaurant(userId: String, restaurantId: String): Flow<RestaurantRating?> = callbackFlow {
        val ratingId = "${userId}_${restaurantId}"
        val listener = ratingsRef.document(ratingId).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(RestaurantRating::class.java))
        }
        awaitClose { listener.remove() }
    }

    fun logVisit(restaurantId: String) {
        restaurantsRef.document(restaurantId).update("visitCount", FieldValue.increment(1))
    }

    suspend fun saveRestaurant(userId: String, restaurantId: String): Result<Unit> = runCatching {
        firestore.collection("users").document(userId)
            .collection("savedRestaurants").document(restaurantId)
            .set(mapOf("savedAt" to System.currentTimeMillis())).await()
    }

    suspend fun unsaveRestaurant(userId: String, restaurantId: String): Result<Unit> = runCatching {
        firestore.collection("users").document(userId)
            .collection("savedRestaurants").document(restaurantId)
            .delete().await()
    }

    fun getSavedRestaurantIds(userId: String): Flow<Set<String>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("savedRestaurants")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.map { it.id }?.toSet() ?: emptySet())
            }
        awaitClose { listener.remove() }
    }
}
