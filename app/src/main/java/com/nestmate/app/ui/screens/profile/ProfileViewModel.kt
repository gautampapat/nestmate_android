package com.nestmate.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.model.SanitisedUserProfile
import com.nestmate.app.data.model.User
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.utils.FirebaseConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _profile = MutableStateFlow<User?>(null)
    val profile: StateFlow<User?> = _profile.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val uid = authRepository.getCurrentUserId() ?: return
        firestore.collection(FirebaseConstants.COLLECTION_USERS).document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Failed to listen for profile updates"
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _profile.value = snapshot.toObject(User::class.java)
                }
            }
    }

    private val _otherUserProfile = MutableStateFlow<SanitisedUserProfile?>(null)
    val otherUserProfile: StateFlow<SanitisedUserProfile?> = _otherUserProfile.asStateFlow()

    fun loadOtherUserProfile(targetUserId: String) {
        val currentUserId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val isConnected = false
            try {
                val snap = firestore.collection(FirebaseConstants.COLLECTION_USERS).document(targetUserId).get().await()
                val user = snap.toObject(User::class.java)
                if (user != null) {
                    _otherUserProfile.value = sanitiseUser(user, isConnected)
                } else {
                    _error.value = "Failed to load profile"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load profile"
            }
            _isLoading.value = false
        }
    }

    private fun sanitiseUser(user: User, isConnected: Boolean): SanitisedUserProfile {
        fun <T> visibleIf(setting: String, value: T): T? = when (setting) {
            "EVERYONE" -> value
            "CONNECTED_ONLY" -> if (isConnected) value else null
            else -> null // ONLY_ME
        }

        return SanitisedUserProfile(
            userId = user.userId,
            name = user.name,
            college = user.college,
            role = user.role,
            profilePhotoUrl = user.profilePhotoUrl,
            bio = user.bio,
            courseOrDepartment = user.courseOrDepartment,
            yearOfStudy = user.yearOfStudy,
            age = user.age,
            trustRating = user.trustRating,
            successfulTrades = user.successfulTrades,
            interestTags = user.interestTags,
            
            isVerified = user.isVerified,
            providerVerificationStatus = user.providerVerificationStatus,

            phone = visibleIf(user.phoneVisibility, user.phone),
            instagramUrl = visibleIf(user.socialVisibility, user.instagramUrl),
            linkedinUrl = visibleIf(user.socialVisibility, user.linkedinUrl),
            sleepSchedule = visibleIf(user.budgetVisibility, user.sleepSchedule),
            cleanliness = visibleIf(user.budgetVisibility, user.cleanliness),
            guestPolicy = visibleIf(user.budgetVisibility, user.guestPolicy),
            smokingDrinking = visibleIf(user.budgetVisibility, user.smokingDrinking),
            budgetMin = visibleIf(user.budgetVisibility, user.budgetMin),
            budgetMax = visibleIf(user.budgetVisibility, user.budgetMax)
        )
    }

    fun reportUser(targetUserId: String, reason: String) {
        // Phase 4 stub
    }

    fun blockUser(targetUserId: String) {
        // Phase 4 stub
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onSuccess()
        }
    }

    private val _updateState = MutableStateFlow<Boolean?>(null)
    val updateState: StateFlow<Boolean?> = _updateState.asStateFlow()

    fun updateProfile(updates: Map<String, Any?>) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            try {
                firestore.collection(FirebaseConstants.COLLECTION_USERS)
                    .document(userId)
                    .update(updates)
                    .await()
                _updateState.value = true
            } catch (e: Exception) {
                _updateState.value = false
            }
        }
    }

    fun resetUpdateState() { _updateState.value = null }

    fun clearError() { _error.value = null }
}
