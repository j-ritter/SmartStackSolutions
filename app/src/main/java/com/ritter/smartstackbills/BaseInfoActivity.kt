package com.ritter.smartstackbills

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

abstract class BaseInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_info_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.infoPageRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialToolbar>(R.id.infoToolbar).setNavigationOnClickListener {
            finish()
        }
    }

    protected fun setupPage(
        @StringRes title: Int,
        @StringRes subtitle: Int,
        @DrawableRes icon: Int
    ) {
        findViewById<MaterialToolbar>(R.id.infoToolbar).title = getString(title)
        findViewById<TextView>(R.id.infoSubtitle).setText(subtitle)
        findViewById<ImageView>(R.id.infoIcon).setImageResource(icon)
    }

    protected fun showTextContent(
        @StringRes content: Int,
        image: Drawable? = null,
        autoLinkEmail: Boolean = false
    ) {
        val contentView = findViewById<TextView>(R.id.infoTextContent)
        contentView.setText(content)
        contentView.visibility = View.VISIBLE
        if (autoLinkEmail) {
            Linkify.addLinks(contentView, Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS)
        }

        val imageView = findViewById<ImageView>(R.id.infoPageImage)
        if (image != null) {
            imageView.setImageDrawable(image)
            imageView.visibility = View.VISIBLE
        }
    }

    protected fun showExpandableContent(
        questionPrefix: String,
        answerPrefix: String,
        count: Int
    ) {
        val container = findViewById<LinearLayout>(R.id.infoAccordionContainer)
        container.visibility = View.VISIBLE

        repeat(count) { index ->
            val number = index + 1
            val questionId = resources.getIdentifier(
                "${questionPrefix}_$number",
                "string",
                packageName
            )
            val answerId = resources.getIdentifier(
                "${answerPrefix}_$number",
                "string",
                packageName
            )
            if (questionId == 0 || answerId == 0) return@repeat

            val item = LayoutInflater.from(this)
                .inflate(R.layout.item_info_accordion, container, false)
            val header = item.findViewById<View>(R.id.infoAccordionHeader)
            val question = item.findViewById<TextView>(R.id.infoAccordionQuestion)
            val answer = item.findViewById<TextView>(R.id.infoAccordionAnswer)
            val arrow = item.findViewById<ImageView>(R.id.infoAccordionArrow)

            question.setText(questionId)
            answer.setText(answerId)
            header.contentDescription = getString(questionId)
            header.setOnClickListener {
                val expand = answer.visibility != View.VISIBLE
                answer.visibility = if (expand) View.VISIBLE else View.GONE
                arrow.animate().rotation(if (expand) 180f else 0f).setDuration(180L).start()
            }
            container.addView(item)
        }
    }
}
