package com.bakersoftware.plannextday.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bakersoftware.plannextday.MainActivity
import com.bakersoftware.plannextday.data.AppDatabase
import com.bakersoftware.plannextday.data.AppSettings
import com.bakersoftware.plannextday.data.SettingsRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class ReminderScheduler(private val context: Context) {
    suspend fun scheduleNext(settings: AppSettings) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.remindersEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val now = LocalDateTime.now()
        val reminderTime = now.toLocalDate().atStartOfDay()
            .plusMinutes(settings.reminderMinutes.toLong())
        val scheduledAt = if (now.isBefore(reminderTime)) reminderTime else reminderTime.plusDays(1)
        val delay = Duration.between(now, scheduledAt).toMillis().coerceAtLeast(0)
        val inputData = Data.Builder()
            .putString(DATE_KEY, scheduledAt.toLocalDate().toString())
            .build()
        val request = OneTimeWorkRequestBuilder<TomorrowReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "tomorrow-plan-reminder"
        const val DATE_KEY = "reminder_date"
        const val CHANNEL_ID = "planning-reminders"
    }
}

class TomorrowReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext)
        val settings = settingsRepository.settings.first()
        val scheduledDate = inputData.getString(ReminderScheduler.DATE_KEY)
            ?: return Result.success()
        val today = LocalDate.now()

        if (
            settings.remindersEnabled &&
            scheduledDate == today.toString() &&
            settings.lastReminderDate != scheduledDate
        ) {
            val tomorrow = today.plusDays(1).toString()
            val taskCount = AppDatabase.get(applicationContext).taskDao().countForDate(tomorrow)
            if (taskCount == 0 && NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                createChannel(applicationContext)
                showNotification(applicationContext)
                settingsRepository.markReminderSent(scheduledDate)
            }
        }

        ReminderScheduler(applicationContext).scheduleNext(settingsRepository.settings.first())
        return Result.success()
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            "Planning reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "A single reminder to plan tomorrow"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.TOMORROW_DESTINATION)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Plan tomorrow")
            .setContentText("A small plan tonight makes tomorrow easier.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(10, notification)
    }
}
