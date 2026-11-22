package com.example.pricecompare.util

import android.content.Context

// Remembers which prices this device already voted on (one vote per price).
class VoteStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("votes", Context.MODE_PRIVATE)

    fun hasVoted(priceId: Long): Boolean = prefs.contains("p$priceId")
    fun recordVote(priceId: Long) = prefs.edit().putBoolean("p$priceId", true).apply()
}