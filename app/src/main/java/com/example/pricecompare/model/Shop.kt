package com.example.pricecompare.model

data class Shop(
    val id: Long, val name: String, val type: String?,
    val address: String?, val lat: Double, val lng: Double
)