package com.nestmate.app.ui.screens.auth

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.local.PreferencesManager
import com.nestmate.app.data.model.User
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.utils.ImageStorageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Stores the role chosen on RoleSelectionScreen before registration. */
    val selectedRole = MutableStateFlow("student")
    fun setSelectedRole(role: String) { selectedRole.value = role }

    val onboardingCompleted = preferencesManager.onboardingCompleted

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.loginUser(email, pass)
            _authState.value = if (result.isSuccess) AuthState.Success
            else AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed. Check your email and password.")
        }
    }

    fun register(user: User, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.registerUser(user, pass)
            _authState.value = if (result.isSuccess) AuthState.Success
            else AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed.")
        }
    }

    /**
     * Handles the Google Sign-In token returned from the GoogleSignIn activity result.
     * [role] is written to the user's Firestore doc only on first Google login (no existing role).
     */
    fun handleGoogleSignIn(idToken: String, role: String = "student") {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                // Write role only if this user has no role set yet (first Google sign-in)
                val uid = authRepository.getCurrentUserId()
                if (uid != null) {
                    val userResult = authRepository.getUserData(uid)
                    if (userResult.getOrNull()?.role.isNullOrBlank()) {
                        authRepository.updateUserProfile(uid, mapOf("role" to role))
                    }
                }
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Google sign-in failed."
                )
            }
        }
    }

    /**
     * Compresses the image to Base64 on IO dispatcher, then stores directly in Firestore.
     * Navigation is triggered by the LaunchedEffect watching for AuthState.Success.
     */
    fun uploadIdCard(imageUri: Uri) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _authState.value = AuthState.Error("You must be logged in to upload an ID.")
                return@launch
            }
            val compressResult = withContext(Dispatchers.IO) {
                ImageStorageUtil.compressToBase64(context, imageUri)
            }
            if (compressResult.isFailure) {
                _authState.value = AuthState.Error("Image processing failed. Try again.")
                return@launch
            }
            val result = authRepository.uploadIdCard(userId, compressResult.getOrThrow())
            _authState.value = if (result.isSuccess) AuthState.Success
            else AuthState.Error(result.exceptionOrNull()?.message ?: "Upload failed. Please try again.")
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
