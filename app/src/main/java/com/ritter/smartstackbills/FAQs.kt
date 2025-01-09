package com.ritter.smartstackbills

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageView
import android.widget.LinearLayout

class FAQs : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_faqs)

        // Handle back button click
        val btnBackFAQs: ImageView = findViewById(R.id.btnBackFAQs)
        btnBackFAQs.setOnClickListener {
            onBackPressed()  // Go back to the previous activity
        }

        setupExpandableSection(R.id.arrow_1, R.id.answer_section_1)
        setupExpandableSection(R.id.arrow_2, R.id.answer_section_2)
        setupExpandableSection(R.id.arrow_3, R.id.answer_section_3)
        setupExpandableSection(R.id.arrow_4, R.id.answer_section_4)
        setupExpandableSection(R.id.arrow_5, R.id.answer_section_5)
        setupExpandableSection(R.id.arrow_6, R.id.answer_section_6)
        setupExpandableSection(R.id.arrow_7, R.id.answer_section_7)
        setupExpandableSection(R.id.arrow_8, R.id.answer_section_8)
        setupExpandableSection(R.id.arrow_9, R.id.answer_section_9)
        setupExpandableSection(R.id.arrow_10, R.id.answer_section_10)
        setupExpandableSection(R.id.arrow_11, R.id.answer_section_11)
        setupExpandableSection(R.id.arrow_12, R.id.answer_section_12)
    }
    private fun setupExpandableSection(arrowId: Int, answerSectionId: Int)  {

        val arrow: ImageView = findViewById(arrowId)
        val answerSection: LinearLayout = findViewById(answerSectionId)

        arrow.setOnClickListener {
            if (answerSection.visibility == View.GONE) {
                answerSection.visibility = View.VISIBLE
                arrow.animate().rotation(180f).start()
            } else {
                answerSection.visibility = View.GONE
                arrow.animate().rotation(0f).start()
            }
        }


        // Apply window insets for edge-to-edge experience
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scroll_view_faqs)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
