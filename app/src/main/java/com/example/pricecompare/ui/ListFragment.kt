package com.example.pricecompare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecompare.R
import com.example.pricecompare.adapters.ListRow
import com.example.pricecompare.adapters.ShoppingItemAdapter
import com.example.pricecompare.adapters.formatPack
import com.example.pricecompare.db.ProductDao
import com.example.pricecompare.db.ShoppingListDao

// List tab: build the shopping list, pick a mode, open the basket screen.
class ListFragment : Fragment(R.layout.fragment_list) {

    private lateinit var listDao: ShoppingListDao
    private lateinit var productDao: ProductDao
    private lateinit var adapter: ShoppingItemAdapter
    private lateinit var empty: TextView
    private lateinit var spK: Spinner
    private lateinit var rbSplit: RadioButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        listDao = ShoppingListDao(ctx)
        productDao = ProductDao(ctx)
        empty = view.findViewById(R.id.tv_empty)
        spK = view.findViewById(R.id.sp_k)
        rbSplit = view.findViewById(R.id.rb_split)

        adapter = ShoppingItemAdapter { productId -> listDao.remove(productId); refresh() }
        val rv = view.findViewById<RecyclerView>(R.id.rv_list)
        rv.layoutManager = LinearLayoutManager(ctx)
        rv.adapter = adapter

        // k = spinner position + 1. Only used in split mode, so disabled otherwise.
        spK.adapter = ArrayAdapter(
            ctx, android.R.layout.simple_spinner_dropdown_item,
            listOf("At most 1 shop", "At most 2 shops", "At most 3 shops")
        )
        spK.setSelection(1)
        spK.isEnabled = false
        view.findViewById<RadioGroup>(R.id.rg_mode).setOnCheckedChangeListener { _, id ->
            spK.isEnabled = id == R.id.rb_split
        }

        view.findViewById<Button>(R.id.btn_add).setOnClickListener { showAddDialog() }
        view.findViewById<Button>(R.id.btn_clear).setOnClickListener { listDao.clear(); refresh() }
        view.findViewById<Button>(R.id.btn_calc).setOnClickListener { calculate() }
        refresh()
    }

    // Dialog with a product spinner and a pack count. Quantity is stored in base units.
    private fun showAddDialog() {
        val products = productDao.all()
        val v = layoutInflater.inflate(R.layout.dialog_add_to_list, null)
        val sp = v.findViewById<Spinner>(R.id.sp_item)
        val hint = v.findViewById<TextView>(R.id.tv_hint)
        val et = v.findViewById<EditText>(R.id.et_packs)

        sp.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, products.map { it.name })
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val product = products[pos]
                hint.text = "1 pack = ${formatPack(product.packSize, product.unit)}"
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add to list")
            .setView(v)
            .setPositiveButton("Add", null)      // real handler set below, so bad input keeps it open
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val packs = et.text.toString().toIntOrNull()
            if (packs == null || packs !in 1..99) { et.error = "Enter 1 to 99"; return@setOnClickListener }
            val product = products[sp.selectedItemPosition]
            listDao.set(product.id, packs * product.packSize)
            refresh()
            dialog.dismiss()
        }
    }

    private fun refresh() {
        val byId = productDao.all().associateBy { it.id }
        val rows = listDao.all().mapNotNull { item ->
            val p = byId[item.productId] ?: return@mapNotNull null
            ListRow(p.id, p.name, formatPack(item.quantity, p.unit))
        }
        adapter.submit(rows)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    // Mode and k travel to the next screen as Intent extras.
    private fun calculate() {
        if (listDao.all().isEmpty()) {
            Toast.makeText(requireContext(), "Add at least one product first", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(requireContext(), BasketResultActivity::class.java)
                .putExtra(BasketResultActivity.EXTRA_MODE,
                    if (rbSplit.isChecked) BasketResultActivity.MODE_SPLIT else BasketResultActivity.MODE_SINGLE)
                .putExtra(BasketResultActivity.EXTRA_K, spK.selectedItemPosition + 1)
        )
    }
}