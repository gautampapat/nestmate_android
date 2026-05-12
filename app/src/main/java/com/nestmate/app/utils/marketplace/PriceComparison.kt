package com.nestmate.app.utils.marketplace

import com.nestmate.app.data.model.ItemCategory
import com.nestmate.app.data.model.MarketplaceItem

object PriceComparison {

    private const val BELOW_THRESHOLD = 0.85

    fun buildBelowAverageSet(items: List<MarketplaceItem>): Set<String> {
        val byCategory: Map<ItemCategory, List<MarketplaceItem>> = items
            .filter { !it.isBundleListing }
            .groupBy { it.category }

        val result = mutableSetOf<String>()
        for ((_, group) in byCategory) {
            if (group.size < 2) continue
            val totalPrice = group.sumOf { it.price }
            for (item in group) {
                val othersCount = group.size - 1
                if (othersCount == 0) continue
                val othersAvg = (totalPrice - item.price).toDouble() / othersCount.toDouble()
                if (othersAvg > 0 && item.price < othersAvg * BELOW_THRESHOLD) {
                    result += item.id
                }
            }
        }
        return result
    }
}
