package com.nestmate.app.data.model

data class User(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "student",
    val college: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    
    // Original Profile Fields
    val profilePhotoUrl: String? = null,
    val bio: String = "",
    val phone: String = "",
    val degree: String = "",
    val yearOfStudy: String = "",
    
    // Legacy fields needed by SeedDataUtil / ProfileScreen
    val isVerified: Boolean = false,
    val verificationStatus: String = "PENDING",
    val instagramHandle: String = "",
    val collegeId: String = "",
    val year: String = "",
    val isSenior: Boolean = false,
    val buddyBadge: Boolean = false,
    val gender: String = "OTHER",
    val providerVerificationStatus: String = "PENDING",
    val providerBusinessName: String = "",
    val providerGstNumber: String = "",
    val providerIdDocUrl: String = "",
    val providerSubmittedAt: Long = 0L,
    val serviceType: String = "",



    // ── Phase 4.3: New Profile Fields ───────────────────────────────────────
    val age: Int? = null,
    val courseOrDepartment: String = "",
    
    // Privacy & Visibility Settings (EVERYONE | CONNECTED_ONLY | ONLY_ME)
    val phoneVisibility: String = "ONLY_ME",
    val socialVisibility: String = "EVERYONE",
    val budgetVisibility: String = "ONLY_ME",

    // Roommate matching preferences (Lifestyle)
    val sleepSchedule: String = "NIGHT_OWL",     // EARLY_BIRD | NIGHT_OWL | FLEXIBLE
    val cleanliness: String = "NEAT",            // STRICT | NEAT | CASUAL | MESSY
    val guestPolicy: String = "WEEKENDS_ONLY",   // STRICT | WEEKENDS_ONLY | ANYTIME
    val smokingDrinking: String = "NEITHER",     // NEITHER | SMOKING_ONLY | DRINKING_ONLY | BOTH
    val budgetMin: Long = 0L,                    // in paise
    val budgetMax: Long = 1000000L,              // in paise
    
    // Discovery
    val interestTags: List<String> = emptyList(),// [GAMING, CODING, SPORTS, MUSIC...]

    // Socials
    val instagramUrl: String? = null,
    val linkedinUrl: String? = null,

    // Stats
    val trustRating: Float = 0f,
    val reviewsCount: Int = 0,
    val successfulTrades: Int = 0,
    val activeListingCount: Int = 0,

    // Moderation & Blocks
    val blockedUserIds: List<String> = emptyList(),
    val isBanned: Boolean = false,
    val warningCount: Int = 0,

    // Settings
    val notificationsEnabled: Boolean = true,
    val pushToken: String? = null,

    // Monetisation / Plan hooks (Phase 5.5)
    val subscriptionPlan: String = "FREE",       // FREE | PREMIUM | ALUMNI
    val walletBalance: Long = 0L,                // NestCoins/Tokens
)
