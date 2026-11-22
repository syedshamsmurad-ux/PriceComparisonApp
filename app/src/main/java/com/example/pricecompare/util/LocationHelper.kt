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

// Runtime permission check + one-shot location. Uses the framework LocationManager
// (no extra Gradle dependency). The caller must cancel the returned signal when its screen closes.
object LocationHelper {
    val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private fun granted(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    // Either precise OR approximate is enough for "nearest shop".
    fun hasPermission(ctx: Context) = PERMISSIONS.any { granted(ctx, it) }

    // Calls onResult(location) on the main thread, or onResult(null) if no location is available.
    @SuppressLint("MissingPermission")   // hasPermission() is checked on the first line
    fun getLocation(ctx: Context, onResult: (Location?) -> Unit): CancellationSignal? {
        if (!hasPermission(ctx)) { onResult(null); return null }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // GPS needs the precise permission; with approximate only, just the network provider is allowed.
        val candidates = if (granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION))
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        else listOf(LocationManager.NETWORK_PROVIDER)
        val enabled = candidates.filter { lm.isProviderEnabled(it) }
        if (enabled.isEmpty()) { onResult(null); return null }   // Location switched off

        val signal = CancellationSignal()
        try {
            LocationManagerCompat.getCurrentLocation(
                lm, enabled.first(), signal, ContextCompat.getMainExecutor(ctx)
            ) { fresh -> onResult(fresh ?: lastKnown(lm, enabled)) }   // no fresh fix -> newest old one
        } catch (e: SecurityException) {
            onResult(null)
        }
        return signal
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(lm: LocationManager, providers: List<String>): Location? =
        providers.mapNotNull {
            try { lm.getLastKnownLocation(it) } catch (e: SecurityException) { null }
        }.maxByOrNull { it.time }
}