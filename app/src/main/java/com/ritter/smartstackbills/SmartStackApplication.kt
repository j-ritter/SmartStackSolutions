package com.ritter.smartstackbills

import android.app.Application
import com.google.firebase.FirebaseApp

class SmartStackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        AppCheckProvider.install()
    }
}
