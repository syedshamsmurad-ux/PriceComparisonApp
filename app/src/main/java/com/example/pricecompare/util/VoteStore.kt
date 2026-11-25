package com.example.pricecompare.util

import android.content.Context

// Stores which price rows a user has already voted on using SharedPreferences.
// Used by ProductDetailActivity to prevent the same user from voting twice on one row.
class VoteStore(ctx: Context, private val uid: String) {
    private val prefs = ctx.getSharedPreferences("votes", Context.MODE_PRIVATE)
    // Builds a unique preference key per user ID and price row ID.
    private fun key(priceId: Long) = "u${uid}_p$priceId"

    // Returns true if this user has already confirmed or disputed the given price row.
    fun hasVoted(priceId: Long): Boolean = prefs.contains(key(priceId))
    // Saves asynchronously that this user voted on `priceId` so buttons stay disabled.
    fun recordVote(priceId: Long) = prefs.edit().putBoolean(key(priceId), true).apply()
}