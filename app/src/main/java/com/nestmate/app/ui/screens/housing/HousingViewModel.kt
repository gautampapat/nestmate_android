package com.nestmate.app.ui.screens.housing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.ProviderListing
import com.nestmate.app.data.model.Inquiry
import com.nestmate.app.data.model.User
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.InquiryRepository
import com.nestmate.app.data.repository.ProviderRepository
import com.nestmate.app.utils.FirebaseConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HousingFilterState(
    val femaleOnly: Boolean = false,
    val bachelorFriendly: Boolean = false
)

sealed class HousingState {
    object Loading : HousingState()
    data class Success(val listings: List<ProviderListing>, val isPaginating: Boolean = false) : HousingState()
    data class Error(val message: String) : HousingState()
}

sealed class HousingDetailState {
    object Loading : HousingDetailState()
    data class Success(val listing: ProviderListing) : HousingDetailState()
    data class Error(val message: String) : HousingDetailState()
}

@HiltViewModel
class HousingViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val authRepository: AuthRepository,
    private val inquiryRepository: InquiryRepository,
) : ViewModel() {

    private val collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID

    private val _rawListings = MutableStateFlow<List<ProviderListing>>(emptyList())
    private val _filterState = MutableStateFlow(HousingFilterState())
    val filterState: StateFlow<HousingFilterState> = _filterState.asStateFlow()
    
    val uiState: StateFlow<HousingState> = combine(_rawListings, _filterState) { listings, filter ->
        val filtered = listings.filter { listing ->
            (if (filter.femaleOnly) listing.isFemaleOnly else true) &&
                (if (filter.bachelorFriendly) listing.isBachelorFriendly else true)
        }
        HousingState.Success(filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HousingState.Loading)

    private val _detailState = MutableStateFlow<HousingDetailState?>(null)
    val detailState: StateFlow<HousingDetailState?> = _detailState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            providerRepository.getHousingListings(collegeId).collect { listings ->
                _rawListings.value = listings
            }
        }
    }

    fun toggleFemaleOnly() {
        _filterState.value = _filterState.value.copy(femaleOnly = !_filterState.value.femaleOnly)
    }

    fun toggleBachelorFriendly() {
        _filterState.value = _filterState.value.copy(bachelorFriendly = !_filterState.value.bachelorFriendly)
    }

    fun loadListingDetail(listingId: String) {
        _detailState.value = HousingDetailState.Loading
        providerRepository.incrementViewCount(listingId)
        viewModelScope.launch {
            val result = providerRepository.getListingById(listingId)
            _detailState.value = if (result.isSuccess) {
                val listing = result.getOrNull()
                if (listing != null) HousingDetailState.Success(listing)
                else HousingDetailState.Error("Listing not found")
            } else {
                HousingDetailState.Error(result.exceptionOrNull()?.message ?: "Failed to load")
            }
        }
    }

    fun submitInquiry(message: String, listing: ProviderListing, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val userResult = authRepository.getUserData(uid)
            val user = userResult.getOrNull() ?: return@launch

            val inquiry = Inquiry(
                listingId = listing.id,
                listingTitle = listing.title,
                listingType = listing.listingType,
                studentId = uid,
                studentName = user.name,
                studentPhone = user.phone,
                providerId = listing.ownerId,
                message = message,
            )
            val result = inquiryRepository.submitInquiry(inquiry)
            onResult(result.isSuccess)
        }
    }
}
