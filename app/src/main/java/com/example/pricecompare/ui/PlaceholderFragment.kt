package com.example.pricecompare.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment

// Stand-in screen for tabs we haven't built yet.
class PlaceholderFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): TextView = TextView(requireContext()).apply {
        text = requireArguments().getString(ARG_TEXT)
        gravity = Gravity.CENTER
        textSize = 18f
    }

    companion object {
        private const val ARG_TEXT = "text"
        // Pass data through a Bundle, never a constructor (Android recreates fragments with no-arg).
        fun newInstance(text: String) =
            PlaceholderFragment().apply { arguments = bundleOf(ARG_TEXT to text) }
    }
}