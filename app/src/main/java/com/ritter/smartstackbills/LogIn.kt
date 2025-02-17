package com.ritter.smartstackbills

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class LogIn : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_log_in)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("SmartStackBillsPrefs", MODE_PRIVATE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle Register Link
        val registerText: TextView = findViewById(R.id.txtSign)
        registerText.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        // Handle Forgot Password Link
        val forgotPasswordText: TextView = findViewById(R.id.txtForgotPassword)
        forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }

        // Handle Login Button
        val loginButton: Button = findViewById(R.id.btnlog)
        loginButton.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val email: EditText = findViewById(R.id.edtEmail)
        val password: EditText = findViewById(R.id.edtPassword)

        if (email.text.isEmpty() || password.text.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val emailInput = email.text.toString()
        val passwordInput = password.text.toString()
        auth.signInWithEmailAndPassword(emailInput, passwordInput)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (isFirstLogin()) {
                        showWelcomeDialog(emailInput)
                    } else {
                        navigateToMainMenu(emailInput)
                    }
                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Authentication failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showForgotPasswordDialog() {
        val emailInput = findViewById<EditText>(R.id.edtEmail).text.toString()

        if (emailInput.isEmpty()) {
            Toast.makeText(this, "Please enter your email address to reset your password.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(emailInput)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent. Please check your inbox.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send password reset email.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showWelcomeDialog(emailInput: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_welcome, null)
        builder.setView(dialogView)

        val btnGetStarted: Button = dialogView.findViewById(R.id.btnGetStarted)
        val dialog = builder.create()

        btnGetStarted.setOnClickListener {
            dialog.dismiss()
            setFirstLoginFlag(false)
            navigateToMainMenu(emailInput)
        }

        dialog.show()
    }

    private fun navigateToMainMenu(emailInput: String) {
        val intent = Intent(this, MainMenu::class.java)
        intent.putExtra("USER_EMAIL", emailInput)
        startActivity(intent)
    }

    private fun isFirstLogin(): Boolean {
        return sharedPreferences.getBoolean("isFirstLogin", true)
    }

    private fun setFirstLoginFlag(isFirst: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isFirstLogin", isFirst)
        editor.apply()
    }
}
