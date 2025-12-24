package com.example.wellnesstrack.reminders

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.wellnesstrack.data.Prefs

class MedicineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val time = intent.getStringExtra(EXTRA_TIME) ?: ""
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "medicine"

        val (channelId, title, body) = when (type) {
            "medicine" -> Triple("medicine_channel", "Medicine Reminder 💊", "Time to take your medicine: $time")
            "habit" -> Triple("habit_channel", "Habit Reminder", "Time for your habit: $time")
            "sleep" -> Triple("sleep_channel", "Sleep Reminder", "Time to prepare for sleep: $time")
            else -> Triple("medicine_channel", "Reminder", "It's time: $time")
        }

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
        }

        try {
            val prefs = Prefs(context)
            prefs.addNotification(Prefs.AppNotification(System.currentTimeMillis(), type, title, body))
        } catch (e: Exception) { }
    }

    companion object {
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_TYPE = "extra_type"
    }
}
