package com.example.pricecompare.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pricecompare.util.NetworkUtil

// BroadcastReceiver that listens for network changes and triggers a background sync.
// Registered dynamically at runtime by ConnectivityWatcher while MainActivity is visible.
class SyncReceiver : BroadcastReceiver() {
    // Called by Android on the main thread when connectivity changes; starts sync if online.
    override fun onReceive(context: Context, intent: Intent) {
        // onReceive runs on the MAIN thread, so do nothing slow here.
        // syncNow() starts its own worker thread.
        if (NetworkUtil.isOnline(context)) {
            Log.d("SYNC", "SyncReceiver: connectivity changed, online -> sync")
            SyncManager.syncNow(context)
        } else {
            Log.d("SYNC", "SyncReceiver: connectivity changed, not online yet")
        }
    }
}