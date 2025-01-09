package com.ritter.smartstackbills

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class Terms : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        // Handle back button click
        val btnBackTerms: ImageView = findViewById(R.id.btnBackTerms)
        btnBackTerms.setOnClickListener {
            onBackPressed()  // Go back to the previous activity
        }
    }
}
