package com.nestmate.app.data.model

data class BuddyPair(
    val pairId: String = "",
    val seniorId: String = "",
    val juniorId: String = "",
    val collegeId: String = "",
    val status: String = "Pending", // Pending, Active, Declined
    val createdAt: Long = System.currentTimeMillis(),
    val rating: Double = 0.0
)

data class BuddyMessage(
    val messageId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
