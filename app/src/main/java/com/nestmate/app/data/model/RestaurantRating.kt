package com.nestmate.app.data.model

data class RestaurantRating(
    val id: String = "",
    val restaurantId: String = "",
    val userId: String = "",
    val rating: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
)
