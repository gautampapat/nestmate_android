package com.nestmate.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.nestmate.app.data.model.ChatMessage
import com.nestmate.app.data.model.ConnectionStatus
import com.nestmate.app.data.model.GroupStatus
import com.nestmate.app.data.model.RoommateConnection
import com.nestmate.app.data.model.RoommateGroupListing
import com.nestmate.app.data.model.RoommateProfile
import com.nestmate.app.utils.imageupload.CloudinaryUploader
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoommateRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryUploader: CloudinaryUploader,
) {

    private val profilesRef
        get() = firestore.collection("roommate").document("root").collection("profiles")
    private val groupsRef
        get() = firestore.collection("roommate").document("root").collection("groups")
    private val chatsRef
        get() = firestore.collection("roommate").document("root").collection("chats")

    // ---- Profiles ----

    suspend fun saveProfile(profile: RoommateProfile): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val toWrite = profile.copy(
            lastActiveAt = now,
            updatedAt = now,
            createdAt = if (profile.createdAt == 0L) now else profile.createdAt
        )
        profilesRef.document(profile.userId).set(toWrite).await()
    }

    fun getProfile(userId: String): Flow<RoommateProfile?> = callbackFlow {
        val reg = profilesRef.document(userId).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(RoommateProfile::class.java))
        }
        awaitClose { reg.remove() }
    }

    fun getAllProfiles(excludeUserId: String): Flow<List<RoommateProfile>> = callbackFlow {
        val reg = profilesRef
            .whereEqualTo("isActivelySearching", true)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.toObjects(RoommateProfile::class.java).orEmpty()
                    .filter { it.userId.isNotBlank() && it.userId != excludeUserId }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun uploadPhoto(userId: String, bytes: ByteArray): Result<String> =
        cloudinaryUploader.uploadCompressed(
            bytes = bytes,
            folderHint = "nestmate/profiles/$userId",
        )

    // Self-write only — rules block cross-profile writes. `RoommateViewModel.buildScored`
    // already filters both directions client-side by reading each profile's blockedUserIds,
    // so a unidirectional store is sufficient.
    suspend fun blockUser(blockerId: String, blockedId: String): Result<Unit> = runCatching {
        profilesRef.document(blockerId).set(
            mapOf("blockedUserIds" to FieldValue.arrayUnion(blockedId)),
            SetOptions.merge(),
        ).await()
    }

    suspend fun unblockUser(blockerId: String, blockedId: String): Result<Unit> = runCatching {
        profilesRef.document(blockerId).update(
            "blockedUserIds", FieldValue.arrayRemove(blockedId),
        ).await()
    }


    // ---- Groups ----

    suspend fun createGroup(group: RoommateGroupListing): Result<String> = runCatching {
        val ref = groupsRef.document()
        val toWrite = group.copy(
            id = ref.id,
            createdAt = System.currentTimeMillis(),
            memberIds = (group.memberIds + group.creatorId).distinct(),
            spotsConfirmed = 1,
        )
        ref.set(toWrite).await()
        ref.id
    }

    fun getOpenGroups(): Flow<List<RoommateGroupListing>> = callbackFlow {
        val reg = groupsRef
            .whereEqualTo("status", GroupStatus.OPEN.name)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.toObjects(RoommateGroupListing::class.java).orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun requestJoinGroup(groupId: String, userId: String): Result<Unit> = runCatching {
        groupsRef.document(groupId).update(
            "pendingRequestIds", FieldValue.arrayUnion(userId),
        ).await()
    }

    suspend fun approveJoinRequest(groupId: String, userId: String): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val ref = groupsRef.document(groupId)
            val snap = tx.get(ref)
            val current = snap.toObject(RoommateGroupListing::class.java)
                ?: error("Group not found")
            val members = (current.memberIds + userId).distinct()
            val newConfirmed = (current.spotsConfirmed + 1).coerceAtMost(current.spotsNeeded)
            val newStatus = if (newConfirmed >= current.spotsNeeded) GroupStatus.FULL else current.status
            tx.update(
                ref,
                mapOf(
                    "memberIds" to members,
                    "pendingRequestIds" to FieldValue.arrayRemove(userId),
                    "spotsConfirmed" to newConfirmed,
                    "status" to newStatus.name,
                ),
            )
        }.await()
    }

    // ---- Chat (accepted connections only) ----

    suspend fun sendChatMessage(chatId: String, message: ChatMessage): Result<Unit> = runCatching {
        val ref = chatsRef.document(chatId).collection("messages").document()
        val toWrite = message.copy(id = ref.id, sentAt = System.currentTimeMillis())
        ref.set(toWrite).await()
        chatsRef.document(chatId).set(
            mapOf(
                "lastMessage" to toWrite.text,
                "lastMessageAt" to toWrite.sentAt,
            ),
            SetOptions.merge(),
        ).await()
    }

    fun getChatMessages(chatId: String, limit: Int = 30): Flow<List<ChatMessage>> = callbackFlow {
        val reg = chatsRef.document(chatId).collection("messages")
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(ChatMessage::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }
}
