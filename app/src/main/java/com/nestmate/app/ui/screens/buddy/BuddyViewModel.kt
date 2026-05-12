package com.nestmate.app.ui.screens.buddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.BuddyMessage
import com.nestmate.app.data.model.BuddyPair
import com.nestmate.app.data.model.User
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.BuddyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BuddyState {
    object Loading : BuddyState()
    data class Success(val pairs: List<BuddyPair>, val availableSeniors: List<User> = emptyList()) : BuddyState()
    data class Error(val message: String) : BuddyState()
}

@HiltViewModel
class BuddyViewModel @Inject constructor(
    private val buddyRepository: BuddyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BuddyState>(BuddyState.Loading)
    val uiState: StateFlow<BuddyState> = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<BuddyMessage>>(emptyList())
    val chatMessages: StateFlow<List<BuddyMessage>> = _chatMessages.asStateFlow()

    fun loadBuddyHome() {
        viewModelScope.launch {
            _uiState.value = BuddyState.Loading
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            val pairsResult = buddyRepository.getActivePairsForUser(userId)
            val seniorsResult = buddyRepository.getSeniors("wce_sangli") // default for MVP phase 1
            
            if (pairsResult.isSuccess && seniorsResult.isSuccess) {
                _uiState.value = BuddyState.Success(
                    pairs = pairsResult.getOrNull() ?: emptyList(),
                    availableSeniors = seniorsResult.getOrNull() ?: emptyList()
                )
            } else {
                _uiState.value = BuddyState.Error("Failed to sync buddy ecosystem.")
            }
        }
    }

    fun requestBuddy(seniorId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            buddyRepository.requestBuddy(userId, seniorId, "wce_sangli")
            loadBuddyHome() // re-fetch after requesting
        }
    }

    fun observeChat(pairId: String) {
        viewModelScope.launch {
            buddyRepository.observeMessages(pairId).collect { messages ->
                _chatMessages.value = messages
            }
        }
    }

    fun sendMessage(pairId: String, text: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            buddyRepository.sendMessage(pairId, userId, text)
        }
    }
    
    fun getCurrentUserId() = authRepository.getCurrentUserId()
}
