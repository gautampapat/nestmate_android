package com.nestmate.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.model.DailyService
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ServicesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun getDailyServices(type: String? = null): Result<List<DailyService>> {
        return try {
            val snapshot = firestore.collection(FirebaseConstants.COLLECTION_SERVICES)
                .get()
                .await()

            var services = snapshot.documents.mapNotNull { it.toObject(DailyService::class.java) }

            if (type != null && type != "All") {
                services = services.filter { it.type == type }
            }

            services = services.sortedByDescending { it.rating }
            Result.success(services)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
