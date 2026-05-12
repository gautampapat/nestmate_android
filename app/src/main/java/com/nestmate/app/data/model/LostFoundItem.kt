package com.nestmate.app.data.model

data class LostFoundItem(
    val id: String = "",
    val type: LostFoundType = LostFoundType.LOST,       // LOST or FOUND
    val title: String = "",                              // e.g. "Blue Water Bottle"
    val description: String = "",                        // details, identifying marks
    val category: LostFoundCategory = LostFoundCategory.OTHER,
    val location: String = "",                           // e.g. "Library 2nd floor", "Canteen"
    val photoUrls: List<String> = emptyList(),           // Cloudinary URLs, max 3
    val reportedByUserId: String = "",
    val reportedByName: String = "",
    val reportedByPhotoUrl: String? = null,
    val collegeId: String = "",
    val status: LostFoundStatus = LostFoundStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolvedWithUserId: String? = null,              // who found/returned it
    val contactPreference: String = "IN_APP",            // IN_APP | PHONE | EMAIL
    val contactDetail: String = "",                      // filled if PHONE or EMAIL preference
    val viewCount: Int = 0,
    val claimCount: Int = 0,                             // how many people said "I found this"
)

enum class LostFoundType { LOST, FOUND }

enum class LostFoundStatus { OPEN, RESOLVED, EXPIRED }

enum class LostFoundCategory {
    ELECTRONICS,      // phones, earbuds, chargers, laptops
    ID_CARDS,         // college ID, PAN, Aadhaar
    KEYS,
    CLOTHING,         // jackets, bags, umbrellas
    BOOKS_NOTES,
    WATER_BOTTLES,
    STATIONERY,
    JEWELLERY,
    WALLET_CARDS,
    OTHER
}
