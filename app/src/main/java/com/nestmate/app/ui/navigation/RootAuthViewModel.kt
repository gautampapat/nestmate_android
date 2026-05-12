package com.nestmate.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.AuthSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthSessionState?> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Three-state role signal:
     *   null             = role not yet fetched (show loading indicator)
     *   "student"        = route to StudentHome
     *   "service_provider" = route to ProviderDashboard
     *
     * Reset to null on sign-out so the next login always re-fetches.
     */
    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    init {
        viewModelScope.launch {
            authState.collect { state ->
                when (state) {
                    is AuthSessionState.Authenticated -> {
                        // Keep null until the Firestore fetch completes so RootNavGate
                        // shows a loading indicator and does not flash to the wrong screen.
                        _userRole.value = null
                        val result = authRepository.getUserData(state.uid)
                        val role = result.getOrNull()?.role?.takeIf { it.isNotBlank() } ?: "student"
                        _userRole.value = role
                    }
                    is AuthSessionState.Unauthenticated -> {
                        // Reset so the next login always triggers a fresh fetch
                        _userRole.value = null
                    }
                    null -> { /* initial state, do nothing */ }
                }
            }
        }

        viewModelScope.launch {
            authRepository.verificationSuccessEvent.collect {
                val uid = authRepository.getCurrentUserId()
                if (uid != null) {
                    val result = authRepository.getUserData(uid)
                    val role = result.getOrNull()?.role?.takeIf { it.isNotBlank() } ?: "student"
                    _userRole.value = role
                }
            }
        }
    }
}
