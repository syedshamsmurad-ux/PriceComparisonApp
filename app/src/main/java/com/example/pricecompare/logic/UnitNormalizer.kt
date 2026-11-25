package com.example.pricecompare.logic

import java.util.Locale

// Converts pack prices into comparable unit prices and formats paisa amounts as PKR text.
// Used by price lists, outlier detection, and basket screens across the app.
object UnitNormalizer {

    // Returns the unit price in paisa per 1 kg, 1 litre, or 1 piece as a Double.
    fun unitPricePaisa(pricePaisa: Long, packSize: Int, unit: String): Double {
        // Prevent division by zero when calculating the per-unit price.
        require(packSize > 0) { "packSize must be positive" }
        // Multiplies g or ml by 1000.0 to scale from grams/millilitres up to kg/litre.
        return when (unit) {
            "piece" -> pricePaisa.toDouble() / packSize           // paisa per piece
            "g", "ml" -> pricePaisa * 1000.0 / packSize           // g or ml -> per kg / per litre
            else -> throw IllegalArgumentException("Unsupported unit: $unit")
        }
    }

    // Returns the display label ("kg", "litre", or "piece") matching unitPricePaisa().
    fun unitLabel(unit: String): String = when (unit) {
        "g" -> "kg"
        "ml" -> "litre"
        "piece" -> "piece"
        else -> throw IllegalArgumentException("Unsupported unit: $unit")
    }

    // Formats an integer paisa amount (100 paisa = 1 Rs) into a string like "Rs 450.00".
    // Uses Locale.US so the decimal point is always a dot regardless of phone language.
    fun formatPkr(paisa: Long): String =
        String.format(Locale.US, "Rs %.2f", paisa / 100.0)
}