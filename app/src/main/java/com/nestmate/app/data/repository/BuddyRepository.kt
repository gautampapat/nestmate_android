package com.nestmate.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nestmate.app.data.model.BuddyMessage
import com.nestmate.app.data.model.BuddyPair
import com.nestmate.app.data.model.User
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BuddyRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getSeniors(collegeId: String): Result<List<User>> {
        return try {
            val snapshot = firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .whereEqualTo("collegeId", collegeId)
                .whereEqualTo("isSenior", true)
                .get()
                .await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(User::class.java) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestBuddy(juniorId: String, seniorId: String, collegeId: String): Result<Unit> {
        return try {
            val ref = firestore.collection(FirebaseConstants.COLLECTION_BUDDY_PAIRS).document()
            val pair = BuddyPair(
                pairId = ref.id,
                seniorId = seniorId,
                juniorId = juniorId,
                collegeId = collegeId
            )
            ref.set(pair).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivePairsForUser(userId: String): Result<List<BuddyPair>> {
        return try {
            // Firestore requires compound queries to be indexed, doing simple local filter for MVP
            val seniorPairs = firestore.collection(FirebaseConstants.COLLECTION_BUDDY_PAIRS)
                .whereEqualTo("seniorId", userId)
                .get().await().documents.mapNotNull { it.toObject(BuddyPair::class.java) }
            
            val juniorPairs = firestore.collection(FirebaseConstants.COLLECTION_BUDDY_PAIRS)
                .whereEqualTo("juniorId", userId)
                .get().await().documents.mapNotNull { it.toObject(BuddyPair::class.java) }

            Result.success(seniorPairs + juniorPairs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMessages(pairId: String): Flow<List<BuddyMessage>> = callbackFlow {
        val listener = firestore.collection(FirebaseConstants.COLLECTION_BUDDY_PAIRS)
            .document(pairId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(BuddyMessage::class.java) }
                    trySend(messages)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(pairId: String, senderId: String, text: String): Result<Unit> {
        return try {
            val ref = firestore.collection(FirebaseConstants.COLLECTION_BUDDY_PAIRS)
                .document(pairId)
                .collection("messages")
                .document()
            
            val message = BuddyMessage(
                messageId = ref.id,
                senderId = senderId,
                text = text
            )
            ref.set(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
