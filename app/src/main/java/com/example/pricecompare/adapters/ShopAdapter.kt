package com.example.pricecompare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R
import java.util.Locale

// One shop row, display-ready. lat/lng ride along so the Navigate button needs no DB lookup.
data class ShopRow(
    val name: String, val info: String, val distanceText: String,
    val nearest: Boolean, val lat: Double, val lng: Double
)

// Custom adapter for the nearby shops screen (NearbyShopsActivity).
// onNavigate is supplied by the Activity, which opens the maps app.
class ShopAdapter(private val onNavigate: (ShopRow) -> Unit) :
    RecyclerView.Adapter<ShopAdapter.VH>() {

    // The rows currently shown. Starts empty until submit() is called.
    private var rows: List<ShopRow> = emptyList()

    // Replace the data and redraw.
    fun submit(list: List<ShopRow>) {
        rows = list
        notifyDataSetChanged()
    }

    // Holds the row's views so findViewById runs once per row, not on every scroll.
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_name)
        val info: TextView = v.findViewById(R.id.tv_info)
        val distance: TextView = v.findViewById(R.id.tv_distance)
        val navigate: Button = v.findViewById(R.id.btn_navigate)
    }

    // Called when RecyclerView needs a new row view: inflate item_shop.xml.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_shop, parent, false))

    override fun getItemCount(): Int = rows.size

    // Fills a (possibly reused) row; the nearest shop gets a star.
    override fun onBindViewHolder(h: VH, position: Int) {
        val r = rows[position]
        h.name.text = (if (r.nearest) "★ " else "") + r.name
        h.info.text = r.info
        h.distance.text = r.distanceText
        h.navigate.setOnClickListener { onNavigate(r) }
    }
}

// 0.85 km -> "850 m", 4.057 km -> "4.1 km". Display only.
fun formatDistance(km: Double): String =
    if (km < 1.0) "${Math.round(km * 1000)} m" else String.format(Locale.US, "%.1f km", km)