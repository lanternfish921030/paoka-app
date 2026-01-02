package com.example.a0725.notif

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZoneOffset

object CalendarUtils {

    data class InsertResult(val success: Boolean, val eventId: Long? = null, val message: String? = null)

    fun hasCalendarPermission(context: Context): Boolean {
        val r = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val w = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        return r && w
    }

    /**
     * 找一個可寫入的行事曆（多數裝置會有使用者 Google 帳號的行事曆）。回傳 calendarId，找不到回 null。
     */
    fun findWritableCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val uri = CalendarContract.Calendars.CONTENT_URI
        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val level = c.getInt(3)
                // 讀寫權限（>= Calendars.CAL_ACCESS_CONTRIBUTOR 通常可新增事件）
                if (level >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    return id
                }
            }
        }
        return null
    }

    /**
     * 建立「整天」事件（最穩定，跨日也簡單）；endExclusive：Calendar 要求 DTEND 是結束日的隔天 00:00。
     */
    fun insertAllDayEvent(
        context: Context,
        calendarId: Long,
        title: String,
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        description: String? = null,
        location: String? = null
    ): InsertResult {
        return try {
            // ✅ 全日事件用 UTC 的 00:00 → 隔天 00:00（end 為「不含」）
            val startUtc = startDate.atStartOfDay(ZoneOffset.UTC)
            val endExclUtc = endDateInclusive.plusDays(1).atStartOfDay(ZoneOffset.UTC)

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DTSTART, startUtc.toInstant().toEpochMilli())
                put(CalendarContract.Events.DTEND, endExclUtc.toInstant().toEpochMilli())
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")   // ✅ 關鍵：UTC
                put(CalendarContract.Events.ALL_DAY, 1)              // ✅ 全日事件
                if (!description.isNullOrBlank()) put(CalendarContract.Events.DESCRIPTION, description)
                if (!location.isNullOrBlank()) put(CalendarContract.Events.EVENT_LOCATION, location)
            }

            val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLongOrNull()
            if (eventId != null) {
                InsertResult(true, eventId)
            } else {
                InsertResult(false, null, "Insert returned null id")
            }
        } catch (e: Exception) {
            InsertResult(false, null, e.message)
        }
    }

    fun updateAllDayEvent(
        context: Context,
        eventId: Long,
        title: String,
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        description: String? = null,
        location: String? = null
    ): Boolean = try {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            put(CalendarContract.Events.DTEND,   endDateInclusive.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            put(CalendarContract.Events.ALL_DAY, 1)
            if (description != null) put(CalendarContract.Events.DESCRIPTION, description) else putNull(CalendarContract.Events.DESCRIPTION)
            if (location != null) put(CalendarContract.Events.EVENT_LOCATION, location) else putNull(CalendarContract.Events.EVENT_LOCATION)
        }
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.update(uri, values, null, null) > 0
    } catch (_: Exception) { false }

    fun deleteById(context: Context, eventId: Long): Boolean = try {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.delete(uri, null, null) > 0
    } catch (_: Exception) { false }

    /** 方便：把 yyyy-MM-dd 轉 LocalDate（你的模型已經是這個格式） */
    fun parseIsoDate(iso: String?): LocalDate? = try { LocalDate.parse(iso) } catch (_: Exception) { null }

    /** 給沒有時間時的預設（例如 09:00~18:00） */
    fun defaultTimeRange(date: LocalDate, start: String? = "09:00", end: String? = "18:00"): Pair<ZonedDateTime, ZonedDateTime> {
        val zone = ZoneId.systemDefault()
        val (sh, sm) = (start ?: "09:00").split(":").map { it.toInt() }
        val (eh, em) = (end ?: "18:00").split(":").map { it.toInt() }
        val s = date.atTime(LocalTime.of(sh, sm)).atZone(zone)
        val e = date.atTime(LocalTime.of(eh, em)).atZone(zone)
        return s to e
    }

    /**
     * 嘗試用「標題 + 附近幾天」找出已存在的全日事件，回傳 eventId；找不到回 null。
     * aroundDate：用「舊日期或新日期」都行，會用 ±3 天區間去抓。
     */
    fun findExistingAllDayEventIdByTitleNearDate(
        context: Context,
        calendarId: Long,
        title: String,
        aroundDate: LocalDate,
        dayWindow: Long = 3L
    ): Long? {
        val zone = java.time.ZoneOffset.UTC // 全日事件以 UTC 儲存
        val startMillis = aroundDate.minusDays(dayWindow).atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis   = aroundDate.plusDays(dayWindow + 1).atStartOfDay(zone).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_ID
        )
        val sel = ("(${CalendarContract.Events.CALENDAR_ID}=?) AND " +
                "(${CalendarContract.Events.TITLE}=? ) AND " +
                "(${CalendarContract.Events.ALL_DAY}=1) AND " +
                "(${CalendarContract.Events.DTSTART}>=?) AND " +
                "(${CalendarContract.Events.DTSTART}<?)")
        val args = arrayOf(calendarId.toString(), title, startMillis.toString(), endMillis.toString())

        context.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, sel, args, null)?.use { c ->
            if (c.moveToFirst()) {
                return c.getLong(0) // _ID
            }
        }
        return null
    }
}
