package com.example.pricecompare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R

// One list row, already formatted for display.
data class ListRow(val productId: Long, val name: String, val qtyText: String)

// Custom adapter for the shopping list. Removal is a lambda, so the adapter never touches the DB.
class ShoppingItemAdapter(private val onRemove: (productId: Long) -> Unit) :
    RecyclerView.Adapter<ShoppingItemAdapter.VH>() {

    private var rows: List<ListRow> = emptyList()

    fun submit(list: List<ListRow>) {
        rows = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_name)
        val qty: TextView = v.findViewById(R.id.tv_qty)
        val remove: Button = v.findViewById(R.id.btn_remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_shopping_item, parent, false))

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val r = rows[position]
        h.name.text = r.name
        h.qty.text = "Quantity: ${r.qtyText}"
        h.remove.setOnClickListener { onRemove(r.productId) }
    }
}