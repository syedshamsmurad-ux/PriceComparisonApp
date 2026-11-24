package com.example.pricecompare.sync

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat

// Owns both connectivity triggers. start() in onStart, stop() in onStop (no leaks).
class ConnectivityWatcher {
    private var receiver: SyncReceiver? = null
    private var callback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var online = false

    fun start(ctx: Context) {
        if (receiver != null) return                      // already running
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

    fun stop(ctx: Context) {
        val app = ctx.applicationContext
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