package com.example.pricecompare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.pricecompare.R
import com.google.firebase.auth.FirebaseAuth

// Profile tab: who is signed in + log out.
class ProfileFragment : Fragment(R.layout.fragment_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        view.findViewById<TextView>(R.id.tv_email).text = auth.currentUser?.email ?: "Not signed in"
        view.findViewById<Button>(R.id.btn_logout).setOnClickListener {
            auth.signOut()
            // New task + clear task: nothing from the logged-in session stays on the back stack.
            startActivity(
                Intent(requireContext(), LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }
}