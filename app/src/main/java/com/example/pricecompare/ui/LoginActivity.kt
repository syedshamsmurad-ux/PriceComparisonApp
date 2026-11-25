package com.example.pricecompare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pricecompare.R
import com.example.pricecompare.util.AuthMessages
import com.example.pricecompare.util.AuthValidator
import com.google.firebase.auth.FirebaseAuth

// Login screen (email and password) using Firebase Authentication.
// Firebase remembers the session, so a user who is already signed in skips straight to MainActivity.
// Reached from MainActivity's login check and from the log out button in ProfileFragment.
class LoginActivity : AppCompatActivity() {

    // Kept as fields so setBusy() can disable the button and show the spinner while we wait.
    private lateinit var btnLogin: Button
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) { openMain(); return }   // already signed in

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        // Keep content clear of the status bar and the gesture bar.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left + 48, bars.top + 48, bars.right + 48, bars.bottom + 48)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        progress = findViewById(R.id.progress)

        // On click: check the input on the phone first, then ask Firebase to sign in.
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            AuthValidator.emailError(email)?.let { etEmail.error = it; return@setOnClickListener }
            if (password.isEmpty()) { etPassword.error = "Enter your password"; return@setOnClickListener }

            setBusy(true)
            // Passing `this` ties the callback to this Activity's lifecycle (no leak after it closes).
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                setBusy(false)
                if (task.isSuccessful) openMain()
                else Toast.makeText(this, AuthMessages.friendly(task.exception), Toast.LENGTH_LONG).show()
            }
        }
        findViewById<TextView>(R.id.tv_signup).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    // Shows the spinner and disables the button while waiting for Firebase.
    private fun setBusy(busy: Boolean) {
        btnLogin.isEnabled = !busy      // blocks double taps while waiting
        progress.visibility = if (busy) View.VISIBLE else View.GONE
    }

    // CLEAR_TASK wipes the back stack so Back cannot return to the login screen.
    private fun openMain() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}