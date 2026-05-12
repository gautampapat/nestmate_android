package com.nestmate.app.data.model

import com.google.firebase.Timestamp

data class Mess(
    val messId: String = "",
    val name: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val collegeId: String = "",
    val ownerId: String = "",
    val vegNonVeg: String = "Veg", // Veg, Non-Veg, Both
    val menu: Map<String, String> = emptyMap(),
    val pricing: Map<String, Int> = emptyMap(),
    val timings: Map<String, String> = emptyMap(),
    val crowdLevel: String = "low", // low, medium, high — derived from votes
    val crowdVotesLow: Int = 0,
    val crowdVotesMedium: Int = 0,
    val crowdVotesHigh: Int = 0,
    val isActive: Boolean = true,
    val photoUrls: List<String> = emptyList(),
    val rating: Double = 0.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val derivedCrowdLevel: String
        get() {
            val max = maxOf(crowdVotesHigh, crowdVotesMedium, crowdVotesLow)
            return when {
                max == 0 -> "low" // Or crowdLevel if we want fallback
                max == crowdVotesHigh -> "high"
                max == crowdVotesMedium -> "medium"
                else -> "low"
            }
        }
}
