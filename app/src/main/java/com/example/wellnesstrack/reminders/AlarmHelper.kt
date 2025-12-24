package com.example.wellnesstrack.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmHelper {
    // Each reminder will use a requestCode derived from time string to allow cancel
    // request code can be based on type+time for uniqueness across reminder types
    private fun codeFor(time: String, type: String = "medicine"): Int {
        val key = "$type|$time"
        return key.replace(":", "").filter { it.isDigit() }.toIntOrNull() ?: key.hashCode()
    }

    fun scheduleExact(context: Context, time: String) {
        val parts = time.split(":").map { it.toIntOrNull() ?: 0 }
        val hour = parts.getOrNull(0) ?: 0
        val minute = parts.getOrNull(1) ?: 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appCtx, MedicineAlarmReceiver::class.java).apply {
            putExtra(MedicineAlarmReceiver.EXTRA_TIME, time)
            putExtra(MedicineAlarmReceiver.EXTRA_TYPE, "medicine")
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getBroadcast(appCtx, codeFor(time, "medicine"), intent, flags)
        // exact alarm
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } catch (e: Exception) {
            // fallback to set if setExactAndAllowWhileIdle not available
            try { am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi) } catch (_: Exception) { }
        }
    }

    fun cancel(context: Context, time: String) {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appCtx, MedicineAlarmReceiver::class.java)
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_NO_CREATE
        val pi = PendingIntent.getBroadcast(appCtx, codeFor(time, "medicine"), intent, flags)
        if (pi != null) am.cancel(pi)
    }

    // Schedule an alarm identifying a reminder type
    fun scheduleExactForType(context: Context, time: String, type: String) {
        val parts = time.split(":").map { it.toIntOrNull() ?: 0 }
        val hour = parts.getOrNull(0) ?: 0
        val minute = parts.getOrNull(1) ?: 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appCtx, MedicineAlarmReceiver::class.java).apply {
            putExtra(MedicineAlarmReceiver.EXTRA_TIME, time)
            putExtra(MedicineAlarmReceiver.EXTRA_TYPE, type)
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getBroadcast(appCtx, codeFor(time, type), intent, flags)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } catch (e: Exception) {
            try { am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi) } catch (_: Exception) { }
        }
    }

    fun cancelForType(context: Context, type: String) {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // For type-scoped cancel, scan possible times? simplest: cancel using last saved time from prefs
        try {
            val prefs = com.example.wellnesstrack.data.Prefs(appCtx)
            val time = when (type) {
                "habit" -> prefs.getHabitReminderTime()
                "sleep" -> prefs.getSleepReminderTime()
                else -> ""
            }
            val intent = Intent(appCtx, MedicineAlarmReceiver::class.java)
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_NO_CREATE
            val pi = PendingIntent.getBroadcast(appCtx, codeFor(time, type), intent, flags)
            if (pi != null) am.cancel(pi)
        } catch (e: Exception) {
            // ignore
        }
    }
}
