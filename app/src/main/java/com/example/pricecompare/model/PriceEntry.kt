package com.example.pricecompare.model

// One price report for a product at a shop. Plain data, no Android code.
// Money is stored as whole paisa (Rs 450.50 = 45050) so there are no rounding errors.
data class PriceEntry(
    val id: Long = 0,
    val productId: Long,
    val shopId: Long,
    val pricePaisa: Long,
    val packSize: Int,
    // Time the price was recorded, in UTC milliseconds.
    val createdAt: Long = 0,
    // "verified", "unverified" (flagged outlier) or "rejected" (voted down).
    val status: String = "verified",
    val confirms: Int = 0,
    val disputes: Int = 0,
    val syncStatus: Int = 0,        // 0 = waiting to upload, 1 = synced
    val remoteId: String? = null,   // Firestore document ID
    val userId: String? = null      // Firebase uid of the submitter
)