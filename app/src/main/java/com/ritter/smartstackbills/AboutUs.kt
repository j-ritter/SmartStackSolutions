package com.ritter.smartstackbills

import android.os.Bundle

class AboutUs : BaseInfoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupPage(R.string.about_us_title, R.string.about_us_subtitle, R.drawable.ic_nav_about)
        showTextContent(R.string.about_us_content, getDrawable(R.drawable.image_aboutus))
    }
}
