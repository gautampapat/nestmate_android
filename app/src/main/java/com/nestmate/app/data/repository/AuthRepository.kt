package com.nestmate.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.model.User
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    private val _verificationSuccessEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val verificationSuccessEvent = _verificationSuccessEvent.asSharedFlow()

    suspend fun notifyVerificationSuccess() {
        _verificationSuccessEvent.emit(Unit)
    }

    fun observeAuthState(): Flow<AuthSessionState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            trySend(
                if (uid != null) AuthSessionState.Authenticated(uid)
                else AuthSessionState.Unauthenticated
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection(FirebaseConstants.COLLECTION_USERS)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { listener.remove() }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun getUserData(userId: String): Result<User?> {
        return try {
            val doc = firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            Result.success(doc.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(user: User, password: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(user.email, password).await()
            val createdUserId = authResult.user?.uid ?: throw Exception("User creation failed")
            val finalUser = user.copy(
                userId = createdUserId,
                collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID
            )
            firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .document(createdUserId)
                .set(finalUser)
                .await()
            Result.success(finalUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs in with a Google ID token obtained from GoogleSignIn client.
     * Creates a Firestore user document if this is first sign-in.
     */
    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google sign-in failed")

            // Create Firestore profile if new user
            val doc = firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .document(firebaseUser.uid).get().await()
            if (!doc.exists()) {
                val newUser = User(
                    userId = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "NestMate User",
                    email = firebaseUser.email ?: "",
                    profilePhotoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID,
                    verificationStatus = "Pending"
                )
                firestore.collection(FirebaseConstants.COLLECTION_USERS)
                    .document(firebaseUser.uid).set(newUser).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stores the pre-encoded Base64 ID card string in Firestore and sets verificationStatus = "Pending".
     */
    suspend fun uploadIdCard(userId: String, base64String: String): Result<Unit> {
        return try {
            firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .document(userId)
                .update(
                    mapOf(
                        "collegeIdUrl" to base64String,
                        "verificationStatus" to "Pending"
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates user profile fields in Firestore.
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
