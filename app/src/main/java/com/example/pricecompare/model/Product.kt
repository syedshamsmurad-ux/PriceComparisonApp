package com.example.pricecompare.model

data class Product(
    val id: Long, val name: String, val category: String,
    val brand: String?, val packSize: Int, val unit: String
)