package com.example.pricecompare.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.pricecompare.model.ShoppingItem

// Data Access Object (DAO) for managing items in the local shopping_list table.
// Used by BasketResultActivity (ListFragment routes through CatalogProvider instead).
class ShoppingListDao(ctx: Context) {
    private val helper = DatabaseHelper.get(ctx)

    // Returns all shopping list items ordered oldest-first; quantity is in base units.
    fun all(): List<ShoppingItem> {
        val out = mutableListOf<ShoppingItem>()
        helper.readableDatabase.rawQuery(
            "SELECT product_id, quantity FROM shopping_list ORDER BY id", null
        ).use { c -> while (c.moveToNext()) out.add(ShoppingItem(c.getLong(0), c.getInt(1))) }
        return out
    }

    // Inserts a product or replaces its quantity if that product_id is already on the list.
    fun set(productId: Long, quantity: Int) {
        require(quantity >= 1) { "quantity must be >= 1" }
        // product_id is UNIQUE, so CONFLICT_REPLACE updates quantity instead of failing.
        helper.writableDatabase.insertWithOnConflict(
            "shopping_list", null,
            ContentValues().apply { put("product_id", productId); put("quantity", quantity) },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // Removes one product from shopping_list and returns the count of deleted rows.
    fun remove(productId: Long): Int = helper.writableDatabase
        .delete("shopping_list", "product_id = ?", arrayOf(productId.toString()))

    // Deletes every row in shopping_list and returns how many rows were removed.
    fun clear(): Int = helper.writableDatabase.delete("shopping_list", null, null)
}