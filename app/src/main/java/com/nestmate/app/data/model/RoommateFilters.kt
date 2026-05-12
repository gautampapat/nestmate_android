package com.nestmate.app.data.model

data class RoommateFilters(
    val gender: Gender? = null,
    val minBudget: Long? = null,
    val maxBudget: Long? = null,
    val roomType: RoomType? = null,
    val location: String? = null,
)
