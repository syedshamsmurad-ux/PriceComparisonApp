package com.example.pricecompare.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

// Turns Firebase exceptions into short messages a user can act on.
object AuthMessages {
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