package com.example.pricecompare.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import com.example.pricecompare.db.DatabaseHelper

// ContentProvider exposing read-only products and full CRUD access to shopping_list.
// Used by ListFragment through Android's ContentResolver instead of direct DAO calls.
class CatalogProvider : ContentProvider() {

    companion object {
        private const val CODE_PRODUCTS = 1
        private const val CODE_PRODUCT_ID = 2
        private const val CODE_SHOPPING_LIST = 3
        private const val CODE_SHOPPING_LIST_ITEM = 4

        // Maps incoming content URIs to integer codes for routing.
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(CatalogContract.AUTHORITY, CatalogContract.PATH_PRODUCTS, CODE_PRODUCTS)
            addURI(CatalogContract.AUTHORITY, "${CatalogContract.PATH_PRODUCTS}/#", CODE_PRODUCT_ID)
            addURI(CatalogContract.AUTHORITY, CatalogContract.PATH_SHOPPING_LIST, CODE_SHOPPING_LIST)
            addURI(CatalogContract.AUTHORITY, "${CatalogContract.PATH_SHOPPING_LIST}/#", CODE_SHOPPING_LIST_ITEM)
        }

        // Default columns returned when querying shopping_list with a null projection.
        private val SHOPPING_LIST_DEFAULT_PROJECTION = arrayOf(
            CatalogContract.ShoppingList.COL_ID,
            CatalogContract.ShoppingList.COL_NAME,
            CatalogContract.ShoppingList.COL_UNIT,
            CatalogContract.ShoppingList.COL_QUANTITY
        )

        // Maps public shopping_list column names to qualified SQL columns in the join.
        private val SHOPPING_LIST_PROJECTION_MAP = mapOf(
            CatalogContract.ShoppingList.COL_ID to "shopping_list.product_id AS _id",
            CatalogContract.ShoppingList.COL_NAME to "products.name",
            CatalogContract.ShoppingList.COL_UNIT to "products.unit",
            CatalogContract.ShoppingList.COL_QUANTITY to "shopping_list.quantity"
        )
    }

    // Never open the database in onCreate(); Android calls onCreate() on the main thread
    // during app startup before any query or write is even requested.
    override fun onCreate(): Boolean = true

    // Returns the Android MIME type string for a matched URI, or null if unknown.
    override fun getType(uri: Uri): String? = when (uriMatcher.match(uri)) {
        CODE_PRODUCTS -> CatalogContract.Products.CONTENT_TYPE
        CODE_PRODUCT_ID -> CatalogContract.Products.CONTENT_ITEM_TYPE
        CODE_SHOPPING_LIST -> CatalogContract.ShoppingList.CONTENT_TYPE
        CODE_SHOPPING_LIST_ITEM -> CatalogContract.ShoppingList.CONTENT_ITEM_TYPE
        else -> null
    }

    // Queries products or joined shopping_list rows and returns a live-notified Cursor.
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val ctx = checkNotNull(context) { "Provider context is null" }
        // Lazily obtain the singleton helper only when data is actually requested.
        val db = DatabaseHelper.get(ctx).readableDatabase

        val cursor = when (uriMatcher.match(uri)) {
            CODE_PRODUCTS -> {
                val order = if (sortOrder.isNullOrBlank()) CatalogContract.Products.COL_NAME else sortOrder
                db.query("products", projection, selection, selectionArgs, null, null, order)
            }

            CODE_PRODUCT_ID -> {
                val productId = ContentUris.parseId(uri)
                val (where, args) = appendIdClause(
                    selection, selectionArgs, "${CatalogContract.Products.COL_ID} = ?", productId
                )
                val order = if (sortOrder.isNullOrBlank()) CatalogContract.Products.COL_NAME else sortOrder
                db.query("products", projection, where, args, null, null, order)
            }

            CODE_SHOPPING_LIST -> {
                queryShoppingList(db, projection, selection, selectionArgs, sortOrder)
            }

            CODE_SHOPPING_LIST_ITEM -> {
                val productId = ContentUris.parseId(uri)
                val (where, args) = appendIdClause(
                    selection, selectionArgs, "shopping_list.product_id = ?", productId
                )
                queryShoppingList(db, projection, where, args, sortOrder)
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        // Register the cursor to auto-receive change notifications for this URI.
        cursor.setNotificationUri(ctx.contentResolver, uri)
        return cursor
    }

    // Inserts or replaces a shopping_list item and returns its item URI, or null on failure.
    // Throws UnsupportedOperationException for products and IllegalArgumentException for bad data.
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val ctx = checkNotNull(context) { "Provider context is null" }
        when (uriMatcher.match(uri)) {
            CODE_PRODUCTS, CODE_PRODUCT_ID ->
                throw UnsupportedOperationException("Products are read-only")

            CODE_SHOPPING_LIST -> {
                if (values == null) {
                    throw IllegalArgumentException("ContentValues cannot be null")
                }
                val productId = values.getAsLong(CatalogContract.ShoppingList.COL_PRODUCT_ID)
                if (productId == null || productId <= 0L) {
                    throw IllegalArgumentException("Missing or invalid product_id")
                }
                val quantity = values.getAsInteger(CatalogContract.ShoppingList.COL_QUANTITY)
                if (quantity == null || quantity < 1) {
                    throw IllegalArgumentException("Quantity must be an integer >= 1")
                }

                // Copy only validated columns so unexpected keys cannot break the SQL statement.
                val cleanValues = ContentValues().apply {
                    put(CatalogContract.ShoppingList.COL_PRODUCT_ID, productId)
                    put(CatalogContract.ShoppingList.COL_QUANTITY, quantity)
                }

                // CONFLICT_REPLACE replaces the existing row when product_id is already on the list.
                val rowId = DatabaseHelper.get(ctx).writableDatabase.insertWithOnConflict(
                    "shopping_list",
                    null,
                    cleanValues,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                if (rowId == -1L) return null

                // Item URIs under shopping_list/# identify rows by product_id.
                val itemUri = ContentUris.withAppendedId(CatalogContract.SHOPPING_LIST_URI, productId)
                ctx.contentResolver.notifyChange(itemUri, null)
                return itemUri
            }

            else -> throw IllegalArgumentException("Unsupported URI for insert: $uri")
        }
    }

    // Updates the quantity (>= 1) of one shopping_list/# item and returns rows updated.
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val ctx = checkNotNull(context) { "Provider context is null" }
        when (uriMatcher.match(uri)) {
            CODE_PRODUCTS, CODE_PRODUCT_ID ->
                throw UnsupportedOperationException("Products are read-only")

            CODE_SHOPPING_LIST_ITEM -> {
                if (values == null) {
                    throw IllegalArgumentException("ContentValues cannot be null")
                }
                val quantity = values.getAsInteger(CatalogContract.ShoppingList.COL_QUANTITY)
                if (quantity == null || quantity < 1) {
                    throw IllegalArgumentException("Quantity must be an integer >= 1")
                }

                val productId = ContentUris.parseId(uri)
                val (where, args) = appendIdClause(
                    selection, selectionArgs, "product_id = ?", productId
                )
                val cleanValues = ContentValues().apply {
                    put(CatalogContract.ShoppingList.COL_QUANTITY, quantity)
                }

                val updated = DatabaseHelper.get(ctx).writableDatabase.update(
                    "shopping_list", cleanValues, where, args
                )
                if (updated > 0) {
                    ctx.contentResolver.notifyChange(uri, null)
                }
                return updated
            }

            else -> throw IllegalArgumentException("Unsupported URI for update: $uri")
        }
    }

    // Deletes all shopping_list rows or one shopping_list/# item and returns rows deleted.
    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val ctx = checkNotNull(context) { "Provider context is null" }
        val db = DatabaseHelper.get(ctx).writableDatabase

        val deleted = when (uriMatcher.match(uri)) {
            CODE_PRODUCTS, CODE_PRODUCT_ID ->
                throw UnsupportedOperationException("Products are read-only")

            CODE_SHOPPING_LIST ->
                db.delete("shopping_list", selection, selectionArgs)

            CODE_SHOPPING_LIST_ITEM -> {
                val productId = ContentUris.parseId(uri)
                val (where, args) = appendIdClause(
                    selection, selectionArgs, "product_id = ?", productId
                )
                db.delete("shopping_list", where, args)
            }

            else -> throw IllegalArgumentException("Unsupported URI for delete: $uri")
        }

        if (deleted > 0) {
            ctx.contentResolver.notifyChange(uri, null)
        }
        return deleted
    }

    // Joins shopping_list with products so callers receive product names and units directly.
    private fun queryShoppingList(
        db: SQLiteDatabase,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val qb = SQLiteQueryBuilder().apply {
            tables = "shopping_list INNER JOIN products ON shopping_list.product_id = products.id"
            projectionMap = SHOPPING_LIST_PROJECTION_MAP
        }
        val cols = projection ?: SHOPPING_LIST_DEFAULT_PROJECTION
        val order = if (sortOrder.isNullOrBlank()) "shopping_list.id" else sortOrder
        return qb.query(db, cols, selection, selectionArgs, null, null, order)
    }

    // Safely combines an optional caller selection with a parameterized ID filter using '?'.
    private fun appendIdClause(
        selection: String?,
        selectionArgs: Array<out String>?,
        idCondition: String,
        id: Long
    ): Pair<String, Array<String>> {
        val where = if (selection.isNullOrBlank()) idCondition else "($selection) AND $idCondition"
        val args = (selectionArgs?.toList() ?: emptyList()) + id.toString()
        return where to args.toTypedArray()
    }
}