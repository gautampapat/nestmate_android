package com.nestmate.app.data.model

data class MarketplaceChat(
    val id: String = "",
    val contextType: ChatContextType = ChatContextType.ITEM,
    val itemId: String? = null,
    val itemTitle: String = "",
    val wantedPostId: String? = null,
    val buyerId: String = "",
    val sellerId: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastReadBy: Map<String, Long> = emptyMap(),
    val isItemSold: Boolean = false,
)
