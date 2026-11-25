package com.example.pricecompare.db

import android.content.Context
import android.database.Cursor
import com.example.pricecompare.model.Product

// Data Access Object (DAO) for querying and searching the local products catalog.
// Used by BrowseFragment, ListFragment, SubmitFragment, and BasketResultActivity.
class ProductDao(ctx: Context) {
    private val helper = DatabaseHelper.get(ctx)

    private companion object {
        const val COLS = "id, name, category, brand, pack_size, unit"
    }

    // Returns all products from SQLite sorted alphabetically by name.
    fun all(): List<Product> =
        run("SELECT $COLS FROM products ORDER BY name", arrayOf())

    // Returns the matching Product for `id`, or null if no row exists.
    fun byId(id: Long): Product? =
        run("SELECT $COLS FROM products WHERE id = ?", arrayOf(id.toString())).firstOrNull()

    // Returns a sorted list of unique product category names for filter chips or spinners.
    fun categories(): List<String> {
        val out = mutableListOf<String>()
        helper.readableDatabase.rawQuery(
            "SELECT DISTINCT category FROM products ORDER BY category", null
        ).use { c -> while (c.moveToNext()) out.add(c.getString(0)) }
        return out
    }

    // Returns products whose lowercase name contains `text`, optionally filtered by category.
    fun search(text: String, category: String?): List<Product> {
        // 1=1 lets optional AND filters append cleanly without tracking the first WHERE.
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

    // Executes a parameterized SQL query and maps all cursor rows into Product objects.
    private fun run(sql: String, args: Array<String>): List<Product> {
        val out = mutableListOf<Product>()
        // .use {} automatically closes the Cursor to avoid memory leaks.
        helper.readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) out.add(read(c))
        }
        return out
    }

    // Maps the current Cursor row into a Product data object by fixed column order.
    private fun read(c: Cursor) = Product(
        id = c.getLong(0), name = c.getString(1), category = c.getString(2),
        brand = c.getString(3), packSize = c.getInt(4), unit = c.getString(5)
    )
}