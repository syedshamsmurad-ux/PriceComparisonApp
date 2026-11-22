package com.example.pricecompare.db

import android.content.Context
import android.database.DatabaseUtils
import com.example.pricecompare.logic.GeoUtils
import com.example.pricecompare.logic.UnitNormalizer

// Temporary check of the data layer. Compare Logcat with the expected lines.
object Phase2Demo {
    fun run(ctx: Context): List<String> {
        val db = DatabaseHelper.get(ctx).readableDatabase   // first call creates + seeds the DB
        val products = ProductDao(ctx)
        val shops = ShopDao(ctx)
        val prices = PriceDao(ctx)
        val out = mutableListOf<String>()

        out += "counts: shops=${DatabaseUtils.queryNumEntries(db, "shops")} " +
                "products=${DatabaseUtils.queryNumEntries(db, "products")} " +
                "prices=${DatabaseUtils.queryNumEntries(db, "prices")}"
        out += "categories: ${products.categories()}"
        out += "search rice: ${products.search("rice", null).size}, " +
                "search oil in Staples: ${products.search("oil", "Staples").size}, " +
                "search %: ${products.search("%", null).size}"
        out += "product1 shopOrder: ${prices.byProduct(1).map { it.shopId }}"

        val p = prices.byProduct(2).first { it.shopId == 2L }
        out += "unit(p2@s2): ${UnitNormalizer.unitPricePaisa(p.pricePaisa, p.packSize, "g").toLong()}"

        // Pretend the user stands at the Tariq Road shop.
        out += "nearest: ${GeoUtils.sortByDistance(shops.all(), 24.8710, 67.0620).first().first.name}"
        return out
    }
}