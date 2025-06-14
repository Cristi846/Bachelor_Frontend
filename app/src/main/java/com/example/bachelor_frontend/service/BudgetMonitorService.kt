package com.example.bachelor_frontend.service

import android.content.Context
import android.util.Log
import com.example.bachelor_frontend.utils.NotificationManager
import com.example.bachelor_frontend.utils.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Service to monitor budget spending and trigger notifications
 */
class BudgetMonitorService(private val context: Context) {

    companion object {
        private const val TAG = "BudgetMonitorService"
    }

    private val notificationManager = NotificationManager(context)
    private val notificationPreferences = NotificationPreferences(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Check if user has exceeded budget threshold and send notification
     */
    fun checkBudgetAndNotify(
        currentSpending: Double,
        monthlyBudget: Double,
        currency: String,
        category: String? = null
    ) {
        scope.launch {
            try {
                val settings = notificationPreferences.getAllSettings()

                // Only proceed if notifications and budget alerts are enabled
                if (!settings.notificationsEnabled || !settings.budgetAlertsEnabled) {
                    return@launch
                }

                if (monthlyBudget <= 0) return@launch

                val spendingPercentage = (currentSpending / monthlyBudget) * 100
                val threshold = settings.budgetAlertThreshold.toDouble()

                Log.d(TAG, "Budget check: ${spendingPercentage.toInt()}% of budget used (threshold: ${threshold.toInt()}%)")

                when {
                    currentSpending > monthlyBudget -> {
                        // Over budget
                        notificationManager.showOverBudgetAlert(
                            currentSpending = currentSpending,
                            budgetLimit = monthlyBudget,
                            currency = currency,
                            category = category
                        )
                        Log.d(TAG, "Over budget notification sent")
                    }

                    spendingPercentage >= threshold -> {
                        // Approaching budget limit
                        notificationManager.showBudgetWarning(
                            currentSpending = currentSpending,
                            budgetLimit = monthlyBudget,
                            currency = currency,
                            category = category
                        )
                        Log.d(TAG, "Budget warning notification sent")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking budget and sending notification", e)
            }
        }
    }

    /**
     * Check category budget specifically
     */
    fun checkCategoryBudgetAndNotify(
        categorySpending: Map<String, Double>,
        categoryBudgets: Map<String, Double>,
        currency: String
    ) {
        scope.launch {
            try {
                val settings = notificationPreferences.getAllSettings()

                if (!settings.notificationsEnabled || !settings.budgetAlertsEnabled) {
                    return@launch
                }

                categoryBudgets.forEach { (category, budget) ->
                    val spending = categorySpending[category] ?: 0.0

                    if (budget > 0) {
                        checkBudgetAndNotify(
                            currentSpending = spending,
                            monthlyBudget = budget,
                            currency = currency,
                            category = category
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking category budgets", e)
            }
        }
    }

    /**
     * Monitor overall monthly budget
     */
    fun checkMonthlyBudgetAndNotify(
        totalSpending: Double,
        monthlyBudget: Double,
        currency: String
    ) {
        checkBudgetAndNotify(
            currentSpending = totalSpending,
            monthlyBudget = monthlyBudget,
            currency = currency,
            category = null
        )
    }

    /**
     * Show expense confirmation notification if enabled
     */
    fun showExpenseConfirmation(
        amount: Double,
        category: String,
        currency: String
    ) {
        scope.launch {
            try {
                val settings = notificationPreferences.getAllSettings()

                if (settings.notificationsEnabled && settings.expenseConfirmationsEnabled) {
                    notificationManager.showExpenseAddedConfirmation(
                        amount = amount,
                        category = category,
                        currency = currency
                    )
                    Log.d(TAG, "Expense confirmation notification sent")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing expense confirmation", e)
            }
        }
    }
}