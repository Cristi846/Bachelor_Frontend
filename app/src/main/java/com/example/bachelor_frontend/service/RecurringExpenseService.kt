package com.example.bachelor_frontend.service

import android.content.Context
import android.util.Log
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.repository.ExpenseRepository
import com.example.bachelor_frontend.repository.RecurringExpenseRepository
import com.example.bachelor_frontend.utils.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Service to handle automatic processing of recurring expenses
 */
class RecurringExpenseService(private val context: Context) {

    companion object {
        private const val TAG = "RecurringExpenseService"
    }

    private val recurringExpenseRepository = RecurringExpenseRepository()
    private val expenseRepository = ExpenseRepository()
    private val notificationManager = NotificationManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Check and process due recurring expenses for a user
     */
    fun processDueRecurringExpenses(
        userId: String,
        onExpenseCreated: ((ExpenseDto) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val dueExpenses = recurringExpenseRepository.getRecurringExpensesDueToday(userId)
                var processedCount = 0

                for (recurringExpense in dueExpenses) {
                    if (recurringExpense.isActive) {
                        if (recurringExpense.automaticallyGenerate) {
                            // Auto-generate the expense
                            val expense = ExpenseDto(
                                id = UUID.randomUUID().toString(),
                                userId = userId,
                                amount = recurringExpense.amount,
                                category = recurringExpense.category,
                                description = "${recurringExpense.description} (Auto-generated)",
                                timestamp = LocalDateTime.now(),
                                receiptImageUrl = null
                            )

                            // Add to expenses
                            val expenseId = expenseRepository.addExpense(expense)
                            val savedExpense = expense.copy(id = expenseId)

                            // Update next payment date
                            val nextDate = recurringExpense.frequency.getNextDate(recurringExpense.nextPaymentDate)
                            recurringExpenseRepository.updateNextPaymentDate(recurringExpense.id, nextDate)

                            // Mark as processed
                            recurringExpenseRepository.markAsProcessed(
                                recurringExpense.id,
                                recurringExpense.nextPaymentDate
                            )

                            processedCount++
                            onExpenseCreated?.invoke(savedExpense)

                            Log.d(TAG, "Auto-generated expense: ${recurringExpense.description}")
                        } else {
                            // Just send a reminder notification
                            showRecurringExpenseReminder(recurringExpense.description, recurringExpense.amount)

                            // Update next payment date for reminder-only expenses too
                            val nextDate = recurringExpense.frequency.getNextDate(recurringExpense.nextPaymentDate)
                            recurringExpenseRepository.updateNextPaymentDate(recurringExpense.id, nextDate)
                        }
                    }
                }

                if (processedCount > 0) {
                    showProcessingSummaryNotification(processedCount)
                }

                Log.d(TAG, "Processed $processedCount recurring expenses for user: $userId")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing recurring expenses", e)
            }
        }
    }

    /**
     * Get upcoming recurring expenses (due in next 7 days)
     */
    suspend fun getUpcomingRecurringExpenses(userId: String): List<com.example.bachelor_frontend.classes.RecurringExpenseDto> {
        return try {
            val allExpenses = recurringExpenseRepository.getActiveRecurringExpenses(userId)
            val today = LocalDate.now()
            val nextWeek = today.plusDays(7)

            allExpenses.filter { expense ->
                expense.nextPaymentDate.isAfter(today) &&
                        expense.nextPaymentDate.isBefore(nextWeek.plusDays(1))
            }.sortedBy { it.nextPaymentDate }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting upcoming recurring expenses", e)
            emptyList()
        }
    }

    /**
     * Calculate total monthly recurring expense burden
     */
    suspend fun calculateMonthlyRecurringTotal(userId: String): Double {
        return try {
            val activeExpenses = recurringExpenseRepository.getActiveRecurringExpenses(userId)

            var monthlyTotal = 0.0

            for (expense in activeExpenses) {
                val monthlyEquivalent = when (expense.frequency) {
                    com.example.bachelor_frontend.classes.RecurrenceFrequency.WEEKLY -> expense.amount * 4.33
                    com.example.bachelor_frontend.classes.RecurrenceFrequency.BIWEEKLY -> expense.amount * 2.17
                    com.example.bachelor_frontend.classes.RecurrenceFrequency.MONTHLY -> expense.amount
                    com.example.bachelor_frontend.classes.RecurrenceFrequency.QUARTERLY -> expense.amount / 3
                    com.example.bachelor_frontend.classes.RecurrenceFrequency.YEARLY -> expense.amount / 12
                }
                monthlyTotal += monthlyEquivalent
            }

            monthlyTotal

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating monthly recurring total", e)
            0.0
        }
    }

    /**
     * Show notification for recurring expense reminder
     */
    private fun showRecurringExpenseReminder(description: String, amount: Double) {
        try {
            // You would implement this in your NotificationManager
            // For now, just log it
            Log.d(TAG, "Reminder: $description - $amount is due today")

            // Example notification (you'd need to implement this method in NotificationManager):
            // notificationManager.showRecurringExpenseReminder(description, amount)

        } catch (e: Exception) {
            Log.e(TAG, "Error showing reminder notification", e)
        }
    }

    /**
     * Show summary notification after processing multiple expenses
     */
    private fun showProcessingSummaryNotification(count: Int) {
        try {
            Log.d(TAG, "Processed $count recurring expenses today")

            // Example notification (you'd need to implement this method in NotificationManager):
            // notificationManager.showRecurringExpenseProcessingSummary(count)

        } catch (e: Exception) {
            Log.e(TAG, "Error showing processing summary notification", e)
        }
    }

    /**
     * Check if recurring expenses need attention (overdue, due soon, etc.)
     */
    suspend fun checkRecurringExpenseHealth(userId: String): Map<String, Int> {
        return try {
            val allExpenses = recurringExpenseRepository.getRecurringExpensesByUser(userId)
            val today = LocalDate.now()

            val overdue = allExpenses.count {
                it.isActive && it.nextPaymentDate.isBefore(today)
            }

            val dueToday = allExpenses.count {
                it.isActive && it.nextPaymentDate.isEqual(today)
            }

            val dueSoon = allExpenses.count {
                it.isActive && it.nextPaymentDate.isAfter(today) &&
                        it.nextPaymentDate.isBefore(today.plusDays(4))
            }

            val paused = allExpenses.count { !it.isActive }

            mapOf(
                "overdue" to overdue,
                "dueToday" to dueToday,
                "dueSoon" to dueSoon,
                "paused" to paused,
                "total" to allExpenses.size
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error checking recurring expense health", e)
            emptyMap()
        }
    }
}