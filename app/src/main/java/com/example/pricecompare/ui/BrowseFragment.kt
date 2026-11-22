package com.example.pricecompare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R
import com.example.pricecompare.adapters.ProductAdapter
import com.example.pricecompare.db.ProductDao
import android.widget.Button

// Browse tab: search box + category filter + product list.
class BrowseFragment : Fragment(R.layout.fragment_browse) {

    private lateinit var dao: ProductDao
    private lateinit var adapter: ProductAdapter
    private lateinit var search: EditText
    private lateinit var spinner: Spinner
    private lateinit var empty: TextView
    private var categories: List<String> = emptyList()

    // Runs once the layout exists, so findViewById is safe here.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btn_nearby).setOnClickListener {
            startActivity(Intent(requireContext(), NearbyShopsActivity::class.java))
        }
        dao = ProductDao(requireContext())
        search = view.findViewById(R.id.et_search)
        spinner = view.findViewById(R.id.sp_category)
        empty = view.findViewById(R.id.tv_empty)
        val list = view.findViewById<RecyclerView>(R.id.rv_products)

        // Clicking a row opens the detail screen. The product id travels in the Intent.
        adapter = ProductAdapter { product ->
            startActivity(
                Intent(requireContext(), ProductDetailActivity::class.java)
                    .putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
            )
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        // Spinner entry 0 = "All categories"; entry n = categories[n - 1].
        categories = dao.categories()
        spinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf("All categories") + categories
        )
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = refresh()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        search.doAfterTextChanged { refresh() }   // re-query on every keystroke
        refresh()
    }

    // Re-run the SQL search with the current text + category and redraw.
    private fun refresh() {
        val pos = spinner.selectedItemPosition
        val category = if (pos > 0) categories[pos - 1] else null
        val results = dao.search(search.text.toString(), category)
        adapter.submit(results)
        empty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
    }
}