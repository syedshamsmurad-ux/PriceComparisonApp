package com.example.pricecompare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R
import com.example.pricecompare.model.Product

// Custom adapter: turns a List<Product> into rows (item_product.xml).
// onClick is a lambda the fragment supplies, so the adapter knows nothing about navigation.
class ProductAdapter(private val onClick: (Product) -> Unit) :
    RecyclerView.Adapter<ProductAdapter.VH>() {

    private var items: List<Product> = emptyList()

    // Replace the data and redraw. (notifyDataSetChanged is fine for ~80 rows.)
    fun submit(list: List<Product>) {
        items = list
        notifyDataSetChanged()
    }

    // Holds the row's views so findViewById runs once per row, not on every scroll.
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_name)
        val info: TextView = v.findViewById(R.id.tv_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.name.text = p.name
        holder.info.text = listOfNotNull(p.category, p.brand, formatPack(p.packSize, p.unit))
            .joinToString(" • ")
        holder.itemView.setOnClickListener { onClick(p) }
    }
}

// 5000 g -> "5 kg", 500 ml -> "500 ml", 12 piece -> "12 pcs". Display only.
fun formatPack(size: Int, unit: String): String = when (unit) {
    "piece" -> "$size pcs"
    "g" -> if (size >= 1000) "${trimZero(size / 1000.0)} kg" else "$size g"
    else -> if (size >= 1000) "${trimZero(size / 1000.0)} L" else "$size ml"
}

private fun trimZero(d: Double): String =
    if (d == Math.floor(d)) d.toInt().toString() else d.toString()