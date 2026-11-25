package com.example.pricecompare.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Manages the local SQLite database connection, schema creation, and upgrades.
// Used as a singleton (one shared instance) by all DAOs and CatalogProvider.
class DatabaseHelper private constructor(private val ctx: Context) :
    SQLiteOpenHelper(ctx, "pricecompare.db", null, 1) {

    companion object {
        // @Volatile ensures every thread immediately sees the updated instance reference.
        @Volatile private var inst: DatabaseHelper? = null
        // Returns the single DatabaseHelper instance using thread-safe double-checked locking.
        fun get(c: Context) = inst ?: synchronized(this) {
            // Use applicationContext so the singleton never leaks an Activity into memory.
            inst ?: DatabaseHelper(c.applicationContext).also { inst = it }
        }
    }

    // Runs before onCreate/onOpen; turns on foreign key checks so invalid IDs are rejected.
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    // Creates all four tables, adds a price lookup index, and loads CSV seed data once.
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE shops(
            id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL,
            type TEXT, address TEXT, lat REAL NOT NULL, lng REAL NOT NULL)""")
        db.execSQL("""CREATE TABLE products(
            id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL,
            name_norm TEXT NOT NULL, category TEXT NOT NULL, brand TEXT,
            pack_size INTEGER NOT NULL, unit TEXT NOT NULL)""")   // unit: g, ml, piece
        db.execSQL("""CREATE TABLE prices(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id INTEGER NOT NULL REFERENCES products(id),
            shop_id INTEGER NOT NULL REFERENCES shops(id),
            price_paisa INTEGER NOT NULL, pack_size INTEGER NOT NULL,
            created_at INTEGER NOT NULL,               -- UTC millis
            status TEXT NOT NULL DEFAULT 'verified',   -- verified / unverified
            confirms INTEGER NOT NULL DEFAULT 0, disputes INTEGER NOT NULL DEFAULT 0,
            sync_status INTEGER NOT NULL DEFAULT 0,    -- 0 pending, 1 synced
            remote_id TEXT, user_id TEXT)""")
        db.execSQL("""CREATE TABLE shopping_list(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id INTEGER NOT NULL UNIQUE REFERENCES products(id),
            quantity INTEGER NOT NULL)""")                 // base units
        // Index speeds up queries that look up latest prices by product, shop, and time.
        db.execSQL("CREATE INDEX idx_prices_ps ON prices(product_id, shop_id, created_at)")
        // Populate initial Karachi shops, products, and prices from assets on first run.
        SeedData.load(ctx, db)
    }

    // Drops all tables and rebuilds the database when the schema version number increases.
    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        // Dev only: wipe and recreate
        listOf("shopping_list", "prices", "products", "shops")
            .forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }
}