package com.example.wellnesstrack.reminders

import android.content.Context
import com.example.wellnesstrack.data.Prefs

object HabitScheduler {
    fun reschedule(context: Context) {
        val prefs = Prefs(context)
        // cancel any previous habit alarm using the habit time code
        try { AlarmHelper.cancelForType(context, "habit") } catch (e: Exception) { }

        if (!prefs.isHabitReminderEnabled()) return
        val t = prefs.getHabitReminderTime()
        try { AlarmHelper.scheduleExactForType(context, t, "habit") } catch (e: Exception) { }
    }
}
