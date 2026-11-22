package com.example.pricecompare.model

data class PriceEntry(
    val id: Long = 0,
    val productId: Long,
    val shopId: Long,
    val pricePaisa: Long,
    val packSize: Int,
    val createdAt: Long = 0,
    val status: String = "verified",
    val confirms: Int = 0,
    val disputes: Int = 0,
    val syncStatus: Int = 0,        // 0 = waiting to upload, 1 = synced
    val remoteId: String? = null,   // Firestore document ID
    val userId: String? = null      // Firebase uid of the submitter
)