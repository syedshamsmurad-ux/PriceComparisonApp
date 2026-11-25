package com.example.pricecompare.model

// Outcome of the basket calculation for one shop or one group of shops.
// Produced by BasketCalculator and shown by BasketResultActivity.
data class BasketResult(
    // Shops that actually supply at least one item.
    val shopIds: List<Long>,
    // Total cost in paisa, for the items that have a price.
    val totalPaisa: Long,
    val assignment: Map<Long, Long>,   // productId -> shopId
    // Items on the list that no shop in this group has a valid price for.
    val missingProductIds: List<Long>
)