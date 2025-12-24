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
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.wellnesstrack.data.Prefs
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MedicineWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        showNotification()
        // schedule next day's occurrence using input data if present
        try {
            val hour = inputData.getInt(INPUT_HOUR, -1)
            val minute = inputData.getInt(INPUT_MINUTE, -1)
            if (hour >= 0 && minute >= 0) {
                scheduleForNext(applicationContext, hour, minute)
            }
        } catch (e: Exception) { }
        return Result.success()
    }

    private fun showNotification() {
        val channelId = "medicine_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Medicine", NotificationManager.IMPORTANCE_HIGH))
        }
        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Medicine Reminder 💊")
            .setContentText("Time to take your medicine")
            .setAutoCancel(true)
            .build()
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify((System.currentTimeMillis()%Int.MAX_VALUE).toInt(), notif)
        }

        // record
        try {
            val prefs = Prefs(applicationContext)
            prefs.addNotification(Prefs.AppNotification(System.currentTimeMillis(), "medicine", "Medicine Reminder", "Time to take your medicine"))
        } catch (e: Exception) { }
    }

    companion object {
        private const val TAG = "medicine_tag"
        private const val INPUT_HOUR = "input_hour"
        private const val INPUT_MINUTE = "input_minute"
        fun scheduleForNext(context: Context, hour: Int, minute: Int) {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = next.timeInMillis - now.timeInMillis
            val data = Data.Builder().putInt(INPUT_HOUR, hour).putInt(INPUT_MINUTE, minute).build()
            val work = OneTimeWorkRequestBuilder<MedicineWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(TAG)
                .build() as androidx.work.OneTimeWorkRequest
            WorkManager.getInstance(context).enqueue(work)
        }

        fun rescheduleAll(context: Context) {
            val prefs = Prefs(context)
            if (!prefs.isMedicineRemindersEnabled()) return
            val list = prefs.getMedicineReminders()
            list.forEach { time ->
                val parts = time.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                scheduleForNext(context, h, m)
            }
        }
    }
}
