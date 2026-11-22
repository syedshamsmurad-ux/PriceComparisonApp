package com.example.pricecompare.model

data class BasketResult(
    val shopIds: List<Long>,
    val totalPaisa: Long,
    val assignment: Map<Long, Long>,   // productId -> shopId
    val missingProductIds: List<Long>
)