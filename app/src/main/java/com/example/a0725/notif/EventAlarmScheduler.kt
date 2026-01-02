// file: notif/EventAlarmScheduler.kt
package com.example.a0725.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.a0725.model.ScheduleEvent
import java.time.*
import java.time.format.DateTimeFormatter

private fun pending(
    ctx: Context, requestCode: Int, title: String, text: String
): PendingIntent {
    val i = Intent(ctx, EventAlarmReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("text",  text)
        putExtra("nid",   requestCode)
    }
    return PendingIntent.getBroadcast(
        ctx, requestCode, i,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun millisOf(localDateTime: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()) =
    localDateTime.atZone(zone).toInstant().toEpochMilli()

/** 依 Event 的 days 逐一排程；會在「時間點前 minutesBefore 分鐘」提醒。 */
fun scheduleEventAlarms(ctx: Context, ev: ScheduleEvent, minutesBefore: Long = 30) {
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val zone = ZoneId.systemDefault()
    val hhmm = DateTimeFormatter.ofPattern("HH:mm")

    ev.days.forEachIndexed { index, day ->
        if (day.date.isBlank()) return@forEachIndexed

        val date = LocalDate.parse(day.date, DateTimeFormatter.ISO_LOCAL_DATE)

        // 找這一天最早的時間
        val times = day.projects.mapNotNull { p -> p.checkInTime.takeIf { it.matches(Regex("\\d{2}:\\d{2}")) } }
            .ifEmpty { day.projects.mapNotNull { it.matchTime.takeIf { s -> s.matches(Regex("\\d{2}:\\d{2}")) } } }

        val base = if (times.isNotEmpty()) {
            LocalTime.parse(times.min(), hhmm)
        } else LocalTime.of(9, 0)

        val whenMillis = millisOf(LocalDateTime.of(date, base).minusMinutes(minutesBefore), zone)

        if (whenMillis > System.currentTimeMillis()) {
            val req = (ev.id.hashCode() * 31 + index) // 每天不同的 requestCode
            val pi = pending(
                ctx = ctx,
                requestCode = req,
                title = "${ev.name}（Day ${index + 1}）",
                text  = "即將開始（${day.date} ${base}）"
            )

            // ★ 安全設定鬧鐘，避免 Android 12+ 崩潰
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                // 沒有精確鬧鐘權限，改用非精確鬧鐘
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi)
            } else {
                // 有權限或舊版 Android，使用精確鬧鐘
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi)
            }
        }
    }
}

// 解析 yyyy-MM-dd ~ yyyy-MM-dd 的起始日
private fun parseStart(dateRange: String): LocalDate? {
    val parts = dateRange.split("~").map { it.trim() }
    if (parts.isEmpty()) return null
    return try { LocalDate.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE) } catch (_: Exception) { null }
}

/** 在賽事「前一天」的固定時刻（預設 09:00）發出一則提醒。 */
fun scheduleEventEveAlarm(
    ctx: Context,
    ev: com.example.a0725.model.ScheduleEvent,
    hour: Int = 9,
    minute: Int = 0
) {
    val start = parseStart(ev.dateRange) ?: return
    val triggerDate = start.minusDays(1)
    val ldt = LocalDateTime.of(triggerDate, LocalTime.of(hour, minute))
    val whenMillis = millisOf(ldt)

    if (whenMillis <= System.currentTimeMillis()) return  // 時間已過就不排

    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val req = ev.id.hashCode()
    val pi = pending(
        ctx = ctx,
        requestCode = req,
        title = "賽事明天開始：${ev.name}",
        text = "開始日：${start.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
    )

    // ★ 安全設定鬧鐘
    if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi)
    } else {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi)
    }
}

/** 取消前一天提醒 */
fun cancelEventEveAlarm(ctx: Context, ev: com.example.a0725.model.ScheduleEvent) {
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val req = ev.id.hashCode()
    am.cancel(pending(ctx, req, "", ""))
}