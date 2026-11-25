package com.example.pricecompare.model

// One catalogue item (a product in one pack size). Plain data, no Android code.
// packSize is in base units: grams, millilitres or pieces. unit is "g", "ml" or "piece".
data class Product(
    val id: Long, val name: String, val category: String,
    val brand: String?, val packSize: Int, val unit: String
)