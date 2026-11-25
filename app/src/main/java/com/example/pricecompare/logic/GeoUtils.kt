package com.example.pricecompare.logic

import com.example.pricecompare.model.Shop
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Calculates straight-line distances on Earth between GPS coordinates and sorts shops.
// Used by NearbyShopsActivity to show shops ordered from nearest to farthest.
object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    // Returns the great-circle distance in kilometres between two lat/lng points.
    // Uses the Haversine formula, which accounts for Earth's spherical curve.
    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        // Convert degree differences to radians because trig functions expect radians.
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        // Haversine term `a` is the square of half the chord length between the points.
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        // Clamp `a` to [0.0, 1.0] so floating-point rounding never causes asin() to return NaN.
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    // Pairs each Shop with its distance in km from (lat, lng), sorted nearest first.
    fun sortByDistance(shops: List<Shop>, lat: Double, lng: Double): List<Pair<Shop, Double>> =
        shops.map { it to haversineKm(lat, lng, it.lat, it.lng) }.sortedBy { it.second }
}