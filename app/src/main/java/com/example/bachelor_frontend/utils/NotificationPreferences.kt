package com.example.bachelor_frontend.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "notification_preferences"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_BUDGET_ALERTS_ENABLED = "budget_alerts_enabled"
        private const val KEY_DAILY_REMINDERS_ENABLED = "daily_reminders_enabled"
        private const val KEY_EXPENSE_CONFIRMATIONS_ENABLED = "expense_confirmations_enabled"
        private const val KEY_BUDGET_ALERT_THRESHOLD = "budget_alert_threshold"
        private const val KEY_REMINDER_TIME_HOUR = "reminder_time_hour"
        private const val KEY_REMINDER_TIME_MINUTE = "reminder_time_minute"

        // Default values
        private const val DEFAULT_NOTIFICATIONS_ENABLED = true
        private const val DEFAULT_BUDGET_ALERTS_ENABLED = true
        private const val DEFAULT_DAILY_REMINDERS_ENABLED = false
        private const val DEFAULT_EXPENSE_CONFIRMATIONS_ENABLED = false
        private const val DEFAULT_BUDGET_ALERT_THRESHOLD = 80 // 80% of budget
        private const val DEFAULT_REMINDER_HOUR = 20 // 8 PM
        private const val DEFAULT_REMINDER_MINUTE = 0
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // State flows for reactive updates
    private val _notificationsEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, DEFAULT_NOTIFICATIONS_ENABLED)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _budgetAlertsEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_BUDGET_ALERTS_ENABLED, DEFAULT_BUDGET_ALERTS_ENABLED)
    )
    val budgetAlertsEnabled: StateFlow<Boolean> = _budgetAlertsEnabled.asStateFlow()

    private val _dailyRemindersEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_DAILY_REMINDERS_ENABLED, DEFAULT_DAILY_REMINDERS_ENABLED)
    )
    val dailyRemindersEnabled: StateFlow<Boolean> = _dailyRemindersEnabled.asStateFlow()

    private val _expenseConfirmationsEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_EXPENSE_CONFIRMATIONS_ENABLED, DEFAULT_EXPENSE_CONFIRMATIONS_ENABLED)
    )
    val expenseConfirmationsEnabled: StateFlow<Boolean> = _expenseConfirmationsEnabled.asStateFlow()

    private val _budgetAlertThreshold = MutableStateFlow(
        sharedPreferences.getInt(KEY_BUDGET_ALERT_THRESHOLD, DEFAULT_BUDGET_ALERT_THRESHOLD)
    )
    val budgetAlertThreshold: StateFlow<Int> = _budgetAlertThreshold.asStateFlow()

    private val _reminderTimeHour = MutableStateFlow(
        sharedPreferences.getInt(KEY_REMINDER_TIME_HOUR, DEFAULT_REMINDER_HOUR)
    )
    val reminderTimeHour: StateFlow<Int> = _reminderTimeHour.asStateFlow()

    private val _reminderTimeMinute = MutableStateFlow(
        sharedPreferences.getInt(KEY_REMINDER_TIME_MINUTE, DEFAULT_REMINDER_MINUTE)
    )
    val reminderTimeMinute: StateFlow<Int> = _reminderTimeMinute.asStateFlow()

    /**
     * Enable or disable all notifications
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
        _notificationsEnabled.value = enabled
    }

    /**
     * Enable or disable budget alerts
     */
    fun setBudgetAlertsEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_BUDGET_ALERTS_ENABLED, enabled)
            .apply()
        _budgetAlertsEnabled.value = enabled
    }

    /**
     * Enable or disable daily reminders
     */
    fun setDailyRemindersEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_DAILY_REMINDERS_ENABLED, enabled)
            .apply()
        _dailyRemindersEnabled.value = enabled
    }

    /**
     * Enable or disable expense confirmations
     */
    fun setExpenseConfirmationsEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_EXPENSE_CONFIRMATIONS_ENABLED, enabled)
            .apply()
        _expenseConfirmationsEnabled.value = enabled
    }

    /**
     * Set budget alert threshold (percentage)
     */
    fun setBudgetAlertThreshold(threshold: Int) {
        val validThreshold = threshold.coerceIn(50, 95) // Between 50% and 95%
        sharedPreferences.edit()
            .putInt(KEY_BUDGET_ALERT_THRESHOLD, validThreshold)
            .apply()
        _budgetAlertThreshold.value = validThreshold
    }

    /**
     * Set reminder time
     */
    fun setReminderTime(hour: Int, minute: Int) {
        val validHour = hour.coerceIn(0, 23)
        val validMinute = minute.coerceIn(0, 59)

        sharedPreferences.edit()
            .putInt(KEY_REMINDER_TIME_HOUR, validHour)
            .putInt(KEY_REMINDER_TIME_MINUTE, validMinute)
            .apply()

        _reminderTimeHour.value = validHour
        _reminderTimeMinute.value = validMinute
    }

    /**
     * Get all notification settings as a data class
     */
    fun getAllSettings(): NotificationSettings {
        return NotificationSettings(
            notificationsEnabled = _notificationsEnabled.value,
            budgetAlertsEnabled = _budgetAlertsEnabled.value,
            dailyRemindersEnabled = _dailyRemindersEnabled.value,
            expenseConfirmationsEnabled = _expenseConfirmationsEnabled.value,
            budgetAlertThreshold = _budgetAlertThreshold.value,
            reminderTimeHour = _reminderTimeHour.value,
            reminderTimeMinute = _reminderTimeMinute.value
        )
    }

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        sharedPreferences.edit().clear().apply()

        _notificationsEnabled.value = DEFAULT_NOTIFICATIONS_ENABLED
        _budgetAlertsEnabled.value = DEFAULT_BUDGET_ALERTS_ENABLED
        _dailyRemindersEnabled.value = DEFAULT_DAILY_REMINDERS_ENABLED
        _expenseConfirmationsEnabled.value = DEFAULT_EXPENSE_CONFIRMATIONS_ENABLED
        _budgetAlertThreshold.value = DEFAULT_BUDGET_ALERT_THRESHOLD
        _reminderTimeHour.value = DEFAULT_REMINDER_HOUR
        _reminderTimeMinute.value = DEFAULT_REMINDER_MINUTE
    }
}

/**
 * Data class to hold all notification settings
 */
data class NotificationSettings(
    val notificationsEnabled: Boolean,
    val budgetAlertsEnabled: Boolean,
    val dailyRemindersEnabled: Boolean,
    val expenseConfirmationsEnabled: Boolean,
    val budgetAlertThreshold: Int,
    val reminderTimeHour: Int,
    val reminderTimeMinute: Int
)