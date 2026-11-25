package com.example.pricecompare.db

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import com.example.pricecompare.logic.TrustRules
import com.example.pricecompare.model.PriceEntry

// Data Access Object (DAO) for reading, inserting, voting on, and deleting price rows.
// Used by PriceDetail/Submit screens, BasketResultActivity, and SyncManager.
class PriceDao(ctx: Context) {
    private val helper = DatabaseHelper.get(ctx)

    // Inserts a PriceEntry into SQLite (sync_status 0 = pending, 1 = synced) and returns row ID.
    fun insert(e: PriceEntry): Long = helper.writableDatabase.insert("prices", null,
        ContentValues().apply {
            put("product_id", e.productId); put("shop_id", e.shopId)
            put("price_paisa", e.pricePaisa); put("pack_size", e.packSize)
            // Use current device time if the entry did not already supply a timestamp.
            put("created_at", if (e.createdAt > 0) e.createdAt else System.currentTimeMillis())
            put("status", e.status)
            put("confirms", e.confirms)
            put("disputes", e.disputes)
            put("sync_status", e.syncStatus)
            put("remote_id", e.remoteId)
            put("user_id", e.userId)
        })

    // Marks a local price row as uploaded (sync_status = 1) and saves its Firestore document ID.
    fun markSynced(id: Long, remoteId: String): Int = helper.writableDatabase.update(
        "prices",
        ContentValues().apply { put("sync_status", 1); put("remote_id", remoteId) },
        "id = ?", arrayOf(id.toString())
    )

    // Marks a permanently rejected upload row (sync_status = -1) so future syncs do not retry it.
    fun markFailed(id: Long): Int = helper.writableDatabase.update(
        "prices",
        ContentValues().apply { put("sync_status", -1) },
        "id = ?", arrayOf(id.toString())
    )

    // Records one confirm or dispute vote and updates status via TrustRules.
    // Returns false without saving if the row is missing or voterUid matches the author.
    fun vote(id: Long, confirm: Boolean, voterUid: String?): Boolean {
        val db = helper.writableDatabase
        var status = ""; var confirms = 0; var disputes = 0; var owner: String? = null
        db.rawQuery("SELECT status, confirms, disputes, user_id FROM prices WHERE id = ?",
            arrayOf(id.toString())).use { c ->
            // Stop early if no price row exists for this id.
            if (!c.moveToFirst()) return false
            status = c.getString(0); confirms = c.getInt(1); disputes = c.getInt(2)
            owner = c.getString(3)                      // null for seed rows
        }
        // Block self-voting so users cannot confirm or dispute their own submission.
        if (voterUid != null && owner == voterUid) return false   // no voting on your own price
        if (confirm) confirms++ else disputes++
        return db.update("prices", ContentValues().apply {
            put("confirms", confirms); put("disputes", disputes)
            put("status", TrustRules.nextStatus(status, confirms, disputes))
        }, "id = ?", arrayOf(id.toString())) > 0
    }

    // Returns this user's un-uploaded price rows (sync_status = 0), oldest first.
    // Seed rows and rows without a remote_id are never uploaded.
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

    // Returns how many price rows for `uid` are still waiting to be uploaded.
    fun pendingCount(uid: String): Long = DatabaseUtils.queryNumEntries(
        helper.readableDatabase, "prices",
        "sync_status = 0 AND remote_id IS NOT NULL AND user_id = ?", arrayOf(uid)
    )

    // Returns true if a price row with this Firestore document ID already exists locally.
    fun hasRemote(remoteId: String): Boolean = DatabaseUtils.queryNumEntries(
        helper.readableDatabase, "prices", "remote_id = ?", arrayOf(remoteId)
    ) > 0

    // Returns every row in prices regardless of status; PriceSelector filters them later.
    fun all(): List<PriceEntry> {
        val out = mutableListOf<PriceEntry>()
        helper.readableDatabase.rawQuery(
            "SELECT id, product_id, shop_id, price_paisa, pack_size, created_at, " +
                    "status, confirms, disputes, sync_status, remote_id, user_id FROM prices",
            null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(PriceEntry(
                    id = c.getLong(0), productId = c.getLong(1), shopId = c.getLong(2),
                    pricePaisa = c.getLong(3), packSize = c.getInt(4), createdAt = c.getLong(5),
                    status = c.getString(6), confirms = c.getInt(7), disputes = c.getInt(8),
                    syncStatus = c.getInt(9), remoteId = c.getString(10), userId = c.getString(11)
                ))
            }
        }
        return out
    }

    // Deletes one price row by its local ID and returns the number of rows removed.
    fun delete(id: Long): Int =
        helper.writableDatabase.delete("prices", "id = ?", arrayOf(id.toString()))

    // Returns all price rows for one product (all statuses), sorted cheapest per unit first.
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