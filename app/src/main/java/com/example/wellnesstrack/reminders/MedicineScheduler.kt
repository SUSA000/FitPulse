package com.example.wellnesstrack.reminders

import android.content.Context
import com.example.wellnesstrack.data.Prefs

object MedicineScheduler {
    fun reschedule(context: Context) {
        val prefs = Prefs(context)
        // cancel existing alarms for all stored reminders
        val existing = prefs.getMedicineReminders()
        existing?.forEach { t ->
            try { AlarmHelper.cancel(context, t) } catch (e: Exception) { }
        }

        if (!prefs.isMedicineRemindersEnabled()) return
        // schedule alarms for each stored time
        prefs.getMedicineReminders()?.forEach { t ->
            try { AlarmHelper.scheduleExact(context, t) } catch (e: Exception) { }
        }
    }

    fun cancel(context: Context) {
        val prefs = Prefs(context)
        prefs.getMedicineReminders()?.forEach { t ->
            try { AlarmHelper.cancel(context, t) } catch (e: Exception) { }
        }
    }
}