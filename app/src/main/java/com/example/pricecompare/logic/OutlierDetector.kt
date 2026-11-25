package com.example.pricecompare.logic

import kotlin.math.abs

// Checks whether a newly submitted price looks unusually high or low compared to others.
// Used when submitting prices so suspicious entries are marked unverified until voted on.
object OutlierDetector {
    const val THRESHOLD = 0.40      // 40% rule from the proposal
    const val MIN_SAMPLES = 3       // too few prices -> nothing to compare against

    // Returns the middle value of `values`, or the average of the two middle values if even.
    fun median(values: List<Double>): Double {
        // Fail fast on an empty list because a median is undefined with zero values.
        require(values.isNotEmpty()) { "median of empty list" }
        val s = values.sorted()
        val mid = s.size / 2
        return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2.0
    }

    // Returns true if `newUnitPrice` differs from the median by more than 40%.
    // Expects unit prices (per kg, litre, or piece) so different pack sizes compare fairly.
    fun isOutlier(newUnitPrice: Double, recentUnitPrices: List<Double>): Boolean {
        // Skip outlier detection until enough recent prices exist to form a reliable median.
        if (recentUnitPrices.size < MIN_SAMPLES) return false
        val m = median(recentUnitPrices)
        // Avoid dividing by zero if bad data produces a non-positive median.
        if (m <= 0.0) return false
        return abs(newUnitPrice - m) / m > THRESHOLD
    }
}