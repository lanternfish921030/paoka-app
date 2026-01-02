package com.example.a0725

import android.app.Application
import com.example.a0725.notif.Notif
import com.google.firebase.firestore.FirebaseFirestore


class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notif.ensureChannel(this)
        FirebaseFirestore.setLoggingEnabled(true)
    }
}
