package com.example.bachelor_frontend.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bachelor_frontend.MainActivity
import com.example.bachelor_frontend.R
import java.text.NumberFormat
import java.util.*

class NotificationManager(private val context: Context) {

    companion object {
        private const val BUDGET_ALERT_CHANNEL_ID = "budget_alerts"
        private const val DAILY_REMINDER_CHANNEL_ID = "daily_reminders"
        private const val GENERAL_CHANNEL_ID = "general_notifications"

        private const val BUDGET_ALERT_NOTIFICATION_ID = 1001
        private const val DAILY_REMINDER_NOTIFICATION_ID = 1002
        private const val OVER_BUDGET_NOTIFICATION_ID = 1003
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for different types of notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Budget Alerts Channel
            val budgetChannel = NotificationChannel(
                BUDGET_ALERT_CHANNEL_ID,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about budget limits and spending alerts"
                enableVibration(true)
                setShowBadge(true)
            }

            // Daily Reminders Channel
            val reminderChannel = NotificationChannel(
                DAILY_REMINDER_CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders to track expenses"
                enableVibration(false)
                setShowBadge(false)
            }

            // General Channel
            val generalChannel = NotificationChannel(
                GENERAL_CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(budgetChannel)
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(generalChannel)
        }
    }

    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required for older versions
        }
    }

    /**
     * Show budget warning notification when approaching limit
     */
    @SuppressLint("MissingPermission")
    fun showBudgetWarning(
        currentSpending: Double,
        budgetLimit: Double,
        currency: String,
        category: String? = null
    ) {
        if (!hasNotificationPermission() || !areNotificationsEnabled()) return

        val percentage = (currentSpending / budgetLimit * 100).toInt()
        val currencyFormat = NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }

        val title = if (category != null) {
            "Budget Alert: $category"
        } else {
            "Monthly Budget Alert"
        }

        val message = "You've used $percentage% of your budget (${currencyFormat.format(currentSpending)} of ${currencyFormat.format(budgetLimit)})"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BUDGET_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(context.getColor(android.R.color.holo_orange_light))
            .build()

        notificationManager.notify(BUDGET_ALERT_NOTIFICATION_ID, notification)
    }

    /**
     * Show over budget notification
     */
    @SuppressLint("MissingPermission")
    fun showOverBudgetAlert(
        currentSpending: Double,
        budgetLimit: Double,
        currency: String,
        category: String? = null
    ) {
        if (!hasNotificationPermission() || !areNotificationsEnabled()) return

        val overage = currentSpending - budgetLimit
        val currencyFormat = NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }

        val title = if (category != null) {
            "Over Budget: $category"
        } else {
            "Monthly Budget Exceeded"
        }

        val message = "You're ${currencyFormat.format(overage)} over budget! Current spending: ${currencyFormat.format(currentSpending)}"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BUDGET_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(context.getColor(android.R.color.holo_red_light))
            .addAction(
                R.drawable.ic_analytics,
                "View Analytics",
                pendingIntent
            )
            .build()

        notificationManager.notify(OVER_BUDGET_NOTIFICATION_ID, notification)
    }

    /**
     * Show daily reminder to track expenses
     */
    @SuppressLint("MissingPermission")
    fun showDailyReminder() {
        if (!hasNotificationPermission() || !areNotificationsEnabled()) return

        val messages = listOf(
            "Don't forget to track your expenses today!",
            "How much did you spend today? Add your expenses now.",
            "Keep your budget on track - log today's purchases.",
            "Time to update your expense tracker!",
            "Stay financially organized - record today's spending."
        )

        val randomMessage = messages.random()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DAILY_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Finance Tracker Reminder")
            .setContentText(randomMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_add,
                "Add Expense",
                pendingIntent
            )
            .build()

        notificationManager.notify(DAILY_REMINDER_NOTIFICATION_ID, notification)
    }

    /**
     * Show expense added confirmation
     */
    @SuppressLint("MissingPermission")
    fun showExpenseAddedConfirmation(
        amount: Double,
        category: String,
        currency: String
    ) {
        if (!hasNotificationPermission() || !areNotificationsEnabled()) return

        val currencyFormat = NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }

        val message = "Added ${currencyFormat.format(amount)} expense in $category"

        val notification = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Expense Added")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(3000) // Auto dismiss after 3 seconds
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Cancel specific notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}