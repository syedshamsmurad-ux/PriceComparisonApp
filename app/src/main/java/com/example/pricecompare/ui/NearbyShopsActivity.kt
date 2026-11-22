package com.example.pricecompare.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.CancellationSignal
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R
import com.example.pricecompare.adapters.ShopAdapter
import com.example.pricecompare.adapters.ShopRow
import com.example.pricecompare.adapters.formatDistance
import com.example.pricecompare.db.ShopDao
import com.example.pricecompare.logic.GeoUtils
import com.example.pricecompare.model.Shop
import com.example.pricecompare.util.LocationHelper
import com.example.pricecompare.util.MapsIntents

// Lists shops nearest-first (our own Haversine). Falls back to A-Z if location is unavailable.
class NearbyShopsActivity : AppCompatActivity() {

    private lateinit var shops: List<Shop>
    private lateinit var adapter: ShopAdapter
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private var signal: CancellationSignal? = null

    // Must be created before the Activity starts, so it is a property, not a local.
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) findLocation()
        else showAlphabetical("Location permission denied. Showing all shops A to Z.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nearby_shops)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nearby_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left + 48, bars.top + 48, bars.right + 48, bars.bottom + 48)
            insets
        }

        status = findViewById(R.id.tv_status)
        progress = findViewById(R.id.progress)
        shops = ShopDao(this).all()
        adapter = ShopAdapter { r -> MapsIntents.navigateTo(this, r.name, r.lat, r.lng) }
        val rv = findViewById<RecyclerView>(R.id.rv_shops)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        // Ask at runtime if not yet granted (a manifest entry alone is not enough).
        if (LocationHelper.hasPermission(this)) findLocation()
        else permLauncher.launch(LocationHelper.PERMISSIONS)
    }

    private fun findLocation() {
        status.text = "Finding your location…"
        progress.visibility = View.VISIBLE
        signal = LocationHelper.getLocation(this) { loc ->
            if (isFinishing || isDestroyed) return@getLocation
            progress.visibility = View.GONE
            if (loc == null) {
                showAlphabetical("Location is off or unavailable. Showing all shops A to Z.")
            } else {
                val sorted = GeoUtils.sortByDistance(shops, loc.latitude, loc.longitude)
                status.text = "Nearest first, measured from your location."
                adapter.submit(sorted.mapIndexed { i, (shop, km) -> row(shop, formatDistance(km), i == 0) })
            }
        }
    }

    private fun showAlphabetical(message: String) {
        progress.visibility = View.GONE
        status.text = message
        adapter.submit(shops.map { row(it, "", false) })   // ShopDao.all() is already A to Z
    }

    private fun row(s: Shop, distance: String, nearest: Boolean) = ShopRow(
        name = s.name,
        info = listOfNotNull(s.type, s.address).joinToString(" • "),
        distanceText = distance, nearest = nearest, lat = s.lat, lng = s.lng
    )

    override fun onDestroy() {
        signal?.cancel()      // stop waiting for a fix once the screen is gone
        super.onDestroy()
    }
}