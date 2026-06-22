package com.ritter.smartstackbills

import android.os.Bundle

class FAQs : BaseInfoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupPage(R.string.faqs, R.string.faqs_subtitle, R.drawable.ic_nav_faq)
        showExpandableContent("faqs_question", "faqs_answer", 20)
    }
}
