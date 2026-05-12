package com.nestmate.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.model.ConnectionStatus
import com.nestmate.app.data.model.RoommateConnection
import com.nestmate.app.data.model.User
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val connectionsRef
        get() = firestore.collection(FirebaseConstants.COLLECTION_CONNECTIONS)
    private val chatsRef
        get() = firestore.collection("roommate").document("root").collection("chats") // Reused for roommate chats

    // Send a connection request
    suspend fun sendRequest(fromUser: User, toUserId: String, note: String = ""): Result<Unit> = runCatching {
        val connectionId = RoommateConnection.canonicalId(fromUser.userId, toUserId)
        val ref = connectionsRef.document(connectionId)
        val now = System.currentTimeMillis()
        val connection = RoommateConnection(
            id = connectionId,
            requesterId = fromUser.userId,
            receiverId = toUserId,
            status = ConnectionStatus.PENDING,
            createdAt = now,
            updatedAt = now,
            note = note,
            connectedUserName = fromUser.name,
            connectedUserPhotoUrl = null, // Or fromUser.photoUrl if User has it
            connectedUserCollege = fromUser.collegeId
        )
        ref.set(connection).await()

        // Create notification
        val notificationRef = firestore.collection("users").document(toUserId).collection("notifications").document()
        notificationRef.set(
            mapOf(
                "id" to notificationRef.id,
                "type" to "CONNECTION_REQUEST",
                "fromUserId" to fromUser.userId,
                "fromUserName" to fromUser.name,
                "createdAt" to now,
                "read" to false
            )
        ).await()
    }

    // Accept a pending request
    suspend fun acceptRequest(connectionId: String): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val connRef = connectionsRef.document(connectionId)
            val conn = tx.get(connRef).toObject(RoommateConnection::class.java)
                ?: error("Connection not found")

            val status = ConnectionStatus.ACCEPTED.name
            val chatId = "conn_$connectionId"

            val updates = buildMap<String, Any?> {
                put("status", status)
                put("chatId", chatId)
                put("updatedAt", System.currentTimeMillis())
            }
            tx.update(connRef, updates)

            val chatRef = chatsRef.document(chatId)
            tx.set(
                chatRef,
                mapOf(
                    "id" to chatId,
                    "memberIds" to listOf(conn.requesterId, conn.receiverId),
                    "createdAt" to System.currentTimeMillis(),
                    "lastMessage" to "",
                    "lastMessageAt" to System.currentTimeMillis(),
                ),
            )

            // Optional: send notification back to requester
            val notificationRef = firestore.collection("users").document(conn.requesterId).collection("notifications").document()
            tx.set(notificationRef, mapOf(
                "id" to notificationRef.id,
                "type" to "CONNECTION_ACCEPTED",
                "fromUserId" to conn.receiverId,
                "fromUserName" to conn.connectedUserName,
                "createdAt" to System.currentTimeMillis(),
                "read" to false
            ))
        }.await()
    }

    // Reject / decline a request
    suspend fun rejectRequest(connectionId: String): Result<Unit> = runCatching {
        connectionsRef.document(connectionId).update(
            "status", ConnectionStatus.REJECTED.name,
            "updatedAt", System.currentTimeMillis()
        ).await()
    }

    // Remove / disconnect an existing accepted connection
    suspend fun removeConnection(connectionId: String): Result<Unit> = runCatching {
        // Here we just delete the connection, or change status to something else? The prompt says "remove".
        connectionsRef.document(connectionId).delete().await()
    }

    // Get all accepted connections for a user
    fun getConnections(userId: String): Flow<List<RoommateConnection>> = callbackFlow {
        val reqQuery = connectionsRef.whereEqualTo("requesterId", userId).whereEqualTo("status", ConnectionStatus.ACCEPTED.name)
        val recvQuery = connectionsRef.whereEqualTo("receiverId", userId).whereEqualTo("status", ConnectionStatus.ACCEPTED.name)

        val combined = mutableMapOf<String, RoommateConnection>()
        fun emit() { trySend(combined.values.sortedByDescending { it.updatedAt }) }

        val r1 = reqQuery.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            snap?.toObjects(RoommateConnection::class.java)?.forEach { combined[it.id] = it }
            emit()
        }
        val r2 = recvQuery.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            snap?.toObjects(RoommateConnection::class.java)?.forEach { combined[it.id] = it }
            emit()
        }
        awaitClose { r1.remove(); r2.remove() }
    }

    // Get all pending INCOMING requests for a user
    fun getIncomingRequests(userId: String): Flow<List<RoommateConnection>> = callbackFlow {
        val query = connectionsRef.whereEqualTo("receiverId", userId).whereEqualTo("status", ConnectionStatus.PENDING.name)
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val list = snap?.toObjects(RoommateConnection::class.java) ?: emptyList()
            trySend(list.sortedByDescending { it.createdAt })
        }
        awaitClose { reg.remove() }
    }

    // Get all pending OUTGOING requests sent by a user
    fun getOutgoingRequests(userId: String): Flow<List<RoommateConnection>> = callbackFlow {
        val query = connectionsRef.whereEqualTo("requesterId", userId).whereEqualTo("status", ConnectionStatus.PENDING.name)
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val list = snap?.toObjects(RoommateConnection::class.java) ?: emptyList()
            trySend(list.sortedByDescending { it.createdAt })
        }
        awaitClose { reg.remove() }
    }

    // Get the connection status between two specific users
    suspend fun getConnectionStatus(myId: String, otherId: String): ConnectionStatus? {
        val id = RoommateConnection.canonicalId(myId, otherId)
        val doc = connectionsRef.document(id).get().await()
        return doc.toObject(RoommateConnection::class.java)?.status
    }

    // Realtime Flow of connection between two specific users
    fun getConnectionBetween(myId: String, otherId: String): Flow<RoommateConnection?> = callbackFlow {
        val id = RoommateConnection.canonicalId(myId, otherId)
        val reg = connectionsRef.document(id).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(RoommateConnection::class.java))
        }
        awaitClose { reg.remove() }
    }

    // Get list of accepted connection user IDs
    suspend fun getConnectedUserIds(userId: String): Result<List<String>> = runCatching {
        val asReq = connectionsRef.whereEqualTo("requesterId", userId).whereEqualTo("status", ConnectionStatus.ACCEPTED.name).get().await()
        val asRecv = connectionsRef.whereEqualTo("receiverId", userId).whereEqualTo("status", ConnectionStatus.ACCEPTED.name).get().await()

        val reqIds = asReq.toObjects(RoommateConnection::class.java).map { it.receiverId }
        val recvIds = asRecv.toObjects(RoommateConnection::class.java).map { it.requesterId }
        (reqIds + recvIds).distinct()
    }
}
