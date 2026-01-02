package com.example.a0725.notif

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object NotifyGate {
    private const val PREFS = "app_settings"
    private const val KEY_NOTIFY_ENABLED = "notifyEnabled"

    /** 讀本機偏好（預設 true） */
    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFY_ENABLED, true)

    /** 寫本機偏好 */
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NOTIFY_ENABLED, enabled).apply()
    }

    /** Android 13+ 是否已拿到 POST_NOTIFICATIONS */
    fun hasPostNotifPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    /** 是否可立即顯示通知（同時考慮開關＋權限） */
    fun canPostNow(context: Context): Boolean =
        isEnabled(context) && hasPostNotifPermission(context)
}
