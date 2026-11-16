package com.example.practice.platform.reminders

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

internal const val CLASS_REMINDER_CHANNEL = "favorite_class_reminders"
internal const val CLASS_REMINDER_NAME = "profileName"

class ClassReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun notificationsEnabled(): Boolean = notifications.areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            notifications.getNotificationChannel(CLASS_REMINDER_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)

    fun schedule(triggerAtMillis: Long, name: String) {
        check(notificationsEnabled())
        if (!canScheduleExactAlarms()) throw SecurityException("Exact alarms are disabled")

        val intent = Intent(context, ClassReminderReceiver::class.java)
            .putExtra(CLASS_REMINDER_NAME, name)
        val reminder = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, reminder)
    }
}
