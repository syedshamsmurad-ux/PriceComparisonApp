package com.example.pricecompare.ui

import android.content.ContentUris
import android.content.ContentValues
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
import com.example.pricecompare.provider.CatalogContract

// List tab: build the shopping list through ContentResolver, pick a mode, open the basket screen.
class ListFragment : Fragment(R.layout.fragment_list) {

    private lateinit var productDao: ProductDao
    private lateinit var adapter: ShoppingItemAdapter
    private lateinit var empty: TextView
    private lateinit var spK: Spinner
    private lateinit var rbSplit: RadioButton

    // Tracks the number of rows loaded at the last refresh so Calculate works without a DAO call.
    private var itemCount: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        productDao = ProductDao(ctx)
        empty = view.findViewById(R.id.tv_empty)
        spK = view.findViewById(R.id.sp_k)
        rbSplit = view.findViewById(R.id.rb_split)

        adapter = ShoppingItemAdapter { productId -> removeItem(productId) }
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
        view.findViewById<Button>(R.id.btn_clear).setOnClickListener { clearList() }
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
            // Insert through ContentResolver; CONFLICT_REPLACE in CatalogProvider updates existing items.
            val values = ContentValues().apply {
                put(CatalogContract.ShoppingList.COL_PRODUCT_ID, product.id)
                put(CatalogContract.ShoppingList.COL_QUANTITY, packs * product.packSize)
            }
            requireContext().contentResolver.insert(CatalogContract.SHOPPING_LIST_URI, values)
            refresh()
            dialog.dismiss()
        }
    }

    // Deletes a single item using the shopping_list/# URI (where # is productId).
    private fun removeItem(productId: Long) {
        val itemUri = ContentUris.withAppendedId(CatalogContract.SHOPPING_LIST_URI, productId)
        requireContext().contentResolver.delete(itemUri, null, null)
        refresh()
    }

    // Clears every row in shopping_list through the collection URI.
    private fun clearList() {
        requireContext().contentResolver.delete(CatalogContract.SHOPPING_LIST_URI, null, null)
        refresh()
    }

    // Queries the joined shopping_list cursor from CatalogProvider and updates the UI.
    private fun refresh() {
        val rows = mutableListOf<ListRow>()
        requireContext().contentResolver.query(
            CatalogContract.SHOPPING_LIST_URI, null, null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CatalogContract.ShoppingList.COL_ID)
            val nameCol = cursor.getColumnIndexOrThrow(CatalogContract.ShoppingList.COL_NAME)
            val unitCol = cursor.getColumnIndexOrThrow(CatalogContract.ShoppingList.COL_UNIT)
            val qtyCol = cursor.getColumnIndexOrThrow(CatalogContract.ShoppingList.COL_QUANTITY)
            while (cursor.moveToNext()) {
                val productId = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val unit = cursor.getString(unitCol)
                val quantity = cursor.getInt(qtyCol)
                rows.add(ListRow(productId, name, formatPack(quantity, unit)))
            }
        }
        // Save the count from this refresh so calculate() does not query the DAO directly.
        itemCount = rows.size
        adapter.submit(rows)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    // Mode and k travel to the next screen as Intent extras.
    private fun calculate() {
        if (itemCount == 0) {
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
