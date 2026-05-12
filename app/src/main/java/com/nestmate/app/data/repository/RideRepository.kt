package com.nestmate.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nestmate.app.data.model.PredefinedRoute
import com.nestmate.app.data.model.RideRequest
import com.nestmate.app.data.model.RideReview
import com.nestmate.app.data.model.RideStatus
import com.nestmate.app.data.model.RideType
import com.nestmate.app.data.model.VehicleType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private val requestsRef
        get() = firestore.collection("rides").document("root").collection("requests")
    private val reviewsRef
        get() = firestore.collection("rides").document("root").collection("reviews")
    private val routesRef
        get() = firestore.collection("rides").document("root").collection("predefinedRoutes")

    private fun userRideHistoryRef(userId: String) =
        firestore.collection("users").document(userId).collection("rideHistory")

    // ---- Ride requests ----

    suspend fun createRideRequest(request: RideRequest): Result<String> = runCatching {
        val ref = requestsRef.document()
        val toWrite = request.copy(id = ref.id, createdAt = System.currentTimeMillis())
        ref.set(toWrite).await()
        userRideHistoryRef(request.requesterId)
            .document(ref.id)
            .set(mapOf("rideId" to ref.id, "createdAt" to System.currentTimeMillis()))
            .await()
        ref.id
    }

    fun getRequestById(rideId: String): Flow<RideRequest?> = callbackFlow {
        val reg = requestsRef.document(rideId).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(RideRequest::class.java))
        }
        awaitClose { reg.remove() }
    }

    fun getMatchingSharedRides(
        drop: String,
        scheduledAt: Long?,
        currentUserId: String,
    ): Flow<List<RideRequest>> = callbackFlow {
        // Single equality to stay index-free; filter rideType + status window client-side.
        val reg = requestsRef
            .whereEqualTo("status", RideStatus.SEARCHING.name)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val rides = snap?.toObjects(RideRequest::class.java).orEmpty()
                    .filter { ride ->
                        if (ride.rideType != RideType.SHARED) return@filter false
                        if (ride.requesterId == currentUserId) return@filter false
                        if (!ride.dropLocation.equals(drop, ignoreCase = true)) return@filter false
                        val windowMs = 15L * 60L * 1000L
                        if (scheduledAt == null || ride.scheduledAt == null) true
                        else Math.abs(scheduledAt - ride.scheduledAt) <= windowMs
                    }
                trySend(rides)
            }
        awaitClose { reg.remove() }
    }

    suspend fun joinSharedRide(rideId: String, userId: String): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val ref = requestsRef.document(rideId)
            val snap = tx.get(ref)
            val current = snap.toObject(RideRequest::class.java) ?: error("Ride not found")
            if (current.status != RideStatus.SEARCHING && current.status != RideStatus.MATCHED) {
                error("Ride is not joinable")
            }
            if (userId in current.confirmedPassengerIds) error("Already joined")

            val newPassengers = current.confirmedPassengerIds + userId
            val newFarePerPassenger = if (current.estimatedFare > 0L) {
                (current.estimatedFare + newPassengers.size - 1) / newPassengers.size
            } else current.farePerPassenger
            val newStatus = if (newPassengers.size >= 3) RideStatus.MATCHED.name else current.status.name

            tx.update(
                ref,
                mapOf(
                    "confirmedPassengerIds" to newPassengers,
                    "farePerPassenger" to newFarePerPassenger,
                    "status" to newStatus,
                ),
            )
        }.await()
        userRideHistoryRef(userId)
            .document(rideId)
            .set(mapOf("rideId" to rideId, "createdAt" to System.currentTimeMillis()))
            .await()
    }

    fun getActiveRide(userId: String): Flow<RideRequest?> = callbackFlow {
        // Split whereEqualTo + whereIn into separate single-equality queries; filter status client-side.
        val activeStatusesForRequester = setOf(
            RideStatus.SEARCHING, RideStatus.MATCHED, RideStatus.IN_PROGRESS,
        )
        val activeStatusesForPassenger = setOf(
            RideStatus.MATCHED, RideStatus.IN_PROGRESS,
        )

        var asRequester: RideRequest? = null
        var asPassenger: RideRequest? = null
        fun emit() {
            trySend(
                listOfNotNull(asRequester, asPassenger)
                    .maxByOrNull { it.createdAt },
            )
        }

        val r1 = requestsRef
            .whereEqualTo("requesterId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                asRequester = snap?.toObjects(RideRequest::class.java).orEmpty()
                    .filter { it.status in activeStatusesForRequester }
                    .maxByOrNull { it.createdAt }
                emit()
            }
        val r2 = requestsRef
            .whereArrayContains("confirmedPassengerIds", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                asPassenger = snap?.toObjects(RideRequest::class.java).orEmpty()
                    .filter { it.status in activeStatusesForPassenger }
                    .maxByOrNull { it.createdAt }
                emit()
            }
        awaitClose { r1.remove(); r2.remove() }
    }

    fun getRideHistory(userId: String): Flow<List<RideRequest>> = callbackFlow {
        val reg = userRideHistoryRef(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val ids = snap?.documents?.mapNotNull { it.getString("rideId") }.orEmpty()
                if (ids.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                // Fetch the full ride docs, then emit once when all resolve. No interim
                // emptyList emit — that caused a "No rides yet" flicker on every update.
                val jobs = ids.map { id -> requestsRef.document(id).get() }
                com.google.android.gms.tasks.Tasks.whenAllComplete(jobs)
                    .addOnSuccessListener {
                        val rides = jobs.mapNotNull { t ->
                            t.result?.toObject(RideRequest::class.java)
                        }
                        trySend(rides.sortedByDescending { it.createdAt })
                    }
            }
        awaitClose { reg.remove() }
    }

    fun getScheduledRides(userId: String): Flow<List<RideRequest>> = callbackFlow {
        val reg = requestsRef
            .whereEqualTo("requesterId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val rides = snap?.toObjects(RideRequest::class.java).orEmpty()
                    .filter { it.scheduledAt != null && it.status == RideStatus.SEARCHING }
                    .sortedBy { it.scheduledAt ?: 0L }
                trySend(rides)
            }
        awaitClose { reg.remove() }
    }

    suspend fun cancelRide(rideId: String, userId: String): Result<Unit> = runCatching {
        requestsRef.document(rideId).update(
            mapOf(
                "status" to RideStatus.CANCELLED.name,
                "cancelledBy" to userId,
            ),
        ).await()
    }

    suspend fun startRide(
        rideId: String,
        driverName: String,
        vehicleNumber: String,
        vehicleType: VehicleType,
    ): Result<Unit> = runCatching {
        requestsRef.document(rideId).update(
            mapOf(
                "status" to RideStatus.IN_PROGRESS.name,
                "driverName" to driverName,
                "vehicleNumber" to vehicleNumber,
                "vehicleType" to vehicleType.name,
            ),
        ).await()
    }

    suspend fun completeRide(rideId: String, totalFareRupees: Long): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val ref = requestsRef.document(rideId)
            val snap = tx.get(ref)
            val current = snap.toObject(RideRequest::class.java) ?: error("Ride not found")
            val passengers = (current.confirmedPassengerIds.size + 1).coerceAtLeast(1)
            val perPassenger = (totalFareRupees + passengers - 1) / passengers
            tx.update(
                ref,
                mapOf(
                    "status" to RideStatus.COMPLETED.name,
                    "totalFare" to totalFareRupees,
                    "farePerPassenger" to perPassenger,
                ),
            )
        }.await()
    }

    // ---- Reviews ----

    suspend fun submitReview(review: RideReview): Result<Unit> = runCatching {
        val ref = reviewsRef.document()
        val toWrite = review.copy(id = ref.id, createdAt = System.currentTimeMillis())
        ref.set(toWrite).await()
    }

    // ---- Predefined routes ----

    fun getPredefinedRoutes(): Flow<List<PredefinedRoute>> = callbackFlow {
        val reg = routesRef.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObjects(PredefinedRoute::class.java) ?: emptyList())
        }
        awaitClose { reg.remove() }
    }
}
