package com.ritter.smartstackbills

import android.os.Bundle

class Help : BaseInfoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupPage(R.string.help_title, R.string.help_subtitle, R.drawable.ic_nav_help)
        showTextContent(R.string.help_content, autoLinkEmail = true)
    }
}
