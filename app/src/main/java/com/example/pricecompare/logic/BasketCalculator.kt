package com.example.pricecompare.logic

import com.example.pricecompare.model.BasketResult
import com.example.pricecompare.model.PriceEntry
import com.example.pricecompare.model.ShoppingItem

// Calculates the cheapest shopping basket in single-shop and split-shop modes.
// Used by BasketResultActivity on a background thread; pure Kotlin with no Android UI code.
// `latest` maps a (productId, shopId) pair to the newest verified PriceEntry.
object BasketCalculator {

    // Returns the cost of `quantity` base units in paisa (100 paisa = 1 rupee).
    // Adding half of packSize before integer division rounds to the nearest paisa.
    fun itemCost(price: PriceEntry, quantity: Int): Long {
        require(price.packSize > 0) { "packSize must be positive" }
        return (price.pricePaisa * quantity + price.packSize / 2) / price.packSize
    }

    // Mode 1: totals the whole list at each shop and returns all results ranked best-first.
    // Ranks by fewest missing items first, then lowest total price.
    fun singleShop(
        items: List<ShoppingItem>,
        shopIds: List<Long>,
        latest: Map<Pair<Long, Long>, PriceEntry>
    ): List<BasketResult> =
        shopIds.map { shopId ->
            var total = 0L
            val assignment = mutableMapOf<Long, Long>()
            val missing = mutableListOf<Long>()
            // Check every shopping item at this shop; missing ones are recorded for ranking.
            for (item in items) {
                val price = latest[Pair(item.productId, shopId)]
                if (price == null) {
                    missing.add(item.productId)          // this shop doesn't sell it
                } else {
                    total += itemCost(price, item.quantity)
                    assignment[item.productId] = shopId
                }
            }
            BasketResult(listOf(shopId), total, assignment, missing)
        }.sortedWith(compareBy({ it.missingProductIds.size }, { it.totalPaisa }))

    // Mode 2: tests every group of 1 to k shops and returns the cheapest valid combination.
    // Returns null if the shopping list or shop list is empty.
    fun splitShop(
        items: List<ShoppingItem>,
        shopIds: List<Long>,
        latest: Map<Pair<Long, Long>, PriceEntry>,
        k: Int
    ): BasketResult? {
        // Guard against empty inputs because there is no basket to optimize.
        if (items.isEmpty() || shopIds.isEmpty()) return null

        // Precompute cost of each item at each shop (done once, reused by every subset).
        val cost = HashMap<Pair<Long, Long>, Long>()
        for (item in items) for (shopId in shopIds) {
            val price = latest[Pair(item.productId, shopId)] ?: continue
            cost[Pair(item.productId, shopId)] = itemCost(price, item.quantity)
        }

        var best: BasketResult? = null
        // Cap group size at shopIds.size in case fewer than k shops exist.
        for (size in 1..minOf(k, shopIds.size)) {
            for (subset in combinations(shopIds, size)) {
                val candidate = evaluate(items, subset, cost)
                if (best == null || isBetter(candidate, best)) best = candidate
            }
        }
        return best
    }

    // Assigns every item to the cheapest shop in `subset` and returns one BasketResult.
    private fun evaluate(
        items: List<ShoppingItem>,
        subset: List<Long>,
        cost: Map<Pair<Long, Long>, Long>
    ): BasketResult {
        var total = 0L
        val assignment = mutableMapOf<Long, Long>()
        val missing = mutableListOf<Long>()
        for (item in items) {
            var bestShop = -1L
            var bestCost = Long.MAX_VALUE
            // Scan all shops in this group to find the lowest cost for the current item.
            for (shopId in subset) {
                val c = cost[Pair(item.productId, shopId)] ?: continue
                if (c < bestCost) { bestCost = c; bestShop = shopId }
            }
            // -1L means no shop in this subset has a verified price for this item.
            if (bestShop == -1L) missing.add(item.productId)
            else { total += bestCost; assignment[item.productId] = bestShop }
        }
        // Report only shops that actually supply something.
        val used = assignment.values.toSet().sorted()
        return BasketResult(used, total, assignment, missing)
    }

    // Returns true if basket `a` beats `b` (fewer missing items, cheaper total, fewer shops).
    private fun isBetter(a: BasketResult, b: BasketResult): Boolean {
        if (a.missingProductIds.size != b.missingProductIds.size)
            return a.missingProductIds.size < b.missingProductIds.size
        if (a.totalPaisa != b.totalPaisa) return a.totalPaisa < b.totalPaisa
        return a.shopIds.size < b.shopIds.size
    }

    // Returns all unique groups of exactly `k` shop IDs using recursion (function calls itself).
    private fun combinations(ids: List<Long>, k: Int): List<List<Long>> {
        val out = mutableListOf<List<Long>>()
        fun rec(start: Int, cur: MutableList<Long>) {
            // Base case: copy `cur` before backtracking changes it later.
            if (cur.size == k) { out.add(cur.toList()); return }
            for (i in start until ids.size) {
                cur.add(ids[i])
                rec(i + 1, cur)
                cur.removeAt(cur.size - 1)     // backtrack
            }
        }
        rec(0, mutableListOf())
        return out
    }
}