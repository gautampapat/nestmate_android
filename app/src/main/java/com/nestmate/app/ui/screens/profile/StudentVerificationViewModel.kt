package com.nestmate.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.utils.verification.OtpEmailSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject

@HiltViewModel
class StudentVerificationViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val uiState: StateFlow<VerificationState> = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun sendOtp(email: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            _uiState.value = VerificationState.Loading
            
            try {
                // Generate 6-digit OTP
                val otpCode = (SecureRandom().nextInt(900000) + 100000).toString()
                
                // Hash the OTP
                val hashedOtp = hashOtp(otpCode, uid)
                val expiresAt = System.currentTimeMillis() + 10 * 60 * 1000L // 10 minutes
                
                // Save to Firestore
                val requestData = mapOf(
                    "hashedOtp" to hashedOtp,
                    "expiresAt" to expiresAt,
                    "targetEmail" to email,
                    "attemptCount" to 0,
                    "userId" to uid
                )
                
                firestore.collection("verificationRequests").document(uid).set(requestData).await()
                
                // Get user name for the email
                val userName = authRepository.getUserData(uid).getOrNull()?.name ?: "Student"
                
                // Send Email via EmailJS
                val sendResult = OtpEmailSender.sendOtp(email, userName, otpCode, 10)
                
                if (sendResult.isSuccess) {
                    _uiState.value = VerificationState.OtpSent(expiresAt)
                } else {
                    // Surface the real error from OtpEmailSender (EmailJS body, network error, etc.)
                    _error.value = sendResult.exceptionOrNull()?.message ?: "Failed to send email."
                    _uiState.value = VerificationState.Idle
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
                _uiState.value = VerificationState.Idle
            }
        }
    }

    fun verifyOtp(enteredOtp: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            _uiState.value = VerificationState.Loading
            
            try {
                val doc = firestore.collection("verificationRequests").document(uid).get().await()
                if (!doc.exists()) {
                    _error.value = "No active verification request found."
                    _uiState.value = VerificationState.Idle
                    return@launch
                }
                
                val expiresAt = doc.getLong("expiresAt") ?: 0L
                val attemptCount = doc.getLong("attemptCount")?.toInt() ?: 0
                val hashedOtp = doc.getString("hashedOtp") ?: ""
                
                if (System.currentTimeMillis() > expiresAt) {
                    _error.value = "Code expired. Please request a new one."
                    _uiState.value = VerificationState.Idle
                    return@launch
                }
                
                if (attemptCount >= 5) {
                    _error.value = "Too many failed attempts. Please wait 30 minutes."
                    _uiState.value = VerificationState.Idle
                    return@launch
                }
                
                val hashedInput = hashOtp(enteredOtp, uid)
                if (hashedInput == hashedOtp) {
                    // Success!
                    firestore.collection("users").document(uid).update(
                        mapOf(
                            "isVerified" to true,
                            "verificationStatus" to "Verified"
                        )
                    ).await()
                    
                    firestore.collection("verificationRequests").document(uid).delete().await()
                    
                    authRepository.notifyVerificationSuccess()
                    _uiState.value = VerificationState.Success
                } else {
                    // Mismatch
                    val newAttemptCount = attemptCount + 1
                    var newExpiresAt = expiresAt
                    
                    if (newAttemptCount >= 5) {
                        newExpiresAt = System.currentTimeMillis() + 30 * 60 * 1000L // Lockout for 30 mins
                    }
                    
                    firestore.collection("verificationRequests").document(uid).update(
                        mapOf(
                            "attemptCount" to newAttemptCount,
                            "expiresAt" to newExpiresAt
                        )
                    ).await()
                    
                    val remaining = 5 - newAttemptCount
                    if (remaining > 0) {
                        _error.value = "Incorrect code. $remaining attempts remaining."
                        _uiState.value = VerificationState.OtpSent(expiresAt)
                    } else {
                        _error.value = "Too many failed attempts. Please wait 30 minutes."
                        _uiState.value = VerificationState.Idle
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
                _uiState.value = VerificationState.Idle
            }
        }
    }

    private fun hashOtp(otp: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = otp + salt
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun clearError() {
        _error.value = null
    }

    fun setError(message: String) {
        _error.value = message
    }
}

sealed class VerificationState {
    object Idle : VerificationState()
    object Loading : VerificationState()
    data class OtpSent(val expiresAt: Long) : VerificationState()
    object Success : VerificationState()
}
