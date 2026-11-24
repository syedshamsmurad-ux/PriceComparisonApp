package com.example.pricecompare.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pricecompare.util.NetworkUtil

// Course concept: BroadcastReceiver. Wakes up when connectivity changes.
class SyncReceiver : BroadcastReceiver() {
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