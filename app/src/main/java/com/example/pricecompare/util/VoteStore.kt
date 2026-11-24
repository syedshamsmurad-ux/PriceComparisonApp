package com.example.pricecompare.util

import android.content.Context

// Remembers which prices THIS USER already voted on (key includes the uid).
class VoteStore(ctx: Context, private val uid: String) {
    private val prefs = ctx.getSharedPreferences("votes", Context.MODE_PRIVATE)
    private fun key(priceId: Long) = "u${uid}_p$priceId"

    fun hasVoted(priceId: Long): Boolean = prefs.contains(key(priceId))
    fun recordVote(priceId: Long) = prefs.edit().putBoolean(key(priceId), true).apply()
}