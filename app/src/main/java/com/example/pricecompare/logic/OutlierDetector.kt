package com.example.pricecompare.logic

import kotlin.math.abs

// Flags a new price that is far from the recent median.
object OutlierDetector {
    const val THRESHOLD = 0.40      // 40% rule from the proposal
    const val MIN_SAMPLES = 3       // too few prices -> nothing to compare against

    // Middle value of the list (average of the two middle values if even).
    fun median(values: List<Double>): Double {
        require(values.isNotEmpty()) { "median of empty list" }
        val s = values.sorted()
        val mid = s.size / 2
        return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2.0
    }

    // Pass UNIT prices (per kg/litre/piece), so a 5 kg pack isn't judged against a 1 kg pack.
    fun isOutlier(newUnitPrice: Double, recentUnitPrices: List<Double>): Boolean {
        if (recentUnitPrices.size < MIN_SAMPLES) return false
        val m = median(recentUnitPrices)
        if (m <= 0.0) return false
        return abs(newUnitPrice - m) / m > THRESHOLD
    }
}