package com.nestmate.app.ui.screens.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.Inquiry
import com.nestmate.app.data.model.ListingStatus
import com.nestmate.app.data.model.ProviderListing
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.InquiryRepository
import com.nestmate.app.data.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val providerRepository: ProviderRepository,
    private val inquiryRepository: InquiryRepository,
) : ViewModel() {

    private val uid: String? get() = authRepository.getCurrentUserId()

    private val _currentUser = MutableStateFlow<com.nestmate.app.data.model.User?>(null)
    val currentUser: StateFlow<com.nestmate.app.data.model.User?> = _currentUser.asStateFlow()

    private val _userName = MutableStateFlow("Provider")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _verificationStatus = MutableStateFlow("Pending")
    val verificationStatus: StateFlow<String> = _verificationStatus.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    val myListings: StateFlow<List<ProviderListing>> = refreshTrigger.flatMapLatest {
        val ownerId = uid ?: ""
        if (ownerId.isBlank()) flowOf(emptyList())
        else providerRepository.getMyListings(ownerId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val inquiries: StateFlow<List<Inquiry>> = refreshTrigger.flatMapLatest {
        val providerId = uid ?: ""
        if (providerId.isBlank()) flowOf(emptyList())
        else inquiryRepository.getInquiriesForProvider(providerId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadInquiriesCount: StateFlow<Int> = inquiries.map { list ->
        list.count { it.isUnread }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val pendingInquiriesCount: StateFlow<Int> = unreadInquiriesCount

    val activeListingsCount: StateFlow<Int> = myListings.map { list ->
        list.count { it.status == ListingStatus.ACTIVE.name }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val totalViewsThisWeek: StateFlow<Int> = myListings.map { list ->
        list.sumOf { it.viewCount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val recentInquiries: StateFlow<List<Inquiry>> = inquiries.map { list ->
        list.sortedByDescending { it.createdAt }.take(3)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val avgResponseTimeHours: StateFlow<String> = flowOf("—")
        .stateIn(viewModelScope, SharingStarted.Eagerly, "—")

    val listingHealthIssues: StateFlow<List<ListingHealthIssue>> = myListings.combine(inquiries) { listings, inqs ->
        val issues = mutableListOf<ListingHealthIssue>()
        val now = System.currentTimeMillis()
        val days14 = 14 * 24 * 60 * 60 * 1000L
        val days7 = 7 * 24 * 60 * 60 * 1000L

        listings.forEach { listing ->
            if (listing.photoUrls.isEmpty()) {
                issues.add(ListingHealthIssue(listing.id, listing.title, IssueType.NO_PHOTOS))
            }
            if (listing.listingType == com.nestmate.app.data.model.ListingType.FLAT_PG_HOSTEL.name && listing.rentPaise == 0L) {
                issues.add(ListingHealthIssue(listing.id, listing.title, IssueType.NO_PRICE_SET))
            } else if (listing.listingType == com.nestmate.app.data.model.ListingType.MESS.name && listing.monthlyChargePaise == 0L) {
                issues.add(ListingHealthIssue(listing.id, listing.title, IssueType.NO_PRICE_SET))
            }
            if (listing.status == ListingStatus.PAUSED.name && (now - listing.updatedAt) > days14) {
                issues.add(ListingHealthIssue(listing.id, listing.title, IssueType.PAUSED_TOO_LONG))
            }
            if (listing.status == ListingStatus.ACTIVE.name && (now - listing.createdAt) > days7 && listing.inquiryCount == 0) {
                issues.add(ListingHealthIssue(listing.id, listing.title, IssueType.LOW_INQUIRY_RATE))
            }
        }
        issues
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _snackbarEvent = Channel<String>()
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val userId = uid ?: return@launch
            val result = authRepository.getUserData(userId)
            val user = result.getOrNull() ?: return@launch
            _currentUser.value = user
            user.name.takeIf { it.isNotBlank() }?.let { _userName.value = it }
            _verificationStatus.value = user.providerVerificationStatus
        }
    }

    fun refresh() {
        refreshTrigger.value += 1
        loadUserData()
    }

    fun signOut() = authRepository.logout()

    fun deleteListing(listingId: String) {
        viewModelScope.launch { providerRepository.deleteListing(listingId) }
    }

    fun togglePause(listingId: String, currentStatus: String) {
        viewModelScope.launch {
            val result = providerRepository.togglePause(listingId, currentStatus)
            result.onSuccess { newStatus ->
                val msg = if (newStatus == ListingStatus.PAUSED.name) "Listing paused" else "Listing reactivated"
                _snackbarEvent.send(msg)
            }
        }
    }
}
