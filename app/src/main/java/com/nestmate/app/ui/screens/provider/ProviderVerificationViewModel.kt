package com.nestmate.app.ui.screens.provider

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.utils.imageupload.CloudinaryUploader
import com.nestmate.app.utils.imageupload.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProviderVerificationViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val cloudinaryUploader: CloudinaryUploader
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _serviceType = MutableStateFlow("Service")
    val serviceType: StateFlow<String> = _serviceType.asStateFlow()

    private val _status = MutableStateFlow("idle")
    val status: StateFlow<String> = _status.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val user = authRepository.getUserData(uid).getOrNull()
            user?.let { 
                _serviceType.value = it.serviceType 
                _status.value = it.providerVerificationStatus
            }
        }
    }

    fun uploadDocuments(context: Context, uris: List<Uri>, businessName: String, gstNumber: String, onDone: (Boolean) -> Unit) {
        val uid = authRepository.getCurrentUserId() ?: return
        
        if (uris.isEmpty()) {
            _error.value = "Please select at least one document."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val uploadedUrls = mutableListOf<String>()
                
                for (uri in uris) {
                    val bytes = ImageCompressor.compress(uri, context, targetMaxBytes = 800_000L) // Allow slightly larger for docs
                    val urlResult = cloudinaryUploader.uploadCompressed(bytes, "provider_docs/$uid")
                    urlResult.onSuccess { uploadedUrls.add(it) }
                             .onFailure { throw Exception("Failed to upload document") }
                }
                
                // Update Firestore
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "providerVerificationStatus" to "Submitted",
                        "documentUrls" to uploadedUrls,
                        "businessName" to businessName,
                        "gstNumber" to gstNumber
                    )
                ).await()
                
                _status.value = "Submitted"
                onDone(true)
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred during upload."
                onDone(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
