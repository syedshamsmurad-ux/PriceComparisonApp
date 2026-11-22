package com.example.pricecompare.ui

import android.os.Bundle
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.pricecompare.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Auth gate: no signed-in user -> go to login and stop building this screen.
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Keep content clear of the status bar and gesture bar.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_browse -> BrowseFragment()
                R.id.nav_list -> ListFragment()
                R.id.nav_submit -> SubmitFragment()
                else -> ProfileFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            true
        }
        // Only on first launch; after rotation the FragmentManager restores the screen itself.
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_browse
    }
}