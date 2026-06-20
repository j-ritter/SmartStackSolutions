package com.ritter.smartstackbills

import android.os.Bundle

class Terms : BaseInfoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupPage(
            R.string.terms_and_conditions_title,
            R.string.terms_subtitle,
            R.drawable.ic_nav_terms
        )
        showTextContent(R.string.terms_and_conditions_content, autoLinkEmail = true)
    }
}
