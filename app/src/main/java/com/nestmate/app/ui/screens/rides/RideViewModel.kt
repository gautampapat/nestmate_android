package com.nestmate.app.ui.screens.rides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.FareEstimate
import com.nestmate.app.data.model.PredefinedRoute
import com.nestmate.app.data.model.RideRequest
import com.nestmate.app.data.model.RideReview
import com.nestmate.app.data.model.RideStatus
import com.nestmate.app.data.model.RideType
import com.nestmate.app.data.model.VehicleType
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.RideRepository
import com.nestmate.app.utils.ride.FareCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RideViewModel @Inject constructor(
    private val repository: RideRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val predefinedRoutes: StateFlow<List<PredefinedRoute>> = repository.getPredefinedRoutes()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val activeRide: StateFlow<RideRequest?> = (
        currentUserId?.let { repository.getActiveRide(it) } ?: flowOf(null)
        )
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    val rideHistory: StateFlow<List<RideRequest>> = (
        currentUserId?.let { repository.getRideHistory(it) } ?: flowOf(emptyList())
        )
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val scheduledRides: StateFlow<List<RideRequest>> = (
        currentUserId?.let { repository.getScheduledRides(it) } ?: flowOf(emptyList())
        )
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun estimateFare(
        rideType: RideType,
        route: PredefinedRoute?,
        passengers: Int = 1,
    ): FareEstimate = FareCalculator.estimate(rideType, route, expectedPassengers = passengers)

    fun createRide(
        pickup: String,
        drop: String,
        rideType: RideType,
        scheduledAt: Long?,
        maxBudget: Long?,
        route: PredefinedRoute?,
        onDone: (Result<String>) -> Unit,
    ) {
        val uid = currentUserId ?: run {
            onDone(Result.failure(IllegalStateException("Sign in first"))); return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val name = runCatching { authRepository.getUserData(uid) }
                    .getOrNull()?.getOrNull()?.name.orEmpty()
                val estimate = FareCalculator.estimate(rideType, route, expectedPassengers = 2)
                val fare = if (estimate.minRupees == estimate.maxRupees) estimate.minRupees
                else (estimate.minRupees + estimate.maxRupees) / 2
                val request = RideRequest(
                    requesterId = uid,
                    requesterName = name,
                    pickupLocation = pickup,
                    dropLocation = drop,
                    rideType = rideType,
                    scheduledAt = scheduledAt,
                    maxBudget = maxBudget,
                    estimatedFare = fare,
                    status = RideStatus.SEARCHING,
                )
                val result = repository.createRideRequest(request)
                onDone(result)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Failed to create ride"
                onDone(Result.failure(t))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun findMatchingShared(
        drop: String,
        scheduledAt: Long?,
    ): Flow<List<RideRequest>> {
        val uid = currentUserId ?: return flowOf(emptyList())
        return repository.getMatchingSharedRides(drop, scheduledAt, uid)
    }

    fun joinRide(rideId: String, onDone: (Boolean) -> Unit = {}) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val result = repository.joinSharedRide(rideId, uid)
            result.onFailure { _error.value = it.message ?: "Could not join ride" }
            onDone(result.isSuccess)
        }
    }

    fun cancelRide(rideId: String, onDone: (Boolean) -> Unit = {}) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val result = repository.cancelRide(rideId, uid)
            result.onFailure { _error.value = it.message ?: "Could not cancel ride" }
            onDone(result.isSuccess)
        }
    }

    fun startRide(
        rideId: String,
        driverName: String,
        vehicleNumber: String,
        vehicleType: VehicleType,
        onDone: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = repository.startRide(rideId, driverName, vehicleNumber, vehicleType)
            result.onFailure { _error.value = it.message ?: "Could not start ride" }
            onDone(result.isSuccess)
        }
    }

    fun completeRide(rideId: String, totalFareRupees: Long, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.completeRide(rideId, totalFareRupees)
            result.onFailure { _error.value = it.message ?: "Could not complete ride" }
            onDone(result.isSuccess)
        }
    }

    fun submitReview(
        rideId: String,
        targetId: String,
        rating: Float,
        comment: String?,
        onDone: (Boolean) -> Unit = {},
    ) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val result = repository.submitReview(
                RideReview(
                    rideId = rideId,
                    reviewerId = uid,
                    targetId = targetId,
                    rating = rating,
                    comment = comment,
                ),
            )
            result.onFailure { _error.value = it.message ?: "Could not submit review" }
            onDone(result.isSuccess)
        }
    }

    fun getRideById(rideId: String): Flow<RideRequest?> = repository.getRequestById(rideId)

    fun clearError() { _error.value = null }
}
