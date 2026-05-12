package com.nestmate.app.data.model

data class RideReview(
    val id: String = "",
    val rideId: String = "",
    val reviewerId: String = "",
    val targetId: String = "",
    val rating: Float = 0f,
    val comment: String? = null,
    val createdAt: Long = 0L,
)
