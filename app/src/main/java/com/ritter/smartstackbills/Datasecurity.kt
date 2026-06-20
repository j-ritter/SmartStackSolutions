package com.ritter.smartstackbills

import android.os.Bundle

class Datasecurity : BaseInfoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupPage(
            R.string.data_security_title,
            R.string.data_security_subtitle,
            R.drawable.ic_nav_security
        )
        showExpandableContent("data_security_question", "data_security_answer", 14)
    }
}
