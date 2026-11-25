package com.example.pricecompare.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

// Maps technical Firebase Auth exceptions into plain, user-friendly error messages.
// Used by LoginActivity and SignUpActivity when sign-in or sign-up fails.
object AuthMessages {
    // Returns a short readable message describing why the given Firebase exception happened.
    fun friendly(e: Exception?): String = when (e) {
        // Weak-password must come BEFORE InvalidCredentials: it is a subclass of it.
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Use 6 or more characters."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Wrong email or password."
        is FirebaseNetworkException -> "No internet connection. Try again when online."
        else -> "Something went wrong. Please try again."
    }
}