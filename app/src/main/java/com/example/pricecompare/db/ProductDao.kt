package com.example.pricecompare.db

import android.content.Context
import android.database.Cursor
import com.example.pricecompare.model.Product

class ProductDao(ctx: Context) {
    private val helper = DatabaseHelper.get(ctx)

    private companion object {
        const val COLS = "id, name, category, brand, pack_size, unit"
    }

    // QUERY: every product, A to Z.
    fun all(): List<Product> =
        run("SELECT $COLS FROM products ORDER BY name", arrayOf())

    // QUERY: one product, or null if the id doesn't exist.
    fun byId(id: Long): Product? =
        run("SELECT $COLS FROM products WHERE id = ?", arrayOf(id.toString())).firstOrNull()

    // QUERY: distinct category names for the filter chips/spinner.
    fun categories(): List<String> {
        val out = mutableListOf<String>()
        helper.readableDatabase.rawQuery(
            "SELECT DISTINCT category FROM products ORDER BY category", null
        ).use { c -> while (c.moveToNext()) out.add(c.getString(0)) }
        return out
    }

    // QUERY: substring search on the lowercase name, optionally limited to one category.
    fun search(text: String, category: String?): List<Product> {
        val sql = StringBuilder("SELECT $COLS FROM products WHERE 1=1")
        val args = mutableListOf<String>()
        if (text.isNotBlank()) {
            // Escape LIKE wildcards so a typed % or _ is matched literally.
            val safe = text.trim().lowercase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            sql.append(" AND name_norm LIKE ? ESCAPE '\\'")
            args.add("%$safe%")
        }
        if (category != null) {
            sql.append(" AND category = ?")
            args.add(category)
        }
        sql.append(" ORDER BY name")
        return run(sql.toString(), args.toTypedArray())
    }

    // Shared helper: run a query and map every row. `.use {}` closes the cursor.
    private fun run(sql: String, args: Array<String>): List<Product> {
        val out = mutableListOf<Product>()
        helper.readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) out.add(read(c))
        }
        return out
    }

    private fun read(c: Cursor) = Product(
        id = c.getLong(0), name = c.getString(1), category = c.getString(2),
        brand = c.getString(3), packSize = c.getInt(4), unit = c.getString(5)
    )
}