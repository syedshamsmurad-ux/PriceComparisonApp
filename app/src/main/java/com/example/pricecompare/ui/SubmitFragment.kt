package com.example.pricecompare.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.pricecompare.R
import com.example.pricecompare.db.PriceDao
import com.example.pricecompare.db.ProductDao
import com.example.pricecompare.db.ShopDao
import com.example.pricecompare.logic.OutlierDetector
import com.example.pricecompare.logic.PriceSelector
import com.example.pricecompare.logic.UnitNormalizer
import com.example.pricecompare.model.PriceEntry
import com.example.pricecompare.model.Product
import com.example.pricecompare.model.Shop
import java.math.BigDecimal
import java.math.RoundingMode
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.CancellationSignal
import com.example.pricecompare.adapters.formatDistance
import com.example.pricecompare.logic.GeoUtils
import com.example.pricecompare.util.LocationHelper
import com.example.pricecompare.sync.SyncManager
import com.google.firebase.auth.FirebaseAuth

// Submit tab: validate -> outlier check -> save to SQLite (Firestore upload comes in Phase 5).
class SubmitFragment : Fragment(R.layout.fragment_submit) {

    private companion object {
        val MAX_PKR = BigDecimal(100_000)     // sensible range: Rs 1 to Rs 100,000 per pack
        const val MIN_PAISA = 100L
        const val MAX_PACK = 1_000_000        // base units (g / ml / pieces)
    }

    private lateinit var products: List<Product>
    private lateinit var shops: List<Shop>
    private lateinit var spProduct: Spinner
    private lateinit var spShop: Spinner
    private lateinit var etPrice: EditText
    private lateinit var etPack: EditText
    private lateinit var packLabel: TextView
    private lateinit var priceDao: PriceDao

    private var locSignal: CancellationSignal? = null

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted -> if (granted.values.any { it }) selectNearest() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        priceDao = PriceDao(ctx)
        products = ProductDao(ctx).all()
        shops = ShopDao(ctx).all()

        spProduct = view.findViewById(R.id.sp_product)
        spShop = view.findViewById(R.id.sp_shop)
        etPrice = view.findViewById(R.id.et_price)
        etPack = view.findViewById(R.id.et_pack)
        packLabel = view.findViewById(R.id.tv_pack_label)

        spProduct.adapter = ArrayAdapter(
            ctx, android.R.layout.simple_spinner_dropdown_item, products.map { it.name })
        spShop.adapter = ArrayAdapter(
            ctx, android.R.layout.simple_spinner_dropdown_item, shops.map { it.name })

        // Picking a product pre-fills its standard pack size (the user may change it).
        spProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val product = products[pos]
                etPack.setText(product.packSize.toString())
                val unitName = when (product.unit) { "piece" -> "pieces"; else -> product.unit }
                packLabel.text = "Pack size (in $unitName)"
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        view.findViewById<Button>(R.id.btn_submit).setOnClickListener { submit() }
        if (LocationHelper.hasPermission(ctx)) selectNearest()
        else permLauncher.launch(LocationHelper.PERMISSIONS)
    }

    private fun submit() {
        val product = products[spProduct.selectedItemPosition]
        val shop = shops[spShop.selectedItemPosition]

        // 1. Validate. Return early with an error shown on the field.
        val paisa = parsePaisa(etPrice.text.toString())
        if (paisa == null || paisa < MIN_PAISA) {
            etPrice.error = "Enter a price from Rs 1 to Rs 100,000"
            return
        }
        val pack = etPack.text.toString().toIntOrNull()
        if (pack == null || pack < 1 || pack > MAX_PACK) {
            etPack.error = "Enter a pack size greater than 0"
            return
        }

        // 2. Outlier check against fresh VERIFIED prices, compared per kg / litre / piece.
        val now = System.currentTimeMillis()
        val recent = priceDao.byProduct(product.id)
            .filter { it.status == "verified" && PriceSelector.isFresh(it.createdAt, now) }
            .map { UnitNormalizer.unitPricePaisa(it.pricePaisa, it.packSize, product.unit) }
        val newUnit = UnitNormalizer.unitPricePaisa(paisa, pack, product.unit)
        val outlier = OutlierDetector.isOutlier(newUnit, recent)

        // 3. Save locally. Outliers are stored as "unverified" and stay out of baskets.
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        priceDao.insert(PriceEntry(
            productId = product.id, shopId = shop.id, pricePaisa = paisa, packSize = pack,
            createdAt = now, userId = uid,
            // Same text is the Firestore document ID, so a retry can never create a duplicate.
            remoteId = uid?.let { "${it}_${now}_${product.id}_${shop.id}" },
            status = if (outlier) "unverified" else "verified"
        ))

        SyncManager.syncNow(requireContext())   // uploads now if online; otherwise stays queuedetPrice.text.clear()

        Toast.makeText(
            requireContext(),
            if (outlier) "Saved as UNVERIFIED: far from usual prices. Not used in baskets until confirmed."
            else "Price saved",
            Toast.LENGTH_LONG
        ).show()
    }

    // Pre-select the nearest shop. Silent if there is no fix or the screen already closed.
    private fun selectNearest() {
        locSignal = LocationHelper.getLocation(requireContext()) { loc ->
            if (loc == null || !isAdded || view == null) return@getLocation
            val nearest = GeoUtils.sortByDistance(shops, loc.latitude, loc.longitude)
                .firstOrNull() ?: return@getLocation
            spShop.setSelection(shops.indexOf(nearest.first))   // same order as the spinner
            Toast.makeText(
                requireContext(),
                "Nearest shop selected: ${nearest.first.name} (${formatDistance(nearest.second)} away)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        locSignal?.cancel()
        super.onDestroyView()
    }

    // "450.50" -> 45050. BigDecimal avoids floating-point rounding errors.
    private fun parsePaisa(text: String): Long? =
        text.trim().toBigDecimalOrNull()
            ?.takeIf { it.signum() > 0 && it <= MAX_PKR }   // range check before toLong()
            ?.movePointRight(2)
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.toLong()


}