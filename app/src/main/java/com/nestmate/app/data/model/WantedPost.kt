package com.nestmate.app.data.model

data class WantedPost(
    val id: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val itemDescription: String = "",
    val maxBudget: Long = 0L,
    val category: ItemCategory? = null,
    val status: WantedStatus = WantedStatus.OPEN,
    val createdAt: Long = 0L,
)
