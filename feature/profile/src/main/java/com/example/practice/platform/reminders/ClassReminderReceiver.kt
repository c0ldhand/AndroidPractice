package com.example.practice.platform.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.practice.feature.profile.R
import com.example.practice.feature.profile.platform.reminders.CLASS_REMINDER_CHANNEL
import com.example.practice.feature.profile.platform.reminders.CLASS_REMINDER_NAME

// Keep the component name so alarms scheduled before modularization still resolve.
class ClassReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(CLASS_REMINDER_NAME) ?: return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CLASS_REMINDER_CHANNEL,
                    context.getString(R.string.class_reminder_channel),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            ?: return
        val contentIntent = PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_IMMUTABLE)
        val message = if (name.isBlank()) context.getString(R.string.class_reminder_body_without_name)
            else context.getString(R.string.class_reminder_body, name)
        val notification = NotificationCompat.Builder(context, CLASS_REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_class_time)
            .setContentTitle(context.getString(R.string.class_reminder_title))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(1, notification)
    }
}
