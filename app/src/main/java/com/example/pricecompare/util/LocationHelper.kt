package com.example.pricecompare.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal

// Checks runtime location permissions and fetches a single GPS or network location fix.
// Used by NearbyShopsActivity; returns a CancellationSignal so pending requests can abort.
object LocationHelper {
    val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Returns true if the user has already granted the given Android permission string.
    private fun granted(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    // Returns true if either fine (GPS) or coarse (network) location permission is granted.
    fun hasPermission(ctx: Context) = PERMISSIONS.any { granted(ctx, it) }

    // Requests one fresh location fix and passes it (or null) to `onResult` on the main thread.
    // Returns a CancellationSignal the caller should cancel when its screen closes.
    @SuppressLint("MissingPermission")   // hasPermission() is checked on the first line
    fun getLocation(ctx: Context, onResult: (Location?) -> Unit): CancellationSignal? {
        // Stop immediately and return null if the user has not granted location permission.
        if (!hasPermission(ctx)) { onResult(null); return null }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // GPS needs the precise permission; with approximate only, just the network provider is allowed.
        val candidates = if (granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION))
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        else listOf(LocationManager.NETWORK_PROVIDER)
        val enabled = candidates.filter { lm.isProviderEnabled(it) }
        if (enabled.isEmpty()) { onResult(null); return null }   // Location switched off

        val signal = CancellationSignal()
        val executor = ContextCompat.getMainExecutor(ctx)

        // Try each enabled provider in order before falling back to the newest cached location.
        fun tryProvider(index: Int) {
            if (signal.isCanceled) return
            if (index >= enabled.size) {
                onResult(lastKnown(lm, enabled))
                return
            }
            try {
                LocationManagerCompat.getCurrentLocation(lm, enabled[index], signal, executor) { fresh ->
                    if (fresh != null) onResult(fresh) else tryProvider(index + 1)
                }
            } catch (e: SecurityException) {
                tryProvider(index + 1)
            }
        }

        tryProvider(0)
        return signal
    }

    // Returns the most recent cached Location from enabled providers, or null if none exists.
    @SuppressLint("MissingPermission")
    private fun lastKnown(lm: LocationManager, providers: List<String>): Location? =
        providers.mapNotNull {
            try { lm.getLastKnownLocation(it) } catch (e: SecurityException) { null }
        }.maxByOrNull { it.time }
}