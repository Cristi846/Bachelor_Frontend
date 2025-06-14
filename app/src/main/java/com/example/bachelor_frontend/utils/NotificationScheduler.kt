package com.example.bachelor_frontend.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

/**
 * Handles scheduling of daily reminder notifications
 */
class NotificationScheduler(private val context: Context) {

    companion object {
        private const val DAILY_REMINDER_REQUEST_CODE = 1001
        private const val TAG = "NotificationScheduler"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule daily reminder at specified time
     */
    fun scheduleDailyReminder(hour: Int, minute: Int) {
        try {
            val intent = Intent(context, DailyReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            // Schedule repeating alarm
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            Log.d(TAG, "Daily reminder scheduled for ${hour}:${minute}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling daily reminder", e)
        }
    }

    /**
     * Cancel daily reminder
     */
    fun cancelDailyReminder() {
        try {
            val intent = Intent(context, DailyReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Daily reminder cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling daily reminder", e)
        }
    }

    /**
     * Reschedule daily reminder with new time
     */
    fun rescheduleDailyReminder(hour: Int, minute: Int) {
        cancelDailyReminder()
        scheduleDailyReminder(hour, minute)
    }
}

/**
 * BroadcastReceiver for daily reminder notifications
 */
class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DailyReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Daily reminder received")

        try {
            val notificationPreferences = NotificationPreferences(context)
            val settings = notificationPreferences.getAllSettings()

            // Only show notification if notifications and daily reminders are enabled
            if (settings.notificationsEnabled && settings.dailyRemindersEnabled) {
                val notificationManager = NotificationManager(context)
                notificationManager.showDailyReminder()
                Log.d(TAG, "Daily reminder notification sent")
            } else {
                Log.d(TAG, "Daily reminder skipped - notifications disabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing daily reminder", e)
        }
    }
}