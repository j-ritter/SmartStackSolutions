package com.ritter.smartstackbills

import android.os.Bundle
import android.text.util.Linkify
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutUs : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about_us)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.aboutRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.aboutToolbar)
            .setNavigationOnClickListener { finish() }
        findViewById<TextView>(R.id.aboutContact).let {
            Linkify.addLinks(it, Linkify.EMAIL_ADDRESSES)
        }
        val versionName = packageManager
            .getPackageInfo(packageName, 0)
            .versionName
            .orEmpty()
        findViewById<TextView>(R.id.aboutVersion).text =
            getString(R.string.about_version, versionName)
    }
}
