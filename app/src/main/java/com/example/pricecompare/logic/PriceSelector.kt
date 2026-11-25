package com.example.pricecompare.logic

import com.example.pricecompare.model.PriceEntry

// Selects current verified prices and checks whether a price timestamp is still fresh.
// Used by BasketCalculator and price display screens to ignore unverified or rejected data.
object PriceSelector {
    const val FRESH_DAYS = 14
    private const val DAY_MS = 24L * 60 * 60 * 1000

    // Returns true if `createdAt` (UTC milliseconds) is within the 14-day freshness window.
    fun isFresh(createdAt: Long, now: Long = System.currentTimeMillis()): Boolean =
        now - createdAt <= FRESH_DAYS * DAY_MS

    // Returns a map of (productId, shopId) to the newest verified PriceEntry in `prices`.
    // Unverified (flagged outlier) and rejected prices are excluded until confirmed.
    fun latestValid(prices: List<PriceEntry>): Map<Pair<Long, Long>, PriceEntry> =
        prices
            .filter { it.status == "verified" }
            // Group by product and shop so we only keep one latest price per shop for each product.
            .groupBy { Pair(it.productId, it.shopId) }
            .mapValues { (_, list) -> list.maxBy { it.createdAt } }
}