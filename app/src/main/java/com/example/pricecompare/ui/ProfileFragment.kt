package com.example.pricecompare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.pricecompare.R
import com.example.pricecompare.db.PriceDao
import com.example.pricecompare.sync.SyncManager
import com.google.firebase.auth.FirebaseAuth

// Profile tab: shows who is signed in, lets the user sync by hand, and logs out.
// Hosted by MainActivity; the sync work itself lives in SyncManager.
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    // Kept as fields so the sync callback can update them later.
    private lateinit var status: TextView
    private lateinit var btnSync: Button

    // Runs once the layout exists, so findViewById is safe here.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        view.findViewById<TextView>(R.id.tv_email).text = auth.currentUser?.email ?: "Not signed in"
        status = view.findViewById(R.id.tv_sync_status)
        btnSync = view.findViewById(R.id.btn_sync)

        // Manual sync: force = true skips the 30-second throttle used by automatic triggers.
        btnSync.setOnClickListener {
            btnSync.isEnabled = false
            status.text = "Syncing…"
            SyncManager.syncNow(requireContext(), force = true) { r ->
                // Runs on the UI thread, but the screen may have closed meanwhile.
                if (!isAdded || this.view == null) return@syncNow
                btnSync.isEnabled = true
                status.text = r.message + "\n" + pendingText()
            }
        }
        // Log out, then open the login screen with an empty back stack.
        view.findViewById<Button>(R.id.btn_logout).setOnClickListener {
            auth.signOut()
            startActivity(
                Intent(requireContext(), LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        status.text = pendingText()
    }

    // Counts this user's saved prices that are still waiting to be uploaded.
    private fun pendingText(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return ""
        return "Waiting to upload: ${PriceDao(requireContext()).pendingCount(uid)}"
    }
}