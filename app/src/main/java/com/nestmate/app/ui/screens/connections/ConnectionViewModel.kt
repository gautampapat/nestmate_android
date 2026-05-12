package com.nestmate.app.ui.screens.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.ConnectionStatus
import com.nestmate.app.data.model.RoommateConnection
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.map

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionRepo: ConnectionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val connections: StateFlow<List<RoommateConnection>> = (
        currentUserId?.let { connectionRepo.getConnections(it) } ?: flowOf(emptyList())
    )
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingRequests: StateFlow<List<RoommateConnection>> = (
        currentUserId?.let { connectionRepo.getIncomingRequests(it) } ?: flowOf(emptyList())
    )
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outgoingRequests: StateFlow<List<RoommateConnection>> = (
        currentUserId?.let { connectionRepo.getOutgoingRequests(it) } ?: flowOf(emptyList())
    )
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingCount: StateFlow<Int> = incomingRequests
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun sendRequest(toUserId: String, note: String = "") {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val userRes = runCatching { authRepository.getUserData(uid).getOrThrow() }.getOrNull()
            if (userRes != null) {
                val result = connectionRepo.sendRequest(userRes, toUserId, note)
                result.onFailure { _errorMessage.value = it.message ?: "Failed to send request" }
            } else {
                _errorMessage.value = "Failed to fetch user data"
            }
            _isLoading.value = false
        }
    }

    fun acceptRequest(connectionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = connectionRepo.acceptRequest(connectionId)
            result.onFailure { _errorMessage.value = it.message ?: "Failed to accept request" }
            _isLoading.value = false
        }
    }

    fun rejectRequest(connectionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = connectionRepo.rejectRequest(connectionId)
            result.onFailure { _errorMessage.value = it.message ?: "Failed to decline request" }
            _isLoading.value = false
        }
    }

    fun removeConnection(connectionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = connectionRepo.removeConnection(connectionId)
            result.onFailure { _errorMessage.value = it.message ?: "Failed to remove connection" }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
