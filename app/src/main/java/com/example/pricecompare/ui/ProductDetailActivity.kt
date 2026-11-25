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
import com.example.pricecompare.model.PriceEntry
import com.example.pricecompare.model.Product
import com.example.pricecompare.util.VoteStore
import com.google.firebase.auth.FirebaseAuth

// Displays all fresh, stale, and unverified prices for one product and handles community votes.
// Launched by BrowseFragment when the user taps a product row.
class ProductDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_ID = "product_id"   // key of the value in the Intent
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }

    private lateinit var product: Product
    private lateinit var priceDao: PriceDao
    private lateinit var votes: VoteStore
    private lateinit var uid: String
    private lateinit var adapter: PriceRowAdapter
    private lateinit var empty: TextView

    // Loads the requested product by ID, binds the price list adapter, and populates rows.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_product_detail)
        // Pad root view by system bar insets plus 48px for edge-to-edge clearance.
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
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        votes = VoteStore(this, uid)

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

    // Records one confirm or dispute vote per user/price in SQLite and VoteStore, then reloads.
    private fun onVote(priceId: Long, confirm: Boolean) {
        if (votes.hasVoted(priceId)) return
        if (priceDao.vote(priceId, confirm, uid)) votes.recordVote(priceId)
        load()
    }

    // Converts a PriceEntry into paisa per standard unit (kg, litre, or piece).
    private fun unit(e: PriceEntry) =
        UnitNormalizer.unitPricePaisa(e.pricePaisa, e.packSize, product.unit)

    // Queries prices for this product, filters out rejected rows, marks cheapest, and updates UI.
    private fun load() {
        val shopsById = ShopDao(this).all().associateBy { it.id }
        val all = priceDao.byProduct(product.id)              // every status
        // Keep only the newest verified price per shop, plus unverified outliers awaiting votes.
        val verified = PriceSelector.latestValid(all).values  // latest verified per shop
        val unverified = all.filter { it.status == "unverified" }  // rejected ones are dropped
        val sorted = (verified + unverified).sortedBy { unit(it) }

        val now = System.currentTimeMillis()
        // "Cheapest" = first FRESH and VERIFIED price, so neither stale nor flagged prices win.
        val cheapest = sorted.firstOrNull {
            it.status == "verified" && PriceSelector.isFresh(it.createdAt, now)
        }

        // Build UI rows with age text and disable voting on own or already-voted entries.
        val rows = sorted.map { e ->
            val days = ((now - e.createdAt) / DAY_MS).toInt()
            val mine = e.userId == uid                        // seed rows have no owner (null)
            PriceRow(
                priceId = e.id,
                shopName = shopsById[e.shopId]?.name ?: "Shop #${e.shopId}",
                priceText = UnitNormalizer.formatPkr(e.pricePaisa),
                unitText = "${UnitNormalizer.formatPkr(Math.round(unit(e)))} / " +
                        UnitNormalizer.unitLabel(product.unit),
                ageText = when (days) { 0 -> "today"; 1 -> "1 day ago"; else -> "$days days ago" } +
                        if (mine) " • your price" else "",
                fresh = PriceSelector.isFresh(e.createdAt, now),
                cheapest = e === cheapest,
                verified = e.status == "verified",
                confirms = e.confirms,
                disputes = e.disputes,
                voted = votes.hasVoted(e.id) || mine          // buttons grey out for own prices
            )
        }
        adapter.submit(rows)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }
}