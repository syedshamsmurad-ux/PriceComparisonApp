package com.example.pricecompare.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// db/DatabaseHelper.kt
class DatabaseHelper private constructor(private val ctx: Context) :
    SQLiteOpenHelper(ctx, "pricecompare.db", null, 1) {

    companion object {
        @Volatile private var inst: DatabaseHelper? = null
        fun get(c: Context) = inst ?: synchronized(this) {
            inst ?: DatabaseHelper(c.applicationContext).also { inst = it }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

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
        db.execSQL("CREATE INDEX idx_prices_ps ON prices(product_id, shop_id, created_at)")
        SeedData.load(ctx, db)
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        // Dev only: wipe and recreate
        listOf("shopping_list", "prices", "products", "shops")
            .forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }
}