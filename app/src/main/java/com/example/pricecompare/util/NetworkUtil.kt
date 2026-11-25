package com.example.pricecompare.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// Checks whether the device currently has a working, validated internet connection.
// Used by SyncManager, SyncReceiver, and ConnectivityWatcher before talking to Firebase.
object NetworkUtil {
    // Returns true only if the active network supports internet and passed system validation.
    fun isOnline(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Return false immediately when no active network or capabilities exist.
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}