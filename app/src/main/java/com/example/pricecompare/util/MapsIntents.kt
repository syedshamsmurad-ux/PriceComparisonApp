package com.example.pricecompare.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

// Builds implicit Intents to open driving directions to a shop in an external maps app.
// Used by NearbyShopsActivity when the user taps a shop row.
object MapsIntents {
    // Tries turn-by-turn navigation, geo pin, then web maps until one installed app handles it.
    fun navigateTo(context: Context, name: String, lat: Double, lng: Double) {
        // URL-encode the shop name so spaces or symbols do not break the geo URI.
        val label = Uri.encode(name)
        val attempts = listOf(
            Uri.parse("google.navigation:q=$lat,$lng&mode=d"),                 // turn-by-turn
            Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)"),                    // pin only
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving") // browser
        )
        for (uri in attempts) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                // Restrict navigation URI to Google Maps so other apps do not fail on it.
                if (uri.scheme == "google.navigation") intent.setPackage("com.google.android.apps.maps")
                // Launching an Activity from a non-Activity Context requires the NEW_TASK flag.
                if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) { /* try the next option */ }
        }
        // Show a toast only if every fallback intent had no matching app installed.
        Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
    }
}