package com.example.pricecompare.sync

import com.example.pricecompare.model.PriceEntry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.concurrent.TimeUnit

// All Firestore calls live here. Both functions BLOCK, so call them from a background thread only.
object FirestoreRepository {
    private const val COLLECTION = "prices"
    private const val TIMEOUT_S = 15L          // never hang forever when the network is bad
    private val STATUSES = setOf("verified", "unverified")

    // Writes one price as document <remoteId>. Same ID again = same document (safe retry).
    fun upload(e: PriceEntry) {
        val docId = requireNotNull(e.remoteId)
        val data = hashMapOf<String, Any>(
            "productId" to e.productId, "shopId" to e.shopId,
            "pricePaisa" to e.pricePaisa, "packSize" to e.packSize,
            "createdAt" to e.createdAt, "status" to e.status,
            "userId" to requireNotNull(e.userId)
        )
        Tasks.await(
            FirebaseFirestore.getInstance().collection(COLLECTION).document(docId).set(data),
            TIMEOUT_S, TimeUnit.SECONDS
        )
    }

    // Reads prices created after `since`. Source.SERVER = fresh from the cloud, not Firestore's own cache.
    fun fetchSince(since: Long): List<PriceEntry> {
        val snap = Tasks.await(
            FirebaseFirestore.getInstance().collection(COLLECTION)
                .whereGreaterThan("createdAt", since).get(Source.SERVER),
            TIMEOUT_S, TimeUnit.SECONDS
        )
        return snap.documents.mapNotNull { d ->
            // Never trust cloud data blindly: skip any document with missing or absurd fields.
            val productId = d.getLong("productId") ?: return@mapNotNull null
            val shopId = d.getLong("shopId") ?: return@mapNotNull null
            val price = d.getLong("pricePaisa") ?: return@mapNotNull null
            val pack = d.getLong("packSize") ?: return@mapNotNull null
            val created = d.getLong("createdAt") ?: return@mapNotNull null
            val status = d.getString("status") ?: return@mapNotNull null
            val uid = d.getString("userId") ?: return@mapNotNull null
            if (price < 100 || price > 10_000_000 || pack < 1 || pack > 1_000_000 ||
                created <= 0 || status !in STATUSES) return@mapNotNull null
            PriceEntry(
                productId = productId, shopId = shopId, pricePaisa = price, packSize = pack.toInt(),
                createdAt = created, status = status,
                syncStatus = 1, remoteId = d.id, userId = uid
            )
        }
    }
}