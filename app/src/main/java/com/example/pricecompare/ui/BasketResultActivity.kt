package com.example.pricecompare.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R
import com.example.pricecompare.adapters.BasketAdapter
import com.example.pricecompare.adapters.BasketRow
import com.example.pricecompare.adapters.formatPack
import com.example.pricecompare.db.PriceDao
import com.example.pricecompare.db.ProductDao
import com.example.pricecompare.db.ShopDao
import com.example.pricecompare.db.ShoppingListDao
import com.example.pricecompare.logic.BasketCalculator
import com.example.pricecompare.logic.PriceSelector
import com.example.pricecompare.logic.UnitNormalizer

// Runs the basket algorithm on a BACKGROUND thread, then shows the result on the UI thread.
class BasketResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_K = "k"
        const val MODE_SINGLE = "single"
        const val MODE_SPLIT = "split"
    }

    // Finished, display-ready result. Built on the worker thread, applied on the UI thread.
    private class Outcome(val summary: String, val rows: List<BasketRow>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_basket_result)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.basket_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left + 48, bars.top + 48, bars.right + 48, bars.bottom + 48)
            insets
        }

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SINGLE
        val k = intent.getIntExtra(EXTRA_K, 2).coerceIn(1, 3)

        val summary = findViewById<TextView>(R.id.tv_summary)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val adapter = BasketAdapter()
        val rv = findViewById<RecyclerView>(R.id.rv_basket)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        summary.text = "Calculating…"

        // Worker thread: never touch Views here, only compute.
        Thread {
            val outcome = calculate(mode, k)
            // Back on the UI thread to update the screen. Skip if the user already left.
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                progress.visibility = View.GONE
                summary.text = outcome.summary
                adapter.submit(outcome.rows)
            }
        }.start()
    }

    private fun calculate(mode: String, k: Int): Outcome {
        val products = ProductDao(this).all().associateBy { it.id }
        val shops = ShopDao(this).all().associateBy { it.id }
        val items = ShoppingListDao(this).all()
        if (items.isEmpty()) return Outcome("Your shopping list is empty.", emptyList())

        // Latest VERIFIED price per (product, shop). Unverified and rejected prices are ignored.
        val latest = PriceSelector.latestValid(PriceDao(this).all())
        val shopIds = shops.keys.toList()
        fun name(id: Long) = products[id]?.name ?: "Product #$id"
        fun shop(id: Long) = shops[id]?.name ?: "Shop #$id"
        val noPrices = Outcome("No shop has a price for any item on your list.", emptyList())

        // Shops that sell nothing on the list are hidden.
        val ranked = BasketCalculator.singleShop(items, shopIds, latest)
            .filter { it.assignment.isNotEmpty() }
        if (ranked.isEmpty()) return noPrices

        if (mode == MODE_SINGLE) {
            val rows = ranked.mapIndexed { i, r ->
                BasketRow(
                    title = (if (i == 0) "★ " else "") + shop(r.shopIds[0]),
                    detail = if (r.missingProductIds.isEmpty()) "Has all ${items.size} items"
                    else "Missing: " + r.missingProductIds.joinToString { name(it) },
                    totalText = UnitNormalizer.formatPkr(r.totalPaisa)
                )
            }
            return Outcome("All shops ranked: fewest missing items first, then lowest total.", rows)
        }

        // Split mode: best group of at most k shops.
        val best = BasketCalculator.splitShop(items, shopIds, latest, k) ?: return noPrices
        val rows = best.shopIds.map { shopId ->
            val lines = items.filter { best.assignment[it.productId] == shopId }
            val costs = lines.map {
                BasketCalculator.itemCost(latest.getValue(Pair(it.productId, shopId)), it.quantity)
            }
            val detail = lines.zip(costs).joinToString("\n") { (item, cost) ->
                val unit = products[item.productId]?.unit ?: "g"
                "${name(item.productId)} (${formatPack(item.quantity, unit)}): ${UnitNormalizer.formatPkr(cost)}"
            }
            BasketRow(shop(shopId), detail, UnitNormalizer.formatPkr(costs.sum()))
        }

        val text = StringBuilder(
            "Cheapest with up to $k shop(s): ${UnitNormalizer.formatPkr(best.totalPaisa)} " +
                    "using ${best.shopIds.size} shop(s)."
        )
        // Compare with the best single shop only when both miss the same number of items.
        val single = ranked.first()
        if (single.missingProductIds.size == best.missingProductIds.size &&
            single.totalPaisa > best.totalPaisa) {
            text.append("\nSaves ${UnitNormalizer.formatPkr(single.totalPaisa - best.totalPaisa)} vs the best single shop.")
        }
        if (best.missingProductIds.isNotEmpty()) {
            text.append("\nNo price found for: " + best.missingProductIds.joinToString { name(it) })
        }
        return Outcome(text.toString(), rows)
    }
}