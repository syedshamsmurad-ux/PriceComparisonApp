package com.example.pricecompare.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

// Fills an empty database from CSV files in assets/. Runs once, inside onCreate.
object SeedData {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun load(ctx: Context, db: SQLiteDatabase) {
        // shops.csv columns: name,type,address,lat,lng
        csv(ctx, "shops.csv").forEach { c ->
            db.insert("shops", null, ContentValues().apply {
                put("name", c[0]); put("type", c[1]); put("address", c[2])
                put("lat", c[3].toDouble()); put("lng", c[4].toDouble())
            })
        }
        // products.csv columns: name,category,brand,pack_size,unit
        csv(ctx, "products.csv").forEach { c ->
            db.insert("products", null, ContentValues().apply {
                put("name", c[0])
                put("name_norm", c[0].lowercase())   // lowercase copy used by search
                put("category", c[1]); put("brand", c[2])
                put("pack_size", c[3].toInt()); put("unit", c[4])
            })
        }
        // prices.csv columns: product_id,shop_id,price_paisa,pack_size,days_ago
        val now = System.currentTimeMillis()
        csv(ctx, "prices.csv").forEach { c ->
            db.insert("prices", null, ContentValues().apply {
                put("product_id", c[0].toLong()); put("shop_id", c[1].toLong())
                put("price_paisa", c[2].toLong()); put("pack_size", c[3].toInt())
                put("created_at", now - c[4].toLong() * DAY_MS)   // UTC millis
                put("status", "verified")
                put("sync_status", 1)   // seed rows must never be uploaded as user submissions
            })
        }
    }

    // Reads a CSV from assets: skips the header and blank lines, splits on commas.
    private fun csv(ctx: Context, file: String): List<List<String>> =
        ctx.assets.open(file).bufferedReader().use { it.readLines() }
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line -> line.split(",").map { it.trim() } }
}