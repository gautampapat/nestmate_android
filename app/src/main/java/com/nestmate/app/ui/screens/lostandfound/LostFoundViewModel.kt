package com.nestmate.app.ui.screens.lostandfound

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.LostFoundCategory
import com.nestmate.app.data.model.LostFoundItem
import com.nestmate.app.data.model.LostFoundType
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.LostFoundRepository
import com.nestmate.app.utils.FirebaseConstants
import com.nestmate.app.utils.imageupload.CloudinaryUploader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PostingState {
    object Idle : PostingState()
    data class Uploading(val progress: Int) : PostingState()   // 0–100
    object Success : PostingState()
    data class Error(val message: String) : PostingState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LostFoundViewModel @Inject constructor(
    private val repo: LostFoundRepository,
    private val authRepository: AuthRepository,
    private val cloudinaryUploader: CloudinaryUploader,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _selectedType = MutableStateFlow<LostFoundType?>(null)
    val selectedType: StateFlow<LostFoundType?> = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<LostFoundCategory?>(null)
    val selectedCategory: StateFlow<LostFoundCategory?> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _postingState = MutableStateFlow<PostingState>(PostingState.Idle)
    val postingState: StateFlow<PostingState> = _postingState.asStateFlow()

    private val currentUserFlow = authRepository.observeCurrentUser()
    private val currentUserIdFlow = currentUserFlow.map { it?.userId }.distinctUntilChanged()

    private val refreshTrigger = MutableStateFlow(0)

    val feedItems: StateFlow<List<LostFoundItem>> = combine(
        _selectedType,
        _selectedCategory,
        refreshTrigger
    ) { type, category, _ ->
        Pair(type, category)
    }.flatMapLatest { (type, category) ->
        _isLoading.value = true
        repo.getItems(
            collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID,
            type = type,
            category = category
        ).onEach { 
            _isLoading.value = false 
        }.catch { e ->
            _isLoading.value = false
            _errorMessage.value = e.message ?: "Failed to load items"
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myItems: StateFlow<List<LostFoundItem>> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList<LostFoundItem>())
        else repo.getMyItems(userId).catch { emit(emptyList<LostFoundItem>()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTypeFilter(type: LostFoundType?) {
        _selectedType.value = type
        if (type == null) {
            _selectedCategory.value = null
        }
    }

    fun setCategoryFilter(category: LostFoundCategory?) {
        _selectedCategory.value = category
    }

    fun refreshFeed() {
        refreshTrigger.value += 1
    }

    fun postItem(
        type: LostFoundType,
        title: String,
        description: String,
        category: LostFoundCategory,
        location: String,
        imageUris: List<Uri>,
        contactPreference: String,
        contactDetail: String
    ) {
        viewModelScope.launch {
            _postingState.value = PostingState.Uploading(0)
            
            val user = currentUserFlow.firstOrNull()
            if (user == null) {
                _postingState.value = PostingState.Error("User not logged in")
                return@launch
            }

            val photoUrls = mutableListOf<String>()
            try {
                if (imageUris.isNotEmpty()) {
                    val progressStep = 100 / imageUris.size
                    for ((index, uri) in imageUris.withIndex()) {
                        val bytes = com.nestmate.app.utils.imageupload.ImageCompressor.compress(uri, context)
                        if (bytes == null) throw Exception("Failed to compress image")
                        val result = cloudinaryUploader.uploadCompressed(bytes, "nestmate/lost_found/${user.userId}")
                        if (result.isSuccess) {
                            photoUrls.add(result.getOrNull() ?: "")
                            _postingState.value = PostingState.Uploading((index + 1) * progressStep)
                        } else {
                            throw Exception("Failed to upload image ${index + 1}")
                        }
                    }
                }

                val item = LostFoundItem(
                    type = type,
                    title = title,
                    description = description,
                    category = category,
                    location = location,
                    photoUrls = photoUrls.filter { it.isNotBlank() },
                    reportedByUserId = user.userId,
                    reportedByName = user.name,
                    reportedByPhotoUrl = user.profilePhotoUrl,
                    collegeId = if (user.collegeId.isNotEmpty()) user.collegeId else FirebaseConstants.DEFAULT_COLLEGE_ID,
                    contactPreference = contactPreference,
                    contactDetail = contactDetail
                )

                repo.postItem(item).onSuccess {
                    _postingState.value = PostingState.Success
                }.onFailure { e ->
                    _postingState.value = PostingState.Error(e.message ?: "Failed to post item")
                }
            } catch (e: Exception) {
                _postingState.value = PostingState.Error(e.message ?: "An error occurred during upload")
            }
        }
    }

    fun markResolved(itemId: String) {
        viewModelScope.launch {
            repo.markResolved(itemId).onFailure { e ->
                _errorMessage.value = e.message ?: "Failed to mark as resolved"
            }
        }
    }

    fun submitClaim(itemId: String, message: String) {
        viewModelScope.launch {
            val user = currentUserFlow.firstOrNull() ?: return@launch
            repo.submitClaim(
                itemId = itemId,
                claimantId = user.userId,
                claimantName = user.name.ifBlank { "Anonymous" },
                message = message
            ).onFailure { e ->
                _errorMessage.value = e.message ?: "Failed to submit message"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
    
    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repo.deleteItem(itemId).onFailure { e ->
                 _errorMessage.value = e.message ?: "Failed to delete item"
            }
        }
    }
    
    fun incrementViewCount(itemId: String) {
        repo.incrementViewCount(itemId)
    }

    fun resetPostingState() {
        _postingState.value = PostingState.Idle
    }
}
