package com.example.pricecompare.logic

import com.example.pricecompare.model.PriceEntry

// Picks which price counts as "current" for each product + shop pair.
object PriceSelector {
    const val FRESH_DAYS = 14
    private const val DAY_MS = 24L * 60 * 60 * 1000

    // Older than 14 days -> the UI greys it out as possibly outdated.
    fun isFresh(createdAt: Long, now: Long = System.currentTimeMillis()): Boolean =
        now - createdAt <= FRESH_DAYS * DAY_MS

    // Latest VERIFIED price for every (productId, shopId).
    // Unverified (flagged outliers) are ignored until confirmed.
    fun latestValid(prices: List<PriceEntry>): Map<Pair<Long, Long>, PriceEntry> =
        prices
            .filter { it.status == "verified" }
            .groupBy { Pair(it.productId, it.shopId) }
            .mapValues { (_, list) -> list.maxBy { it.createdAt } }
}