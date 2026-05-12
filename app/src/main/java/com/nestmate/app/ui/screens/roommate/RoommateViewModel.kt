package com.nestmate.app.ui.screens.roommate

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.FoodPreference
import com.nestmate.app.data.model.Gender
import com.nestmate.app.data.model.HabitPreference
import com.nestmate.app.data.model.RoomType
import com.nestmate.app.data.model.RoommateConnection
import com.nestmate.app.data.model.RoommateFilters
import com.nestmate.app.data.model.RoommateGroupListing
import com.nestmate.app.data.model.RoommateProfile
import com.nestmate.app.data.model.User
import com.nestmate.app.data.model.SleepSchedule
import com.nestmate.app.data.model.StudyHabit
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.RoommateRepository
import com.nestmate.app.utils.imageupload.ImageCompressor
import com.nestmate.app.utils.roommate.CompatibilityResult
import com.nestmate.app.utils.roommate.CompatibilityScorer
import com.nestmate.app.data.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScoredProfile(
    val profile: RoommateProfile,
    val score: Int,
    val breakdown: CompatibilityResult,
    val isRecentlyActive: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RoommateViewModel @Inject constructor(
    private val repository: RoommateRepository,
    private val connectionRepository: ConnectionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val _filters = MutableStateFlow(RoommateFilters())
    val filters: StateFlow<RoommateFilters> = _filters.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val currentUserProfile: StateFlow<RoommateProfile?> = (
        currentUserId?.let { repository.getProfile(it) } ?: flowOf(null)
        )
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private val allProfiles: Flow<List<RoommateProfile>> =
        (currentUserId?.let { repository.getAllProfiles(it) } ?: flowOf(emptyList()))
            .catch { emit(emptyList()) }

    val scoredProfiles: StateFlow<List<ScoredProfile>> = combine(
        allProfiles,
        currentUserProfile,
        _filters,
    ) { profiles, self, f -> buildScored(profiles, self, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val connections: StateFlow<List<RoommateConnection>> = (
        currentUserId?.let { connectionRepository.getConnections(it) } ?: flowOf(emptyList())
        )
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val groups: StateFlow<List<RoommateGroupListing>> = repository.getOpenGroups()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private fun buildScored(
        profiles: List<RoommateProfile>,
        self: RoommateProfile?,
        filters: RoommateFilters,
    ): List<ScoredProfile> {
        if (self == null) return emptyList()
        val blocksMe = profiles.filter { self.userId in it.blockedUserIds }.map { it.userId }.toSet()
        val iBlock = self.blockedUserIds.toSet()
        val now = System.currentTimeMillis()
        val activeWindowMs = 48L * 60L * 60L * 1000L

        val filtered = profiles.filter { other ->
            if (other.userId in iBlock) return@filter false
            if (other.userId in blocksMe) return@filter false
            filters.gender?.let { if (other.gender != it) return@filter false }
            filters.roomType?.let { if (other.roomTypePreference != it) return@filter false }
            filters.location?.takeIf { it.isNotBlank() }?.let {
                if (!other.preferredLocation.equals(it, ignoreCase = true)) return@filter false
            }
            val minOk = filters.minBudget?.let { other.maxBudget >= it } ?: true
            val maxOk = filters.maxBudget?.let { other.minBudget <= it } ?: true
            minOk && maxOk
        }

        return filtered.map { other ->
            val result = CompatibilityScorer.score(self, other)
            ScoredProfile(
                profile = other,
                score = result.score,
                breakdown = result,
                isRecentlyActive = now - other.lastActiveAt <= activeWindowMs,
            )
        }.sortedByDescending { it.score }
    }

    fun setGenderFilter(g: Gender?) { _filters.value = _filters.value.copy(gender = g) }
    fun setRoomTypeFilter(t: RoomType?) { _filters.value = _filters.value.copy(roomType = t) }
    fun setLocationFilter(l: String?) {
        _filters.value = _filters.value.copy(location = l?.takeIf { it.isNotBlank() })
    }

    fun setBudgetFilter(min: Long?, max: Long?) {
        _filters.value = _filters.value.copy(minBudget = min, maxBudget = max)
    }

    fun clearFilters() { _filters.value = RoommateFilters() }

    fun getScoredFor(targetUserId: String): ScoredProfile? =
        scoredProfiles.value.firstOrNull { it.profile.userId == targetUserId }

    fun saveProfile(
        context: Context,
        profile: RoommateProfile,
        newPhotoUri: Uri?,
        onDone: (Result<Unit>) -> Unit,
    ) {
        val uid = currentUserId ?: run {
            onDone(Result.failure(IllegalStateException("Sign in first"))); return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val photoUrl = if (newPhotoUri != null) {
                    val bytes = ImageCompressor.compress(newPhotoUri, context, targetMaxBytes = 300_000L)
                    repository.uploadPhoto(uid, bytes).getOrThrow()
                } else profile.photoUrl

                val toSave = profile.copy(
                    userId = uid,
                    photoUrl = photoUrl,
                )
                val result = repository.saveProfile(toSave)
                onDone(result)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Could not save profile"
                onDone(Result.failure(t))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendConnectRequest(targetUserId: String, note: String = "", onDone: (Result<Unit>) -> Unit = {}) {
        val uid = currentUserId ?: run {
            onDone(Result.failure(IllegalStateException("Sign in first"))); return
        }
        viewModelScope.launch {
            // Need a dummy User object or fetch it from authRepository
            val userRes = runCatching { authRepository.getUserData(uid).getOrThrow() }
            val user = userRes.getOrNull() ?: run {
                onDone(Result.failure(IllegalStateException("User data not found")))
                return@launch
            }
            val result = connectionRepository.sendRequest(user, targetUserId, note)
            result.onFailure { _error.value = it.message ?: "Could not send request" }
            onDone(result)
        }
    }

    fun respondToRequest(connectionId: String, accept: Boolean) {
        viewModelScope.launch {
            val result = if (accept) {
                connectionRepository.acceptRequest(connectionId)
            } else {
                connectionRepository.rejectRequest(connectionId)
            }
            result.onFailure { _error.value = it.message ?: "Could not update request" }
        }
    }

    fun blockUser(targetUserId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repository.blockUser(uid, targetUserId)
                .onFailure { _error.value = it.message ?: "Could not block user" }
        }
    }

    fun observeConnectionWith(targetUserId: String): Flow<RoommateConnection?> {
        val uid = currentUserId ?: return flowOf(null)
        return connectionRepository.getConnectionBetween(uid, targetUserId)
    }

    fun createGroup(
        flatDescription: String,
        location: String,
        rent: Long,
        spotsNeeded: Int,
        onDone: (Result<String>) -> Unit = {},
    ) {
        val uid = currentUserId ?: run {
            onDone(Result.failure(IllegalStateException("Sign in first"))); return
        }
        viewModelScope.launch {
            val creatorName = runCatching { authRepository.getUserData(uid) }
                .getOrNull()?.getOrNull()?.name.orEmpty()
            val group = RoommateGroupListing(
                creatorId = uid,
                creatorName = creatorName,
                flatDescription = flatDescription,
                location = location,
                rent = rent,
                spotsNeeded = spotsNeeded,
            )
            val result = repository.createGroup(group)
            result.onFailure { _error.value = it.message ?: "Could not create group" }
            onDone(result)
        }
    }

    fun requestJoinGroup(groupId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repository.requestJoinGroup(groupId, uid)
                .onFailure { _error.value = it.message ?: "Could not request join" }
        }
    }

    fun approveJoinRequest(groupId: String, userId: String) {
        viewModelScope.launch {
            repository.approveJoinRequest(groupId, userId)
                .onFailure { _error.value = it.message ?: "Could not approve request" }
        }
    }

    fun profileTemplateFromUser(): RoommateProfile {
        val uid = currentUserId ?: return RoommateProfile()
        return RoommateProfile(
            userId = uid,
            gender = Gender.PREFER_NOT_TO_SAY,
            roomTypePreference = RoomType.SHARED_ROOM,
            sleepingSchedule = SleepSchedule.NIGHT_OWL,
            studyHabits = StudyHabit.QUIET_STUDIER,
            foodPreference = FoodPreference.VEG,
            smokingHabit = HabitPreference.NO,
            drinkingHabit = HabitPreference.NO,
            isActivelySearching = true,
        )
    }

    fun clearError() { _error.value = null }
}
