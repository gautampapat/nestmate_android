package com.nestmate.app.data.model

data class SanitisedUserProfile(
    val userId: String,
    val name: String,
    val college: String,
    val role: String,
    val profilePhotoUrl: String?,
    val bio: String,
    val courseOrDepartment: String,
    val yearOfStudy: String,
    val age: Int?,
    val trustRating: Float,
    val successfulTrades: Int,
    val interestTags: List<String>,
    
    val isVerified: Boolean,
    val providerVerificationStatus: String,

    // Controlled by visibility rules
    val phone: String?,
    val instagramUrl: String?,
    val linkedinUrl: String?,
    val sleepSchedule: String?,
    val cleanliness: String?,
    val guestPolicy: String?,
    val smokingDrinking: String?,
    val budgetMin: Long?,
    val budgetMax: Long?
)
