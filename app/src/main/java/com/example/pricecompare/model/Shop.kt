package com.example.pricecompare.model

// One shop with its map position. Plain data, no Android code.
// lat and lng are in degrees; GeoUtils uses them to measure distance to the user.
data class Shop(
    val id: Long, val name: String, val type: String?,
    val address: String?, val lat: Double, val lng: Double
)