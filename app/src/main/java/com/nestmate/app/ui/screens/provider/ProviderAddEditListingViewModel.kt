package com.nestmate.app.ui.screens.provider

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.ListingType
import com.nestmate.app.data.model.ProviderListing
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.ProviderRepository
import com.nestmate.app.utils.FirebaseConstants
import com.nestmate.app.utils.imageupload.CloudinaryUploader
import com.nestmate.app.utils.imageupload.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Draft state — all form fields, mutable, nullable-safe
// ─────────────────────────────────────────────────────────────────────────────
data class ListingDraft(
    val title: String = "",
    val description: String = "",
    val address: String = "",
    val googleMapsLink: String = "",
    // Housing-specific
    val rentRupees: String = "",
    val depositRupees: String = "",
    val bhkType: String = "",
    val isBachelorFriendly: Boolean = false,
    val isFemaleOnly: Boolean = false,
    // Mess-specific
    val monthlyChargeRupees: String = "",
    val isVegOnly: Boolean = false,
    val trialAvailable: Boolean = false,
    val menuText: String = "",
    // Service-specific
    val priceDescription: String = "",
    val timings: String = "",
    val specialisation: String = "",
)

// ─────────────────────────────────────────────────────────────────────────────
// Photo upload slot state
// ─────────────────────────────────────────────────────────────────────────────
sealed class PhotoUploadState {
    object Idle : PhotoUploadState()
    object Uploading : PhotoUploadState()
    data class Success(val url: String) : PhotoUploadState()
    data class Failed(val error: String) : PhotoUploadState()
}

@HiltViewModel
class ProviderAddEditListingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val providerRepository: ProviderRepository,
    private val messRepository: com.nestmate.app.data.repository.MessRepository,
    private val cloudinaryUploader: CloudinaryUploader,
    private val okHttpClient: OkHttpClient,
) : ViewModel() {

    private val _draft = MutableStateFlow(ListingDraft())
    val draft: StateFlow<ListingDraft> = _draft.asStateFlow()

    private val _photoUploadStates = MutableStateFlow<Map<Int, PhotoUploadState>>(emptyMap())
    val photoUploadStates: StateFlow<Map<Int, PhotoUploadState>> = _photoUploadStates.asStateFlow()

    private val _parsedCoords = MutableStateFlow<Pair<Double, Double>?>(null)
    val parsedCoords: StateFlow<Pair<Double, Double>?> = _parsedCoords.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private var currentListingType = "FLAT_PG_HOSTEL"
    private var editingListingId: String? = null
    private var nextSlot = 0

    fun init(listingType: String, listingId: String?) {
        currentListingType = listingType
        editingListingId = listingId
        if (listingId != null) {
            viewModelScope.launch {
                val result = providerRepository.getListingById(listingId)
                val listing = result.getOrNull() ?: return@launch
                _draft.value = listing.toDraft()
                // Restore photo URLs into slot states
                val states = listing.photoUrls.mapIndexed { idx, url ->
                    idx to PhotoUploadState.Success(url)
                }.toMap()
                _photoUploadStates.value = states
                nextSlot = listing.photoUrls.size
            }
        } else {
            // Nothing to pre-fill
        }
    }

    fun updateDraft(updated: ListingDraft) { _draft.value = updated }

    fun pickAndUploadPhoto(context: Context, uri: Uri) {
        val slot = nextSlot
        if (slot >= 6) return
        nextSlot++      

        _photoUploadStates.value = _photoUploadStates.value + (slot to PhotoUploadState.Uploading)

        viewModelScope.launch {
            try {
                val bytes = ImageCompressor.compress(uri, context)
                val uid = authRepository.getCurrentUserId() ?: "unknown"
                val result = cloudinaryUploader.uploadCompressed(bytes, "nestmate/listings/$uid")
                result.fold(
                    onSuccess = { url ->
                        _photoUploadStates.value = _photoUploadStates.value + (slot to PhotoUploadState.Success(url))
                    },
                    onFailure = { err ->
                        _photoUploadStates.value = _photoUploadStates.value + (slot to PhotoUploadState.Failed(err.message ?: "Upload failed"))
                        nextSlot-- // reclaim the slot
                    },
                )
            } catch (e: Exception) {
                _photoUploadStates.value = _photoUploadStates.value + (slot to PhotoUploadState.Failed(e.message ?: "Compress failed"))
                nextSlot--
            }
        }
    }

    fun removePhoto(slot: Int) {
        val updated = _photoUploadStates.value.toMutableMap()
        updated.remove(slot)
        _photoUploadStates.value = updated
        nextSlot = (nextSlot - 1).coerceAtLeast(0)
    }

    /** Parses lat/lng from a Google Maps share link via HTTP redirect. */
    fun parseMapsLink(url: String) {
        viewModelScope.launch {
            val coords = withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder().url(url).head().build()
                    val resp = okHttpClient.newCall(req).execute()
                    val finalUrl = resp.request.url.toString()
                    // Extract @lat,lng from the redirect URL
                    val match = Regex("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)").find(finalUrl)
                    if (match != null) {
                        val lat = match.groupValues[1].toDoubleOrNull()
                        val lng = match.groupValues[2].toDoubleOrNull()
                        if (lat != null && lng != null) Pair(lat, lng) else null
                    } else null
                } catch (e: Exception) { null }
            }
            _parsedCoords.value = coords
        }
    }

    fun saveListing() {
        val draft = _draft.value
        if (draft.title.isBlank() || draft.address.isBlank()) {
            _saveError.value = "Title and address are required."
            return
        }
        _isSaving.value = true
        _saveError.value = null

        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: run {
                _saveError.value = "Not authenticated."
                _isSaving.value = false
                return@launch
            }
            val userData = authRepository.getUserData(uid).getOrNull()

            val photoUrls = _photoUploadStates.value.entries
                .sortedBy { it.key }
                .mapNotNull { (_, state) -> (state as? PhotoUploadState.Success)?.url }

            val coords = _parsedCoords.value
            val listing = ProviderListing(
                id = editingListingId ?: "",
                ownerId = uid,
                ownerName = userData?.name ?: "",
                listingType = currentListingType,
                title = draft.title,
                description = draft.description,
                photoUrls = photoUrls,
                address = draft.address,
                googleMapsLink = draft.googleMapsLink,
                latitude = coords?.first ?: 0.0,
                longitude = coords?.second ?: 0.0,
                collegeId = userData?.collegeId ?: FirebaseConstants.DEFAULT_COLLEGE_ID,
                meta = draft.toMeta(currentListingType),
            )

            val result = if (editingListingId == null) {
                providerRepository.createListing(listing)
            } else {
                providerRepository.updateListing(listing).map { editingListingId!! }
            }

            result.fold(
                onSuccess = { newId ->
                    if (currentListingType == "MESS") {
                        val mess = com.nestmate.app.data.model.Mess(
                            messId = newId,
                            name = listing.title,
                            address = listing.address,
                            lat = listing.latitude,
                            lng = listing.longitude,
                            collegeId = listing.collegeId,
                            ownerId = listing.ownerId,
                            vegNonVeg = if (draft.isVegOnly) "Veg" else "Veg & Non-Veg",
                            photoUrls = listing.photoUrls,
                            pricing = mapOf("Monthly" to (draft.monthlyChargeRupees.toIntOrNull() ?: 0)),
                            menu = mapOf("Today" to draft.menuText)
                        )
                        launch {
                            messRepository.createOrUpdateMess(mess)
                        }
                    }
                    _saveSuccess.value = true 
                },
                onFailure = { _saveError.value = it.message ?: "Save failed." },
            )
            _isSaving.value = false
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension mappers
// ─────────────────────────────────────────────────────────────────────────────

private fun ProviderListing.toDraft() = ListingDraft(
    title = title,
    description = description,
    address = address,
    googleMapsLink = googleMapsLink,
    rentRupees = if (rentPaise > 0) (rentPaise / 100).toString() else "",
    depositRupees = if (depositPaise > 0) (depositPaise / 100).toString() else "",
    bhkType = bhkType,
    isBachelorFriendly = isBachelorFriendly,
    isFemaleOnly = isFemaleOnly,
    monthlyChargeRupees = if (monthlyChargePaise > 0) (monthlyChargePaise / 100).toString() else "",
    isVegOnly = isVegOnly,
    trialAvailable = (meta["trialAvailable"] as? Boolean) ?: false,
    menuText = (meta["menu"] as? String) ?: "",
    priceDescription = priceDescription,
    timings = timings,
    specialisation = (meta["specialisation"] as? String) ?: "",
)

private fun ListingDraft.toMeta(listingType: String): Map<String, Any> = when (listingType) {
    "FLAT_PG_HOSTEL" -> buildMap {
        put("rent", (rentRupees.toLongOrNull() ?: 0L) * 100L)
        put("deposit", (depositRupees.toLongOrNull() ?: 0L) * 100L)
        put("bhkType", bhkType)
        put("isBachelorFriendly", isBachelorFriendly)
        put("isFemaleOnly", isFemaleOnly)
    }
    "MESS" -> buildMap {
        put("monthlyCharge", (monthlyChargeRupees.toLongOrNull() ?: 0L) * 100L)
        put("isVegOnly", isVegOnly)
        put("trialAvailable", trialAvailable)
        put("menu", menuText)
    }
    else -> buildMap {
        put("priceDescription", priceDescription)
        put("timings", timings)
        put("specialisation", specialisation)
    }
}
