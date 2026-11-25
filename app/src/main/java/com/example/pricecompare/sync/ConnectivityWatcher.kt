package com.example.pricecompare.sync

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat

// Manages both connectivity listeners that trigger SyncManager when the phone comes online.
// Used by MainActivity in onStart() and onStop() so receivers are cleaned up safely.
class ConnectivityWatcher {
    private var receiver: SyncReceiver? = null
    private var callback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var online = false

    // Registers a BroadcastReceiver and a NetworkCallback to detect online transitions.
    fun start(ctx: Context) {
        if (receiver != null) return                      // already running
        // Hold applicationContext rather than an Activity to avoid memory leaks.
        val app = ctx.applicationContext

        // Trigger 1: BroadcastReceiver, registered in code.
        val r = SyncReceiver()
        @Suppress("DEPRECATION")
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        ContextCompat.registerReceiver(app, r, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiver = r

        // Trigger 2: NetworkCallback. Sync only on the change "offline -> validated online".
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        online = com.example.pricecompare.util.NetworkUtil.isOnline(app)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                // VALIDATED confirms the network actually has working internet, not just Wi-Fi.
                val nowOnline = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (nowOnline && !online) {
                    Log.d("SYNC", "NetworkCallback: back online -> sync")
                    SyncManager.syncNow(app)
                }
                online = nowOnline
            }
            override fun onLost(network: Network) { online = false }
        }
        cm.registerDefaultNetworkCallback(cb)
        callback = cb
    }

    // Unregisters both connectivity listeners when the screen stops to prevent leaks.
    fun stop(ctx: Context) {
        val app = ctx.applicationContext
        // Catch IllegalArgumentException in case Android already unregistered the listener.
        receiver?.let {
            try { app.unregisterReceiver(it) } catch (e: IllegalArgumentException) { /* already gone */ }
        }
        receiver = null
        callback?.let {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try { cm.unregisterNetworkCallback(it) } catch (e: IllegalArgumentException) { /* already gone */ }
        }
        callback = null
    }
}