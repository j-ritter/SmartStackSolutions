package com.ritter.smartstackbills

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnsign: Button = findViewById(R.id.btnSignUp)

        btnsign.setOnClickListener{
            val intent = Intent(this,SignUp::class.java)
            startActivity(intent)
        }


        val btnlog: Button = findViewById(R.id.btnLogIn)

        btnlog.setOnClickListener{
            val intent = Intent(this,LogIn::class.java)
            startActivity(intent)
        }
    }
}