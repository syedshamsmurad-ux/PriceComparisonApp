package com.example.pricecompare.util

import android.util.Patterns

// Input checks done BEFORE calling Firebase, so bad input never costs a network call.
object AuthValidator {
    const val MIN_PASSWORD = 6   // Firebase's own minimum

    fun emailError(email: String): String? =
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Enter a valid email" else null

    fun passwordError(password: String): String? =
        if (password.length < MIN_PASSWORD) "Use at least $MIN_PASSWORD characters" else null
}