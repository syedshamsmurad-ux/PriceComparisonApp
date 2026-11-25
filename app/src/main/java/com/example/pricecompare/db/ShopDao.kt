package com.example.pricecompare.db

import android.content.Context
import android.database.Cursor
import com.example.pricecompare.model.Shop

// Data Access Object (DAO) for reading shop records from the local SQLite database.
// Used by SubmitFragment, NearbyShopsActivity, ProductDetailActivity, and BasketResultActivity.
class ShopDao(ctx: Context) {
    private val helper = DatabaseHelper.get(ctx)

    private companion object {
        const val COLS = "id, name, type, address, lat, lng"
    }

    // Returns all shops sorted alphabetically by name.
    fun all(): List<Shop> = run("SELECT $COLS FROM shops ORDER BY name", arrayOf())

    // Returns the matching Shop for `id`, or null if no row exists.
    fun byId(id: Long): Shop? =
        run("SELECT $COLS FROM shops WHERE id = ?", arrayOf(id.toString())).firstOrNull()

    // Executes a parameterized SQL query and maps every cursor row into a Shop.
    private fun run(sql: String, args: Array<String>): List<Shop> {
        val out = mutableListOf<Shop>()
        // .use {} closes the Cursor automatically to prevent memory leaks.
        helper.readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) out.add(read(c))
        }
        return out
    }

    // Maps the current Cursor row into a Shop object by fixed column order.
    private fun read(c: Cursor) = Shop(
        id = c.getLong(0), name = c.getString(1), type = c.getString(2),
        address = c.getString(3), lat = c.getDouble(4), lng = c.getDouble(5)
    )
}