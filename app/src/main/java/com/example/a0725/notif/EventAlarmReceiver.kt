package com.example.a0725.notif

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.a0725.R

class EventAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // ⬇️ 新增：關閉時直接忽略，不發通知
        if (!NotifyGate.canPostNow(context)) return
        // 先確認頻道
        Notif.ensureChannel(context)

        // ⬇️【新增】Android 13+ 動態權限檢查，沒權限就不要送通知避免 SecurityException
        if (Build.VERSION.SDK_INT >= 33) {
            val hasPostNotif = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPostNotif) return
        }

        // ⬇️【建議】使用者整體通知開關也可能關閉，此時直接跳過
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val title = intent.getStringExtra("title") ?: "賽事提醒"
        val text  = intent.getStringExtra("text")  ?: ""
        val nid   = intent.getIntExtra("nid", (System.currentTimeMillis() % 1_000_000).toInt())

        val n = NotificationCompat.Builder(context, Notif.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // ⬇️【修改】加 try-catch，確保偶發的 SecurityException 不會讓 app 當掉
        try {
            nm.notify(nid, n)
        } catch (_: SecurityException) {
            // 可加上 log，上報或忽略
        }
    }
}
