package com.example.a0725.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build


object Notif {
    const val CHANNEL_ID = "events"
    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "賽事提醒", NotificationManager.IMPORTANCE_HIGH)
            mgr.createNotificationChannel(ch)
        }
    }
}
