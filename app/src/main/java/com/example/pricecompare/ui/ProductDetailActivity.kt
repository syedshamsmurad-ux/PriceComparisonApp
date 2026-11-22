package com.example.pricecompare.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R
import com.example.pricecompare.adapters.PriceRow
import com.example.pricecompare.adapters.PriceRowAdapter
import com.example.pricecompare.adapters.formatPack
import com.example.pricecompare.db.PriceDao
import com.example.pricecompare.db.ProductDao
import com.example.pricecompare.db.ShopDao
import com.example.pricecompare.logic.PriceSelector
import com.example.pricecompare.logic.UnitNormalizer
import com.example.pricecompare.model.Product
import com.example.pricecompare.util.VoteStore

// Price comparison for ONE product, plus confirm/dispute buttons on every price.
class ProductDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_ID = "product_id"   // key of the value in the Intent
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }

    private lateinit var product: Product
    private lateinit var priceDao: PriceDao
    private lateinit var votes: VoteStore
    private lateinit var adapter: PriceRowAdapter
    private lateinit var empty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_product_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left + 48, bars.top + 48, bars.right + 48, bars.bottom + 48)
            insets
        }

        // Read the product id sent by BrowseFragment.
        val found = ProductDao(this).byId(intent.getLongExtra(EXTRA_PRODUCT_ID, -1L))
        if (found == null) { finish(); return }   // bad id: nothing to show
        product = found
        priceDao = PriceDao(this)
        votes = VoteStore(this)

        findViewById<TextView>(R.id.tv_title).text = product.name
        findViewById<TextView>(R.id.tv_pack).text =
            "Pack: ${formatPack(product.packSize, product.unit)}"
        empty = findViewById(R.id.tv_empty)

        adapter = PriceRowAdapter { priceId, confirm -> onVote(priceId, confirm) }
        val list = findViewById<RecyclerView>(R.id.rv_prices)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        load()
    }

    // One vote per price per device; then reload so the new status shows immediately.
    private fun onVote(priceId: Long, confirm: Boolean) {
        if (votes.hasVoted(priceId)) return
        priceDao.vote(priceId, confirm)
        votes.recordVote(priceId)
        load()
    }

    private fun unit(e: com.example.pricecompare.model.PriceEntry) =
        UnitNormalizer.unitPricePaisa(e.pricePaisa, e.packSize, product.unit)

    // Query -> build display rows -> hand them to the adapter.
    private fun load() {
        val shopsById = ShopDao(this).all().associateBy { it.id }
        val all = priceDao.byProduct(product.id)              // every status
        val verified = PriceSelector.latestValid(all).values  // latest verified per shop
        val unverified = all.filter { it.status == "unverified" }  // rejected ones are dropped
        val sorted = (verified + unverified).sortedBy { unit(it) }

        val now = System.currentTimeMillis()
        // "Cheapest" = first FRESH and VERIFIED price, so neither stale nor flagged prices win.
        val cheapest = sorted.firstOrNull {
            it.status == "verified" && PriceSelector.isFresh(it.createdAt, now)
        }

        val rows = sorted.map { e ->
            val days = ((now - e.createdAt) / DAY_MS).toInt()
            PriceRow(
                priceId = e.id,
                shopName = shopsById[e.shopId]?.name ?: "Shop #${e.shopId}",
                priceText = UnitNormalizer.formatPkr(e.pricePaisa),
                unitText = "${UnitNormalizer.formatPkr(Math.round(unit(e)))} / " +
                        UnitNormalizer.unitLabel(product.unit),
                ageText = when (days) { 0 -> "today"; 1 -> "1 day ago"; else -> "$days days ago" },
                fresh = PriceSelector.isFresh(e.createdAt, now),
                cheapest = e === cheapest,
                verified = e.status == "verified",
                confirms = e.confirms,
                disputes = e.disputes,
                voted = votes.hasVoted(e.id)
            )
        }
        adapter.submit(rows)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }
}