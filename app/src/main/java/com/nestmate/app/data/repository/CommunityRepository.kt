package com.nestmate.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.model.Event
import com.nestmate.app.data.model.ForumComment
import com.nestmate.app.data.model.ForumPost
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CommunityRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Real-time listener for forum posts. Emits immediately when any doc in the
     * forums collection changes — so new posts appear instantly without manual refresh.
     */
    fun getForumPostsFlow(): Flow<List<ForumPost>> = callbackFlow {
        val listener = firestore.collection(FirebaseConstants.COLLECTION_FORUMS)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents
                        .mapNotNull { it.toObject(ForumPost::class.java) }
                        .sortedByDescending { it.timestamp }
                    trySend(posts)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getEvents(): Result<List<Event>> {
        return try {
            val snapshot = firestore.collection(FirebaseConstants.COLLECTION_EVENTS)
                .get()
                .await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(Event::class.java) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostDetails(postId: String): Result<ForumPost?> {
        return try {
            val doc = firestore.collection(FirebaseConstants.COLLECTION_FORUMS)
                .document(postId).get().await()
            Result.success(doc.toObject(ForumPost::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(postId: String): Result<List<ForumComment>> {
        return try {
            val snapshot = firestore.collection(FirebaseConstants.COLLECTION_FORUMS)
                .document(postId)
                .collection("comments")
                .get()
                .await()
            val comments = snapshot.documents
                .mapNotNull { it.toObject(ForumComment::class.java) }
                .sortedBy { it.timestamp }
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPost(post: ForumPost): Result<Unit> {
        return try {
            val docRef = firestore.collection(FirebaseConstants.COLLECTION_FORUMS).document()
            docRef.set(post.copy(postId = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upvotePost(postId: String): Result<Unit> {
        return try {
            firestore.collection(FirebaseConstants.COLLECTION_FORUMS)
                .document(postId)
                .update("upvotes", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
