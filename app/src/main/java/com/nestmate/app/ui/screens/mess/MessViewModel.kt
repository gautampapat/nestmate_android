package com.nestmate.app.ui.screens.mess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.Mess
import com.nestmate.app.data.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MessState {
    object Loading : MessState()
    data class Success(val messes: List<Mess>) : MessState()
    data class Error(val message: String) : MessState()
}

@HiltViewModel
class MessViewModel @Inject constructor(
    private val messRepository: MessRepository
) : ViewModel() {

    private val _rawMesses = MutableStateFlow<List<Mess>>(emptyList())
    
    private val _foodFilter = MutableStateFlow("All")
    val foodFilter: StateFlow<String> = _foodFilter.asStateFlow()

    val uiState: StateFlow<MessState> = combine(_rawMesses, _foodFilter) { messes, filter ->
        val filtered = if (filter == "All") messes else messes.filter { it.vegNonVeg.equals(filter, ignoreCase = true) }
        MessState.Success(filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MessState.Loading)

    private val _selectedCrowdLevel = MutableStateFlow<String?>(null)
    val selectedCrowdLevel: StateFlow<String?> = _selectedCrowdLevel.asStateFlow()

    // Single-shot snackbar message for crowd vote feedback
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        // Collect the real-time Flow — crowd levels update automatically via Firestore listener
        viewModelScope.launch {
            messRepository.getMessesFlow().collect { messes ->
                _rawMesses.value = messes
            }
        }
    }

    fun loadUserVote(messId: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val vote = messRepository.getUserVote(messId, userId)
            _selectedCrowdLevel.value = vote
        }
    }

    fun setFoodFilter(filter: String) {
        _foodFilter.value = filter
    }

    /**
     * Submits a crowd vote for the given mess.
     * The real-time listener will automatically update the UI after the transaction completes.
     */
    fun submitCrowdVote(messId: String, vote: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _snackbarMessage.value = "Please log in to vote."
            return
        }
        
        viewModelScope.launch {
            val currentVote = _selectedCrowdLevel.value
            if (currentVote == vote) {
                // Toggle off
                _selectedCrowdLevel.value = null
                val result = messRepository.removeCrowdVote(messId, userId)
                if (result.isSuccess) {
                    _snackbarMessage.value = "Vote removed."
                } else {
                    _selectedCrowdLevel.value = currentVote // Revert on failure
                    _snackbarMessage.value = "Failed to remove vote."
                }
            } else {
                // Submit or change vote
                _selectedCrowdLevel.value = vote
                val result = messRepository.submitCrowdVote(messId, userId, vote)
                if (result.isSuccess) {
                    _snackbarMessage.value = "Thanks for reporting! Crowd level updated."
                } else {
                    _selectedCrowdLevel.value = currentVote // Revert on failure
                    _snackbarMessage.value = "Vote failed. Please try again."
                }
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
