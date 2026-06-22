package com.ritter.smartstackbills

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GettingStartedActivity : AppCompatActivity() {
    private data class Step(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        @StringRes val description: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_getting_started)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gettingStartedRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.gettingStartedToolbar)
            .setNavigationOnClickListener { finish() }

        val steps = listOf(
            Step(R.drawable.ic_nav_open_payments, R.string.guide_step_1_title, R.string.guide_step_1_description),
            Step(R.drawable.ic_nav_income, R.string.guide_step_2_title, R.string.guide_step_2_description),
            Step(R.drawable.ic_nav_closed_payments, R.string.guide_step_3_title, R.string.guide_step_3_description),
            Step(R.drawable.baseline_data_thresholding_24, R.string.guide_step_4_title, R.string.guide_step_4_description),
            Step(R.drawable.ic_notifications_bell, R.string.guide_step_5_title, R.string.guide_step_5_description)
        )
        val container = findViewById<LinearLayout>(R.id.gettingStartedSteps)
        steps.forEachIndexed { index, step ->
            val view = LayoutInflater.from(this)
                .inflate(R.layout.item_getting_started_step, container, false)
            view.findViewById<TextView>(R.id.guideStepNumber).text = (index + 1).toString()
            view.findViewById<ImageView>(R.id.guideStepIcon).setImageResource(step.icon)
            view.findViewById<TextView>(R.id.guideStepTitle).setText(step.title)
            view.findViewById<TextView>(R.id.guideStepDescription).setText(step.description)
            container.addView(view)
        }

        findViewById<android.view.View>(R.id.gettingStartedAddPayment).setOnClickListener {
            EntryCreationFlow.show(this, EntryType.OPEN_PAYMENT, AuthUtils.currentUserEmail())
        }
        findViewById<android.view.View>(R.id.gettingStartedFaq).setOnClickListener {
            startActivity(Intent(this, FAQs::class.java))
        }
    }
}
