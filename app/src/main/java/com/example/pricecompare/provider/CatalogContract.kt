package com.example.pricecompare.provider

import android.net.Uri

/**
 * Contract defining the public URIs, column names, and MIME types
 * exposed by [CatalogProvider].
 */
object CatalogContract {

    // Authority must match android:authorities in AndroidManifest.xml.
    const val AUTHORITY = "com.example.pricecompare.provider"

    // Base URI for all provider paths.
    private val BASE_URI: Uri = Uri.parse("content://$AUTHORITY")

    // Path segments matched by CatalogProvider's UriMatcher.
    const val PATH_PRODUCTS = "products"
    const val PATH_SHOPPING_LIST = "shopping_list"

    // Content URIs for the products catalog and the user's shopping list.
    val PRODUCTS_URI: Uri = BASE_URI.buildUpon().appendPath(PATH_PRODUCTS).build()
    val SHOPPING_LIST_URI: Uri = BASE_URI.buildUpon().appendPath(PATH_SHOPPING_LIST).build()

    // Columns exposed by the read-only products table.
    object Products {
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_NAME_NORM = "name_norm"
        const val COL_CATEGORY = "category"
        const val COL_BRAND = "brand"
        const val COL_PACK_SIZE = "pack_size"
        const val COL_UNIT = "unit"

        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_PRODUCTS"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_PRODUCTS"
    }

    // Columns exposed by shopping_list queries (joined with products) and writes.
    object ShoppingList {
        // Query result columns (product_id is aliased to _id for cursor compatibility).
        const val COL_ID = "_id"
        const val COL_NAME = "name"
        const val COL_UNIT = "unit"
        const val COL_QUANTITY = "quantity"

        // Column name used in ContentValues when inserting into shopping_list.
        const val COL_PRODUCT_ID = "product_id"

        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_SHOPPING_LIST"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_SHOPPING_LIST"
    }
}
