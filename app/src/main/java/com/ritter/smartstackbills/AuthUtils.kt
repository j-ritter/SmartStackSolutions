package com.ritter.smartstackbills

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth

object AuthUtils {
    const val EXTRA_USER_EMAIL = "USER_EMAIL"

    fun currentUserEmail(): String? = FirebaseAuth.getInstance().currentUser?.email

    fun mainMenuIntent(context: Context, email: String? = currentUserEmail()): Intent {
        return Intent(context, MainMenu::class.java).apply {
            putExtra(EXTRA_USER_EMAIL, email)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    fun loginIntent(context: Context): Intent {
        return Intent(context, LogIn::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }
}
