package com.example.wellnesstrack.reminders

import android.content.Context
import com.example.wellnesstrack.data.Prefs

object SleepScheduler {
    fun reschedule(context: Context) {
        val prefs = Prefs(context)
        try { AlarmHelper.cancelForType(context, "sleep") } catch (e: Exception) { }
        if (!prefs.isSleepReminderEnabled()) return
        val t = prefs.getSleepReminderTime()
        try { AlarmHelper.scheduleExactForType(context, t, "sleep") } catch (e: Exception) { }
    }
}
