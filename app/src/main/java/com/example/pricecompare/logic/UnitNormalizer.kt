package com.example.pricecompare.logic

import java.util.Locale

// Makes prices of different pack sizes comparable.
// Rule: packs are stored in base units (g, ml, piece); prices in paisa.
object UnitNormalizer {

    // Returns paisa per 1 kg, 1 litre or 1 piece.
    fun unitPricePaisa(pricePaisa: Long, packSize: Int, unit: String): Double {
        require(packSize > 0) { "packSize must be positive" }
        return if (unit == "piece") {
            pricePaisa.toDouble() / packSize           // paisa per piece
        } else {
            pricePaisa * 1000.0 / packSize             // g or ml -> per kg / per litre
        }
    }

    // Text shown next to the unit price in the UI.
    fun unitLabel(unit: String): String = when (unit) {
        "g" -> "kg"
        "ml" -> "litre"
        else -> "piece"
    }

    // 45000 paisa -> "Rs 450.00". Locale.US keeps the decimal separator a dot.
    fun formatPkr(paisa: Long): String =
        String.format(Locale.US, "Rs %.2f", paisa / 100.0)
}