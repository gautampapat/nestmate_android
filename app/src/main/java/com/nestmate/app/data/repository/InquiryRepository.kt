package com.nestmate.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nestmate.app.data.model.ChatMessage
import com.nestmate.app.data.model.Inquiry
import com.nestmate.app.data.model.InquiryStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InquiryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val inquiriesCol get() = firestore.collection("inquiries")
    private val chatsCol get() = firestore.collection("inquiryChats")

    companion object {
        private const val TAG = "InquiryRepository"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Student writes
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun submitInquiry(inquiry: Inquiry): Result<String> = runCatching {
        val ref = inquiriesCol.document()
        val toWrite = inquiry.copy(id = ref.id, createdAt = System.currentTimeMillis())
        ref.set(toWrite).await()
        ref.id
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Provider reads
    // ─────────────────────────────────────────────────────────────────────────

    /** Real-time flow of all inquiries directed at this provider. Unread first. */
    fun getInquiriesForProvider(providerId: String): Flow<List<Inquiry>> = callbackFlow {
        val reg = inquiriesCol
            .whereEqualTo("providerId", providerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "getInquiriesForProvider failed: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(Inquiry::class.java).orEmpty()
                    .sortedWith(compareBy({ !it.isUnread }, { -it.createdAt }))
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun markAsRead(inquiryId: String): Result<Unit> = runCatching {
        inquiriesCol.document(inquiryId).update("status", InquiryStatus.READ.name).await()
    }

    suspend fun markAsResponded(inquiryId: String): Result<Unit> = runCatching {
        inquiriesCol.document(inquiryId).update("status", InquiryStatus.RESPONDED.name).await()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chat (both provider and student)
    // ─────────────────────────────────────────────────────────────────────────

    /** Creates the chat document if it doesn't exist and returns the chatId. */
    suspend fun getOrCreateChat(
        inquiryId: String,
        providerId: String,
        studentId: String,
    ): Result<String> = runCatching {
        val chatId = "inquiry_${inquiryId}"
        val ref = chatsCol.document(chatId)
        val existing = ref.get().await()
        if (!existing.exists()) {
            ref.set(
                mapOf(
                    "id" to chatId,
                    "inquiryId" to inquiryId,
                    "providerId" to providerId,
                    "studentId" to studentId,
                    "createdAt" to System.currentTimeMillis(),
                ),
            ).await()
        }
        // Write chatId back to the inquiry doc so both sides can find the thread
        inquiriesCol.document(inquiryId).update("chatId", chatId).await()
        chatId
    }

    fun getChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val reg = chatsCol.document(chatId).collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "getChatMessages failed: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects(ChatMessage::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendMessage(chatId: String, message: ChatMessage): Result<Unit> = runCatching {
        val ref = chatsCol.document(chatId).collection("messages").document()
        val toWrite = message.copy(id = ref.id, sentAt = System.currentTimeMillis())
        ref.set(toWrite).await()
    }

    suspend fun getInquiryById(inquiryId: String): Result<Inquiry?> = runCatching {
        inquiriesCol.document(inquiryId).get().await().toObject(Inquiry::class.java)
    }
}
