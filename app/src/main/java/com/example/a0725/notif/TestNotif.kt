// file: notif/TestNotif.kt
package com.example.a0725.notif

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.a0725.R

/**
 * 立刻送出一則測試通知（用來驗證通知權限/頻道與顯示流程）
 */
fun sendImmediateTestNotification(
    ctx: Context,
    title: String,
    text: String,
    nid: Int = (System.currentTimeMillis() % 1_000_000).toInt()
) {
    Notif.ensureChannel(ctx)

    // Android 13+ 要有 POST_NOTIFICATIONS 權限才可送
    if (Build.VERSION.SDK_INT >= 33) {
        val granted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
    }

    val nm = NotificationManagerCompat.from(ctx)
    if (!nm.areNotificationsEnabled()) return

    val n = NotificationCompat.Builder(ctx, Notif.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    try {
        nm.notify(nid, n)
    } catch (_: SecurityException) {
        // 沒權限或被系統攔截就不送；不讓 App 崩潰
    }
}
