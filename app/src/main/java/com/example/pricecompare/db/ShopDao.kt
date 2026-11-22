package com.example.pricecompare.db

import android.content.Context
import android.database.Cursor
import com.example.pricecompare.model.Shop

class ShopDao(ctx: Context) {
    private val helper = DatabaseHelper.get(ctx)

    private companion object {
        const val COLS = "id, name, type, address, lat, lng"
    }

    // QUERY: all shops, A to Z.
    fun all(): List<Shop> = run("SELECT $COLS FROM shops ORDER BY name", arrayOf())

    // QUERY: one shop, or null.
    fun byId(id: Long): Shop? =
        run("SELECT $COLS FROM shops WHERE id = ?", arrayOf(id.toString())).firstOrNull()

    private fun run(sql: String, args: Array<String>): List<Shop> {
        val out = mutableListOf<Shop>()
        helper.readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) out.add(read(c))
        }
        return out
    }

    private fun read(c: Cursor) = Shop(
        id = c.getLong(0), name = c.getString(1), type = c.getString(2),
        address = c.getString(3), lat = c.getDouble(4), lng = c.getDouble(5)
    )
}