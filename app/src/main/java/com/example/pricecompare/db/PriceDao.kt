package com.example.pricecompare.db

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import com.example.pricecompare.logic.TrustRules
import com.example.pricecompare.model.PriceEntry

class PriceDao(ctx: Context) {
    private val helper = DatabaseHelper.get(ctx)

    // INSERT: saves locally. Sync fields come from the entry (0 = pending upload, 1 = already synced).
    fun insert(e: PriceEntry): Long = helper.writableDatabase.insert("prices", null,
        ContentValues().apply {
            put("product_id", e.productId); put("shop_id", e.shopId)
            put("price_paisa", e.pricePaisa); put("pack_size", e.packSize)
            put("created_at", if (e.createdAt > 0) e.createdAt else System.currentTimeMillis())
            put("status", e.status)
            put("sync_status", e.syncStatus)
            put("remote_id", e.remoteId)
            put("user_id", e.userId)
        })

    // UPDATE: mark a row as uploaded.
    fun markSynced(id: Long, remoteId: String): Int = helper.writableDatabase.update(
        "prices",
        ContentValues().apply { put("sync_status", 1); put("remote_id", remoteId) },
        "id = ?", arrayOf(id.toString())
    )

    // UPDATE: add one confirm or dispute, then recompute the status with TrustRules.
    // UPDATE: add one confirm or dispute, then recompute the status with TrustRules.
    // Returns false (and changes nothing) for an unknown id, or if voterUid submitted the price.
    fun vote(id: Long, confirm: Boolean, voterUid: String?): Boolean {
        val db = helper.writableDatabase
        var status = ""; var confirms = 0; var disputes = 0; var owner: String? = null
        db.rawQuery("SELECT status, confirms, disputes, user_id FROM prices WHERE id = ?",
            arrayOf(id.toString())).use { c ->
            if (!c.moveToFirst()) return false
            status = c.getString(0); confirms = c.getInt(1); disputes = c.getInt(2)
            owner = c.getString(3)                      // null for seed rows
        }
        if (voterUid != null && owner == voterUid) return false   // no voting on your own price
        if (confirm) confirms++ else disputes++
        return db.update("prices", ContentValues().apply {
            put("confirms", confirms); put("disputes", disputes)
            put("status", TrustRules.nextStatus(status, confirms, disputes))
        }, "id = ?", arrayOf(id.toString())) > 0
    }

    // QUERY: this user's rows still waiting to upload (oldest first).
    // Seed rows and rows with no document ID are never uploaded.
    fun pending(uid: String): List<PriceEntry> {
        val out = mutableListOf<PriceEntry>()
        helper.readableDatabase.rawQuery(
            "SELECT id, product_id, shop_id, price_paisa, pack_size, created_at, status, remote_id " +
                    "FROM prices WHERE sync_status = 0 AND remote_id IS NOT NULL AND user_id = ? " +
                    "ORDER BY created_at",
            arrayOf(uid)
        ).use { c ->
            while (c.moveToNext()) {
                out.add(PriceEntry(
                    id = c.getLong(0), productId = c.getLong(1), shopId = c.getLong(2),
                    pricePaisa = c.getLong(3), packSize = c.getInt(4), createdAt = c.getLong(5),
                    status = c.getString(6), remoteId = c.getString(7), userId = uid
                ))
            }
        }
        return out
    }

    fun pendingCount(uid: String): Long = DatabaseUtils.queryNumEntries(
        helper.readableDatabase, "prices",
        "sync_status = 0 AND remote_id IS NOT NULL AND user_id = ?", arrayOf(uid)
    )

    // QUERY: do we already hold the row for this Firestore document?
    fun hasRemote(remoteId: String): Boolean = DatabaseUtils.queryNumEntries(
        helper.readableDatabase, "prices", "remote_id = ?", arrayOf(remoteId)
    ) > 0

    // QUERY: every price row of every product (all statuses). PriceSelector decides which count.
    fun all(): List<PriceEntry> {
        val out = mutableListOf<PriceEntry>()
        helper.readableDatabase.rawQuery(
            "SELECT id, product_id, shop_id, price_paisa, pack_size, created_at, status, confirms, disputes FROM prices",
            null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(PriceEntry(
                    id = c.getLong(0), productId = c.getLong(1), shopId = c.getLong(2),
                    pricePaisa = c.getLong(3), packSize = c.getInt(4), createdAt = c.getLong(5),
                    status = c.getString(6), confirms = c.getInt(7), disputes = c.getInt(8)
                ))
            }
        }
        return out
    }

    // DELETE: remove one price row.
    fun delete(id: Long): Int =
        helper.writableDatabase.delete("prices", "id = ?", arrayOf(id.toString()))

    // QUERY: all prices of one product (every status), cheapest UNIT price first.
    // QUERY: all prices of one product (every status), cheapest UNIT price first.
    fun byProduct(productId: Long): List<PriceEntry> {
        val out = mutableListOf<PriceEntry>()
        helper.readableDatabase.rawQuery(
            "SELECT id, shop_id, price_paisa, pack_size, created_at, status, confirms, disputes, user_id " +
                    "FROM prices WHERE product_id = ? ORDER BY price_paisa * 1.0 / pack_size",
            arrayOf(productId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                out.add(PriceEntry(
                    id = c.getLong(0), productId = productId, shopId = c.getLong(1),
                    pricePaisa = c.getLong(2), packSize = c.getInt(3),
                    createdAt = c.getLong(4), status = c.getString(5),
                    confirms = c.getInt(6), disputes = c.getInt(7), userId = c.getString(8)
                ))
            }
        }
        return out
    }
}