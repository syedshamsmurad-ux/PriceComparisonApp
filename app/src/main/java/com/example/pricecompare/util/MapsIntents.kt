package com.example.pricecompare.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

// Implicit intents: hand the route to whichever maps app is installed.
object MapsIntents {
    fun navigateTo(context: Context, name: String, lat: Double, lng: Double) {
        val label = Uri.encode(name)
        val attempts = listOf(
            Uri.parse("google.navigation:q=$lat,$lng&mode=d"),                 // turn-by-turn
            Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)"),                    // pin only
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving") // browser
        )
        for (uri in attempts) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (uri.scheme == "google.navigation") intent.setPackage("com.google.android.apps.maps")
                if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) { /* try the next option */ }
        }
        Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
    }
}