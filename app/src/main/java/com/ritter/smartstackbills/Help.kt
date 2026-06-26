package com.ritter.smartstackbills

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

class Help : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_help_support)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.helpRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialToolbar>(R.id.helpToolbar).setNavigationOnClickListener { finish() }
        findViewById<CardView>(R.id.helpFaqCard).setOnClickListener {
            startActivity(Intent(this, FAQs::class.java))
        }
        findViewById<CardView>(R.id.helpGettingStartedCard).setOnClickListener {
            startActivity(Intent(this, GettingStartedActivity::class.java))
        }
        findViewById<CardView>(R.id.helpDataCard).setOnClickListener {
            startActivity(Intent(this, DataAccountActivity::class.java))
        }
        findViewById<CardView>(R.id.helpEmailCard).setOnClickListener {
            openSupportEmail()
        }
    }

    private fun openSupportEmail() {
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull().orEmpty()
        val subject = getString(R.string.support_email_subject)
        val body = getString(R.string.support_email_template, versionName)
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse(
                "mailto:$SUPPORT_EMAIL" +
                    "?subject=${Uri.encode(subject)}" +
                    "&body=${Uri.encode(body)}"
            )
        )

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.email), SUPPORT_EMAIL))
            Toast.makeText(this, R.string.support_email_copied, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val SUPPORT_EMAIL = "appdev.jr2024@gmail.com"
    }
}
