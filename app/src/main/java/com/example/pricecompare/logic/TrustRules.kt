package com.example.pricecompare.logic

// Pure Kotlin rules that update a price entry's trust status after community votes.
// Used by the voting flow and UI to decide when prices become verified or rejected.
object TrustRules {
    const val VERIFY_NET = 1   // unverified + (confirms - disputes) >= 1  -> verified
    const val REJECT_NET = 2   // (disputes - confirms) >= 2 -> rejected (hidden everywhere)

    // Returns the updated status string ("verified", "unverified", or "rejected") from votes.
    fun nextStatus(status: String, confirms: Int, disputes: Int): String {
        if (status == "rejected") return status          // rejected is final
        // Net score: positive means more confirms than disputes; negative means more disputes.
        val net = confirms - disputes
        // Reject first if net <= -2; only promote unverified entries when net >= 1.
        return when {
            net <= -REJECT_NET -> "rejected"
            status == "unverified" && net >= VERIFY_NET -> "verified"
            else -> status
        }
    }
}