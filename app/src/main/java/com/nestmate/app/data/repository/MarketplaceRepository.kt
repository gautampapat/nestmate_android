package com.nestmate.app.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nestmate.app.data.model.ChatContextType
import com.nestmate.app.data.model.ChatMessage
import com.nestmate.app.data.model.ItemStatus
import com.nestmate.app.data.model.MarketplaceChat
import com.nestmate.app.data.model.MarketplaceFilters
import com.nestmate.app.data.model.MarketplaceItem
import com.nestmate.app.data.model.WantedPost
import com.nestmate.app.data.model.WantedStatus
import com.nestmate.app.utils.imageupload.CloudinaryUploader
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketplaceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryUploader: CloudinaryUploader,
) {

    private val itemsRef
        get() = firestore.collection("marketplace").document("root").collection("items")
    private val wantedRef
        get() = firestore.collection("marketplace").document("root").collection("wanted")
    private val chatsRef
        get() = firestore.collection("marketplace").document("root").collection("chats")

    private fun savedItemsRef(userId: String) =
        firestore.collection("users").document(userId).collection("savedItems")

    // ---- Listings ----

    suspend fun createListing(item: MarketplaceItem): Result<String> = runCatching {
        val ref = itemsRef.document()
        val now = System.currentTimeMillis()
        val toWrite = item.copy(id = ref.id, createdAt = now, updatedAt = now)
        ref.set(toWrite).await()
        ref.id
    }

    suspend fun updateListing(item: MarketplaceItem): Result<Unit> = runCatching {
        val updated = item.copy(updatedAt = System.currentTimeMillis())
        itemsRef.document(item.id).set(updated).await()
    }

    suspend fun deleteListing(itemId: String): Result<Unit> = runCatching {
        itemsRef.document(itemId).delete().await()
    }

    suspend fun markAsSold(itemId: String): Result<Unit> = runCatching {
        itemsRef.document(itemId).update(
            mapOf(
                "status" to ItemStatus.SOLD.name,
                "updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    fun getListingById(itemId: String): Flow<MarketplaceItem?> = callbackFlow {
        val reg = itemsRef.document(itemId).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err); return@addSnapshotListener
            }
            trySend(snap?.toObject(MarketplaceItem::class.java))
        }
        awaitClose { reg.remove() }
    }

    fun getListingsByIds(ids: List<String>): Flow<List<MarketplaceItem>> = callbackFlow {
        if (ids.isEmpty()) {
            trySend(emptyList()); close(); return@callbackFlow
        }
        val reg = itemsRef.whereIn("id", ids.take(30)).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err); return@addSnapshotListener
            }
            trySend(snap?.toObjects(MarketplaceItem::class.java) ?: emptyList())
        }
        awaitClose { reg.remove() }
    }

    /**
     * Server query keeps a single equality filter (status) to stay index-free. Category,
     * condition, and price-range filters are applied client-side — small dataset per college
     * makes this acceptable for Phase 1; swap in Algolia later without touching the VM.
     */
    fun getActiveListings(filters: MarketplaceFilters): Flow<List<MarketplaceItem>> = callbackFlow {
        val reg = itemsRef
            .whereEqualTo("status", ItemStatus.ACTIVE.name)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err); return@addSnapshotListener
                }
                val all = snap?.toObjects(MarketplaceItem::class.java).orEmpty()
                val filtered = all
                    .filter { item ->
                        (filters.category?.let { it == item.category } ?: true) &&
                            (filters.condition?.let { it == item.condition } ?: true) &&
                            (filters.minPrice?.let { item.price >= it } ?: true) &&
                            (filters.maxPrice?.let { item.price <= it } ?: true)
                    }
                    .sortedByDescending { it.createdAt }
                trySend(filtered)
            }
        awaitClose { reg.remove() }
    }

    fun getListingsBySeller(sellerId: String): Flow<List<MarketplaceItem>> = callbackFlow {
        val reg = itemsRef
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err); return@addSnapshotListener
                }
                val list = snap?.toObjects(MarketplaceItem::class.java).orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun incrementViewCount(itemId: String) {
        itemsRef.document(itemId).update("viewCount", FieldValue.increment(1))
    }

    // ---- Save / bookmark ----

    suspend fun saveItem(userId: String, itemId: String): Result<Unit> = runCatching {
        savedItemsRef(userId).document(itemId)
            .set(mapOf("savedAt" to System.currentTimeMillis())).await()
        itemsRef.document(itemId).update("saveCount", FieldValue.increment(1))
    }

    suspend fun unsaveItem(userId: String, itemId: String): Result<Unit> = runCatching {
        savedItemsRef(userId).document(itemId).delete().await()
        itemsRef.document(itemId).update("saveCount", FieldValue.increment(-1))
    }

    fun getSavedItemIds(userId: String): Flow<Set<String>> = callbackFlow {
        val reg = savedItemsRef(userId).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err); return@addSnapshotListener
            }
            trySend(snap?.documents?.map { it.id }?.toSet() ?: emptySet())
        }
        awaitClose { reg.remove() }
    }

    // ---- Photo upload ----

    suspend fun uploadPhoto(
        userId: String,
        itemId: String,
        index: Int,
        bytes: ByteArray,
    ): Result<String> = cloudinaryUploader.uploadCompressed(
        bytes = bytes,
        folderHint = "nestmate/marketplace/$userId",
    )

    // ---- Wanted posts ----

    suspend fun createWantedPost(post: WantedPost): Result<String> = runCatching {
        val ref = wantedRef.document()
        val toWrite = post.copy(id = ref.id, createdAt = System.currentTimeMillis())
        ref.set(toWrite).await()
        ref.id
    }

    suspend fun markWantedFulfilled(postId: String): Result<Unit> = runCatching {
        wantedRef.document(postId).update("status", WantedStatus.FULFILLED.name).await()
    }

    fun getWantedPosts(): Flow<List<WantedPost>> = callbackFlow {
        val reg = wantedRef
            .whereEqualTo("status", WantedStatus.OPEN.name)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err); return@addSnapshotListener
                }
                val list = snap?.toObjects(WantedPost::class.java).orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // ---- Chat ----

    suspend fun getOrCreateItemChat(
        itemId: String,
        itemTitle: String,
        buyerId: String,
        sellerId: String,
    ): Result<String> = runCatching {
        val chatId = "${itemId}_${buyerId}_${sellerId}"
        val ref = chatsRef.document(chatId)
        val existing = ref.get().await()
        if (!existing.exists()) {
            val chat = MarketplaceChat(
                id = chatId,
                contextType = ChatContextType.ITEM,
                itemId = itemId,
                itemTitle = itemTitle,
                buyerId = buyerId,
                sellerId = sellerId,
                lastMessage = "",
                lastMessageAt = System.currentTimeMillis(),
            )
            ref.set(chat).await()
        }
        chatId
    }

    suspend fun getOrCreateWantedChat(
        wantedPostId: String,
        itemDescription: String,
        buyerId: String,
        responderId: String,
    ): Result<String> = runCatching {
        val chatId = "wanted_${wantedPostId}_${buyerId}_${responderId}"
        val ref = chatsRef.document(chatId)
        val existing = ref.get().await()
        if (!existing.exists()) {
            val chat = MarketplaceChat(
                id = chatId,
                contextType = ChatContextType.WANTED,
                wantedPostId = wantedPostId,
                itemTitle = itemDescription,
                buyerId = buyerId,
                sellerId = responderId,
                lastMessage = "",
                lastMessageAt = System.currentTimeMillis(),
            )
            ref.set(chat).await()
        }
        chatId
    }

    fun getChatThreads(userId: String): Flow<List<MarketplaceChat>> = callbackFlow {
        // Two separate single-equality queries — no composite index needed. Sort + merge client-side.
        val buyerQuery = chatsRef.whereEqualTo("buyerId", userId)
        val sellerQuery = chatsRef.whereEqualTo("sellerId", userId)

        val combined = mutableMapOf<String, MarketplaceChat>()
        fun emit() {
            trySend(combined.values.sortedByDescending { it.lastMessageAt })
        }

        val r1 = buyerQuery.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            snap?.toObjects(MarketplaceChat::class.java)?.forEach { combined[it.id] = it }
            emit()
        }
        val r2 = sellerQuery.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            snap?.toObjects(MarketplaceChat::class.java)?.forEach { combined[it.id] = it }
            emit()
        }
        awaitClose { r1.remove(); r2.remove() }
    }

    fun getChatMessages(
        chatId: String,
        limit: Int = 30,
        before: DocumentSnapshot? = null,
    ): Flow<List<ChatMessage>> = callbackFlow {
        val base = chatsRef.document(chatId).collection("messages")
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        val query = if (before != null) base.startAfter(before) else base
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObjects(ChatMessage::class.java) ?: emptyList())
        }
        awaitClose { reg.remove() }
    }

    suspend fun sendMessage(chatId: String, message: ChatMessage): Result<Unit> = runCatching {
        val ref = chatsRef.document(chatId).collection("messages").document()
        val toWrite = message.copy(id = ref.id, sentAt = System.currentTimeMillis())
        ref.set(toWrite).await()
        chatsRef.document(chatId).update(
            mapOf(
                "lastMessage" to toWrite.text,
                "lastMessageAt" to toWrite.sentAt,
            ),
        ).await()
    }

    suspend fun markChatRead(chatId: String, userId: String): Result<Unit> = runCatching {
        chatsRef.document(chatId).update(
            "lastReadBy.$userId", System.currentTimeMillis(),
        ).await()
    }

}
