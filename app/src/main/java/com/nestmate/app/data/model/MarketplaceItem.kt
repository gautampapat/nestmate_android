package com.nestmate.app.data.model

data class MarketplaceItem(
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val title: String = "",
    val description: String = "",
    val price: Long = 0L,
    val isNegotiable: Boolean = false,
    val condition: ItemCondition = ItemCondition.USED,
    val category: ItemCategory = ItemCategory.MISCELLANEOUS,
    val photoUrls: List<String> = emptyList(),
    val tags: List<ItemTag> = emptyList(),
    val status: ItemStatus = ItemStatus.ACTIVE,
    val isBundleListing: Boolean = false,
    val bundleItemIds: List<String> = emptyList(),
    val bundleExpiryDate: Long? = null,
    val viewCount: Long = 0L,
    val saveCount: Long = 0L,
    val isPromoted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
