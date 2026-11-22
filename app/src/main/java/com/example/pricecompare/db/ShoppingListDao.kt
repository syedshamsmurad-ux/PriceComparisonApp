package com.example.pricecompare.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.pricecompare.model.ShoppingItem

class ShoppingListDao(ctx: Context) {
    private val helper = DatabaseHelper.get(ctx)

    // QUERY: the whole list, oldest first. Quantity is in base units (g, ml, piece).
    fun all(): List<ShoppingItem> {
        val out = mutableListOf<ShoppingItem>()
        helper.readableDatabase.rawQuery(
            "SELECT product_id, quantity FROM shopping_list ORDER BY id", null
        ).use { c -> while (c.moveToNext()) out.add(ShoppingItem(c.getLong(0), c.getInt(1))) }
        return out
    }

    // INSERT / UPDATE: product_id is UNIQUE, so adding a product already on the list REPLACES its quantity.
    fun set(productId: Long, quantity: Int) {
        helper.writableDatabase.insertWithOnConflict(
            "shopping_list", null,
            ContentValues().apply { put("product_id", productId); put("quantity", quantity) },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // DELETE: one item.
    fun remove(productId: Long): Int = helper.writableDatabase
        .delete("shopping_list", "product_id = ?", arrayOf(productId.toString()))

    // DELETE: everything.
    fun clear(): Int = helper.writableDatabase.delete("shopping_list", null, null)
}