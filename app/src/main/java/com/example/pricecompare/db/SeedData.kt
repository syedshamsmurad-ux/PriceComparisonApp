package com.example.pricecompare.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

// Loads initial sample shops, products, and prices from assets/*.csv into SQLite.
// Called once by DatabaseHelper.onCreate() when the database is first created.
object SeedData {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    // Reads shops.csv, products.csv, and prices.csv in order so foreign keys resolve.
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
        // Convert each row's days_ago into a UTC millisecond timestamp relative to right now.
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

    // Opens an asset CSV file, skips the header and blank lines, and parses each row.
    private fun csv(ctx: Context, file: String): List<List<String>> =
        ctx.assets.open(file).bufferedReader().use { it.readLines() }
            .drop(1)
            .filter { it.isNotBlank() }
            .map { parseCsvLine(it) }

    // Splits one CSV line on commas while keeping commas inside double quotes intact.
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                // Escaped quote ("") inside a quoted field writes one literal quote.
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    cur.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    fields.add(cur.toString().trim())
                    cur.clear()
                }
                else -> cur.append(ch)
            }
            i++
        }
        fields.add(cur.toString().trim())
        return fields
    }
}