package com.example.pricecompare.logic

// Decides a price's status from its votes. Pure Kotlin.
object TrustRules {
    const val VERIFY_NET = 1   // unverified + (confirms - disputes) >= 1  -> verified
    const val REJECT_NET = 2   // (disputes - confirms) >= 2 -> rejected (hidden everywhere)

    fun nextStatus(status: String, confirms: Int, disputes: Int): String {
        if (status == "rejected") return status          // rejected is final
        val net = confirms - disputes
        return when {
            net <= -REJECT_NET -> "rejected"
            status == "unverified" && net >= VERIFY_NET -> "verified"
            else -> status
        }
    }
}