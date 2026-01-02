package com.example.a0725.notif

import android.content.Context
import androidx.core.content.edit

object CalendarLinkStore {
    private const val PREF = "calendar_links"

    fun save(context: Context, scheduleEventId: String, calendarEventId: Long) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit {
            putLong(scheduleEventId, calendarEventId)
        }
    }

    fun get(context: Context, scheduleEventId: String): Long? {
        val v = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(scheduleEventId, -1L)
        return if (v == -1L) null else v
    }

    fun remove(context: Context, scheduleEventId: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit {
            remove(scheduleEventId)
        }
    }
}
