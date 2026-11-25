package com.example.pricecompare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R

// One result row: a shop, what it supplies (or lacks), and its total.
data class BasketRow(val title: String, val detail: String, val totalText: String)

// Custom adapter for the basket result screen (BasketResultActivity).
class BasketAdapter : RecyclerView.Adapter<BasketAdapter.VH>() {

    // The rows currently shown. Starts empty until submit() is called.
    private var rows: List<BasketRow> = emptyList()

    // Replace the data and redraw.
    fun submit(list: List<BasketRow>) {
        rows = list
        notifyDataSetChanged()
    }

    // Holds the row's views so findViewById runs once per row, not on every scroll.
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tv_title)
        val detail: TextView = v.findViewById(R.id.tv_detail)
        val total: TextView = v.findViewById(R.id.tv_total)
    }

    // Called when RecyclerView needs a new row view: inflate item_basket_shop.xml.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_basket_shop, parent, false))

    override fun getItemCount(): Int = rows.size

    // Fills a (possibly reused) row with the shop result at this position.
    override fun onBindViewHolder(h: VH, position: Int) {
        val r = rows[position]
        h.title.text = r.title
        h.detail.text = r.detail
        h.total.text = r.totalText
    }
}