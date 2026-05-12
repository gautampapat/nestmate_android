package com.nestmate.app.data.model

data class Restaurant(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val photoUrls: List<String> = emptyList(),
    val priceLevel: String = "MODERATE",    // BUDGET | MODERATE | PREMIUM
    val distanceKm: Float = 0f,
    val overallRating: Float = 0f,
    val ratingCount: Int = 0,
    val foodType: String = "BOTH",           // VEG | NON_VEG | BOTH
    val category: String = "OTHER",          // FAST_FOOD | THALI | CAFE | BIRYANI | SOUTH_INDIAN | SNACKS | OTHER
    val mealTimes: List<String> = emptyList(), // BREAKFAST | LUNCH | DINNER
    val openingHours: String = "",
    val studentTags: List<String> = emptyList(), // BUDGET_FRIENDLY | GROUP_HANGOUT | LATE_NIGHT | QUICK_BITES | STUDY_FRIENDLY
    val activeDiscounts: List<Map<String, String>> = emptyList(), // each: {title, validUntil?}
    val googleMapsLink: String = "",
    val isPromoted: Boolean = false,
    val isTrusted: Boolean = false,
    val visitCount: Int = 0,
    // Monetisation hooks (Phase 5.5)
    val isPartner: Boolean = false,
    val discountCode: String? = null,
)
