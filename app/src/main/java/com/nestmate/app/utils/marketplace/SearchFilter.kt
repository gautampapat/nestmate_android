package com.nestmate.app.utils.marketplace

import com.nestmate.app.data.model.MarketplaceItem

object SearchFilter {

    fun apply(items: List<MarketplaceItem>, query: String?): List<MarketplaceItem> {
        val q = query?.trim().orEmpty()
        if (q.isEmpty()) return items
        return items.filter { item ->
            item.title.contains(q, ignoreCase = true) ||
                item.description.contains(q, ignoreCase = true) ||
                item.category.label.contains(q, ignoreCase = true)
        }
    }
}
