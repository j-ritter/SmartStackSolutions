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

        val registertext: TextView = findViewById(R.id.txtSign)

        registertext.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

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
                    // Check if the welcome dialog should be shown
                    if (isFirstLogin()) {
                        showWelcomeDialog(emailInput)
                    } else {
                        navigateToMainMenu(emailInput)
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    baseContext,
                    "Authentication failed.${it.localizedMessage}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
    }

    private fun showWelcomeDialog(emailInput: String) {
        // Create the welcome dialog
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_welcome, null)
        builder.setView(dialogView)

        val btnGetStarted: Button = dialogView.findViewById(R.id.btnGetStarted)
        val dialog = builder.create()

        btnGetStarted.setOnClickListener {
            dialog.dismiss()
            setFirstLoginFlag(false) // Mark as not first login anymore
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
