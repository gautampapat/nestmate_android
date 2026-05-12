package com.nestmate.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.model.Mess
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Real-time listener for the messes collection, filtered by collegeId.
     * Crowd levels update on all devices automatically when any vote is cast.
     */
    fun getMessesFlow(): Flow<List<Mess>> = callbackFlow {
        val listener = firestore.collection(FirebaseConstants.COLLECTION_MESSES)
            .whereEqualTo("collegeId", FirebaseConstants.DEFAULT_COLLEGE_ID)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messes = snapshot.documents
                        .mapNotNull { it.toObject(Mess::class.java) }
                        .sortedByDescending { it.rating }
                    trySend(messes)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMessDetails(messId: String): Result<Mess?> {
        return try {
            val doc = firestore.collection(FirebaseConstants.COLLECTION_MESSES)
                .document(messId)
                .get()
                .await()
            Result.success(doc.toObject(Mess::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserVote(messId: String, userId: String): String? {
        return try {
            val doc = firestore.collection(FirebaseConstants.COLLECTION_MESSES)
                .document(messId)
                .collection("votes")
                .document(userId)
                .get()
                .await()
            doc.getString("vote")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submitCrowdVote(messId: String, userId: String, vote: String): Result<Unit> {
        return try {
            val messRef = firestore.collection(FirebaseConstants.COLLECTION_MESSES).document(messId)
            val voteRef = messRef.collection("votes").document(userId)
            
            // Set the user's vote
            voteRef.set(mapOf("vote" to vote.lowercase())).await()
            
            // Update aggregate
            updateAggregateVotes(messId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeCrowdVote(messId: String, userId: String): Result<Unit> {
        return try {
            val messRef = firestore.collection(FirebaseConstants.COLLECTION_MESSES).document(messId)
            messRef.collection("votes").document(userId).delete().await()
            
            // Update aggregate
            updateAggregateVotes(messId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateAggregateVotes(messId: String) {
        val messRef = firestore.collection(FirebaseConstants.COLLECTION_MESSES).document(messId)
        val votesSnapshot = messRef.collection("votes").get().await()
        
        var low = 0
        var medium = 0
        var high = 0
        
        for (doc in votesSnapshot.documents) {
            when (doc.getString("vote")) {
                "low" -> low++
                "medium" -> medium++
                "high" -> high++
            }
        }
        
        val derivedLevel = when {
            high > medium && high > low -> "high"
            medium > low -> "medium"
            low > 0 -> "low"
            else -> "low"
        }
        
        messRef.update(
            mapOf(
                "crowdVotesLow" to low,
                "crowdVotesMedium" to medium,
                "crowdVotesHigh" to high,
                "crowdLevel" to derivedLevel
            )
        ).await()
    }

    suspend fun createOrUpdateMess(mess: Mess): Result<Unit> {
        return try {
            val messRef = firestore.collection(FirebaseConstants.COLLECTION_MESSES).document(mess.messId)
            val exists = messRef.get().await().exists()
            messRef.set(mess, com.google.firebase.firestore.SetOptions.merge()).await()
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!exists) {
                updates["createdAt"] = FieldValue.serverTimestamp()
            }
            messRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
