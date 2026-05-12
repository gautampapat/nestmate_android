package com.nestmate.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nestmate.app.utils.FirebaseConstants
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class CommunityAlert(
    val id: String = "",
    val message: String = "",
    val type: String = "INFO",
    val createdAt: Timestamp? = null,
    val collegeId: String = ""
)

data class ForumPost(
    val id: String = "",
    val authorName: String = "",
    val content: String = "",
    val commentCount: Int = 0,
    val createdAt: Timestamp? = null,
    val collegeId: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID

    val timeGreeting: String = run {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning ☀️"
            in 12..16 -> "Good Afternoon 👋"
            in 17..20 -> "Good Evening 🌆"
            else -> "Good Night 🌙"
        }
    }

    val userProfile: StateFlow<User?> = callbackFlow {
        val uid = authRepository.getCurrentUserId()
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
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestAlert: StateFlow<CommunityAlert?> = callbackFlow {
        val listener = firestore.collection(FirebaseConstants.COLLECTION_ALERTS)
            .whereEqualTo("collegeId", collegeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val alert = snapshot?.documents?.firstOrNull()?.toObject(CommunityAlert::class.java)?.copy(id = snapshot.documents.first().id)
                trySend(alert)
            }
        awaitClose { listener.remove() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestPost: StateFlow<ForumPost?> = callbackFlow {
        val listener = firestore.collection("forums")
            .whereEqualTo("collegeId", collegeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val post = snapshot?.documents?.firstOrNull()?.toObject(ForumPost::class.java)?.copy(id = snapshot.documents.first().id)
                trySend(post)
            }
        awaitClose { listener.remove() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
