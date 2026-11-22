package com.example.pricecompare.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R

// Everything a price row shows, already formatted. The adapter only displays it.
data class PriceRow(
    val priceId: Long,
    val shopName: String,
    val priceText: String,    // "Rs 450.00"
    val unitText: String,     // "Rs 450.00 / kg"
    val ageText: String,      // "3 days ago"
    val fresh: Boolean,       // false = older than 14 days
    val cheapest: Boolean,    // cheapest FRESH + VERIFIED price
    val verified: Boolean,    // false = flagged outlier awaiting confirmation
    val confirms: Int,
    val disputes: Int,
    val voted: Boolean        // this device already voted -> buttons disabled
)

// onVote is supplied by the Activity; the adapter never touches the database.
class PriceRowAdapter(private val onVote: (priceId: Long, confirm: Boolean) -> Unit) :
    RecyclerView.Adapter<PriceRowAdapter.VH>() {

    private var rows: List<PriceRow> = emptyList()

    fun submit(list: List<PriceRow>) {
        rows = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val badge: TextView = v.findViewById(R.id.tv_badge)
        val shop: TextView = v.findViewById(R.id.tv_shop)
        val meta: TextView = v.findViewById(R.id.tv_meta)
        val price: TextView = v.findViewById(R.id.tv_price)
        val unit: TextView = v.findViewById(R.id.tv_unit)
        val confirm: Button = v.findViewById(R.id.btn_confirm)
        val dispute: Button = v.findViewById(R.id.btn_dispute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_price_row, parent, false))

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val r = rows[position]
        h.shop.text = r.shopName
        h.price.text = r.priceText
        h.unit.text = r.unitText
        h.meta.text = listOfNotNull(
            r.ageText,
            if (!r.fresh) "outdated" else null,
            if (!r.verified) "unverified" else null
        ).joinToString(" • ")
        h.badge.visibility = if (r.cheapest) View.VISIBLE else View.GONE
        // Rows are recycled, so set alpha and button state on EVERY bind.
        h.itemView.alpha = if (r.fresh) 1f else 0.45f
        h.confirm.text = "Confirm (${r.confirms})"
        h.dispute.text = "Dispute (${r.disputes})"
        h.confirm.isEnabled = !r.voted
        h.dispute.isEnabled = !r.voted
        h.confirm.setOnClickListener { onVote(r.priceId, true) }
        h.dispute.setOnClickListener { onVote(r.priceId, false) }
    }
}