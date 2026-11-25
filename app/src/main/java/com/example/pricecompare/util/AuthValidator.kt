package com.example.pricecompare.util

import android.util.Patterns

// Validates email and password input locally before sending Firebase Auth requests.
// Used by LoginActivity and SignUpActivity to give instant feedback offline.
object AuthValidator {
    const val MIN_PASSWORD = 6   // Firebase's own minimum

    // Returns an error string if trimmed `email` is not a valid email format, or null when valid.
    fun emailError(email: String): String? =
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) "Enter a valid email" else null

    // Returns an error string if `password` is blank or shorter than 6 characters, or null when valid.
    fun passwordError(password: String): String? = when {
        password.isBlank() -> "Password cannot be blank"
        password.length < MIN_PASSWORD -> "Use at least $MIN_PASSWORD characters"
        else -> null
    }
}