package com.example.pricecompare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R

// One result row: a shop, what it supplies (or lacks), and its total.
data class BasketRow(val title: String, val detail: String, val totalText: String)

class BasketAdapter : RecyclerView.Adapter<BasketAdapter.VH>() {

    private var rows: List<BasketRow> = emptyList()

    fun submit(list: List<BasketRow>) {
        rows = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tv_title)
        val detail: TextView = v.findViewById(R.id.tv_detail)
        val total: TextView = v.findViewById(R.id.tv_total)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_basket_shop, parent, false))

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val r = rows[position]
        h.title.text = r.title
        h.detail.text = r.detail
        h.total.text = r.totalText
    }
}