package com.nestmate.app.data.model

enum class ListingType {
    FLAT_PG_HOSTEL, MESS, LAUNDRY, XEROX, GROCERY, TUTOR, OTHER_SERVICE
}

enum class ListingStatus {
    ACTIVE, PAUSED, DELETED
}

/**
 * Unified provider listing document stored at providerListings/{listingId}.
 *
 * Type-specific fields are stored in [meta] as a Map<String, Any> for schema flexibility.
 * Documented [meta] keys per type:
 *
 * FLAT_PG_HOSTEL:
 *   rent (Long paise), deposit (Long paise), bhkType (String e.g. "1BHK"),
 *   sharingType (String e.g. "Single/Double/Triple"), isBachelorFriendly (Boolean),
 *   isFemaleOnly (Boolean), availableFrom (Long epoch ms), amenities (List<String>)
 *
 * MESS:
 *   monthlyCharge (Long paise), mealTypes (List<String> e.g. ["Lunch","Dinner"]),
 *   isVegOnly (Boolean), capacity (Long), trialAvailable (Boolean), menu (String)
 *
 * Services (LAUNDRY, XEROX, GROCERY, TUTOR, OTHER_SERVICE):
 *   priceDescription (String), timings (String), specialisation (String — Tutor only)
 */
// PRIVACY: never expose ownerPhone in listing documents
data class ProviderListing(
    val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val listingType: String = ListingType.FLAT_PG_HOSTEL.name,
    val title: String = "",
    val description: String = "",
    val photoUrls: List<String> = emptyList(),   // Cloudinary secure_urls
    val address: String = "",
    val googleMapsLink: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = ListingStatus.ACTIVE.name,
    val isVerifiedByAdmin: Boolean = false,
    val viewCount: Int = 0,
    val inquiryCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val collegeId: String = "",
    val realityScore: Double = 0.0,
    val greenScore: Double = 0.0,
    val meta: Map<String, Any> = emptyMap(),
) {
    // Convenience accessors for common meta fields
    val rentPaise: Long get() = (meta["rent"] as? Long) ?: 0L
    val rentRupees: Long get() = rentPaise / 100L
    val depositPaise: Long get() = (meta["deposit"] as? Long) ?: 0L
    val bhkType: String get() = (meta["bhkType"] as? String) ?: ""
    val isFemaleOnly: Boolean get() = (meta["isFemaleOnly"] as? Boolean) ?: false
    val isBachelorFriendly: Boolean get() = (meta["isBachelorFriendly"] as? Boolean) ?: false
    val monthlyChargePaise: Long get() = (meta["monthlyCharge"] as? Long) ?: 0L
    val monthlyChargeRupees: Long get() = monthlyChargePaise / 100L
    val isVegOnly: Boolean get() = (meta["isVegOnly"] as? Boolean) ?: false
    val priceDescription: String get() = (meta["priceDescription"] as? String) ?: ""
    val timings: String get() = (meta["timings"] as? String) ?: ""
    val firstPhotoUrl: String get() = photoUrls.firstOrNull() ?: ""
    val isActive: Boolean get() = status == ListingStatus.ACTIVE.name
    val isPaused: Boolean get() = status == ListingStatus.PAUSED.name
}
