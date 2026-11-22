package com.example.pricecompare.logic

import com.example.pricecompare.model.Shop
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    fun sortByDistance(shops: List<Shop>, lat: Double, lng: Double): List<Pair<Shop, Double>> =
        shops.map { it to haversineKm(lat, lng, it.lat, it.lng) }.sortedBy { it.second }

    // Temporary check: delete after it passes
    fun demo(): String {
        val same = haversineKm(24.0, 67.0, 24.0, 67.0)
        val oneDeg = haversineKm(24.0, 67.0, 25.0, 67.0)
        val shops = listOf(
            Shop(1, "Far", null, null, 25.0, 67.0),
            Shop(2, "Near", null, null, 24.1, 67.0)
        )
        val sorted = sortByDistance(shops, 24.0, 67.0)
            .joinToString { "${it.first.name}=${"%.2f".format(it.second)}" }
        return "same=%.2f, 1deg=%.2f, sorted: %s".format(same, oneDeg, sorted)
    }
}