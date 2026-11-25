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

// Sign-up screen: creates a Firebase account from an email and a password.
// Firebase signs the new user in automatically, so success goes straight to MainActivity.
// Opened from the "Sign up" link on LoginActivity.
class SignUpActivity : AppCompatActivity() {

    // Kept as fields so setBusy() can disable the button and show the spinner while we wait.
    private lateinit var btnSignUp: Button
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        // Keep content clear of the status bar and the gesture bar.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left + 48, bars.top + 48, bars.right + 48, bars.bottom + 48)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etConfirm = findViewById<EditText>(R.id.et_confirm)
        btnSignUp = findViewById(R.id.btn_signup)
        progress = findViewById(R.id.progress)

        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            // Validate locally first; stop at the first problem.
            AuthValidator.emailError(email)?.let { etEmail.error = it; return@setOnClickListener }
            AuthValidator.passwordError(password)?.let { etPassword.error = it; return@setOnClickListener }
            if (password != etConfirm.text.toString()) {
                etConfirm.error = "Passwords do not match"; return@setOnClickListener
            }

            setBusy(true)
            // Ask Firebase to create the account; the callback runs when it answers.
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    setBusy(false)
                    if (task.isSuccessful) {
                        // Clear the back stack so Back cannot return to the sign-up screen.
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    } else {
                        Toast.makeText(this, AuthMessages.friendly(task.exception), Toast.LENGTH_LONG).show()
                    }
                }
        }
        findViewById<TextView>(R.id.tv_back).setOnClickListener { finish() }
    }

    // Shows the spinner and disables the button while waiting for Firebase.
    private fun setBusy(busy: Boolean) {
        btnSignUp.isEnabled = !busy
        progress.visibility = if (busy) View.VISIBLE else View.GONE
    }
}