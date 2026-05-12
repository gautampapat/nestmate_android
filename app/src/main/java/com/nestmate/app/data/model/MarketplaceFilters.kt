package com.nestmate.app.data.model

data class MarketplaceFilters(
    val category: ItemCategory? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val condition: ItemCondition? = null,
    val searchQuery: String? = null,
    val savedOnly: Boolean = false,
)
