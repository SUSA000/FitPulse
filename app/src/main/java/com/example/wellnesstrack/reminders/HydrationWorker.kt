package com.example.wellnesstrack.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.wellnesstrack.data.Prefs
import java.util.concurrent.TimeUnit

class HydrationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        try {
            val prefs = Prefs(applicationContext)
            val intervalMinutes = prefs.getHydrationReminderInterval()
            val intervalMillis = intervalMinutes * 60 * 1000L
            val last = prefs.getLong("last_hydration_time", 0L)
            val now = System.currentTimeMillis()
            // If user drank within the last interval, skip notification
            if (now - last < intervalMillis) {
                return Result.success()
            }
            showNotification()
        } catch (e: Exception) {
            // fallback to showing notification if prefs fail
            showNotification()
        }
        return Result.success()
    }

    private fun showNotification() {
        val channelId = "hydration_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Hydration", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle("Hydration reminder")
            .setContentText("Time to drink some water!")
            .setAutoCancel(true)
            .build()
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(1001, notif)
        }

        // record notification in prefs for header UI
        try {
            val prefs = Prefs(applicationContext)
            prefs.addNotification(Prefs.AppNotification(System.currentTimeMillis(), "hydration", "Hydration reminder", "Time to drink some water!"))
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        private const val WORK_TAG = "hydration_work"

        fun reschedule(context: Context) {
            val prefs = Prefs(context)
            // Always cancel previous work to ensure only one worker is scheduled.
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)

            if (!prefs.isHydrationReminderEnabled()) {
                return // Reminder is disabled, so we're done.
            }

            val interval = prefs.getHydrationReminderInterval().toLong()

            val workRequest = PeriodicWorkRequestBuilder<HydrationWorker>(interval, TimeUnit.MINUTES)
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }
    }
}
