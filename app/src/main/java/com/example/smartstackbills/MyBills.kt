package com.example.smartstackbills

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MyBills : AppCompatActivity() {
    private var userEmail: String? = null // Declarar userEmail como variable de instancia

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_bills)

        // Obtener el email del Intent
        userEmail = intent.getStringExtra("USER_EMAIL")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnCreate: Button = findViewById(R.id.btnCreate)
        btnCreate.setOnClickListener {
            val intent = Intent(this, createBill::class.java)
            intent.putExtra("USER_EMAIL", userEmail) // Pasar el correo electrónico
            startActivity(intent)
        }
    }
}