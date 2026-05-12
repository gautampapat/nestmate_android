package com.nestmate.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserActivityRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun touchLastActive(uid: String): Result<Unit> {
        return try {
            firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .document(uid)
                .set(mapOf("lastActiveAt" to System.currentTimeMillis()), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
