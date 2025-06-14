package com.example.bachelor_frontend.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.classes.RecurringExpenseDto
import com.example.bachelor_frontend.repository.ExpenseRepository
import com.example.bachelor_frontend.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.*

class RecurringExpenseViewModel : ViewModel() {
    private val recurringExpenseRepository = RecurringExpenseRepository()
    private val expenseRepository = ExpenseRepository()

    private val _recurringExpenses = MutableStateFlow<List<RecurringExpenseDto>>(emptyList())
    val recurringExpenses: StateFlow<List<RecurringExpenseDto>> = _recurringExpenses.asStateFlow()

    private val _dueToday = MutableStateFlow<List<RecurringExpenseDto>>(emptyList())
    val dueToday: StateFlow<List<RecurringExpenseDto>> = _dueToday.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    companion object {
        private const val TAG = "RecurringExpenseViewModel"
    }

    fun loadRecurringExpenses(userId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val expenses = recurringExpenseRepository.getRecurringExpensesByUser(userId)
                _recurringExpenses.value = expenses

                val due = recurringExpenseRepository.getRecurringExpensesDueToday(userId)
                _dueToday.value = due

                _error.value = null
                Log.d(TAG, "Loaded ${expenses.size} recurring expenses, ${due.size} due today")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recurring expenses", e)
                _error.value = "Failed to load recurring expenses: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun addRecurringExpense(expense: RecurringExpenseDto, userId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val expenseWithUser = expense.copy(
                    id = "",
                    userId = userId
                )

                val id = recurringExpenseRepository.addRecurringExpense(expenseWithUser)
                Log.d(TAG, "Added recurring expense with ID: $id")

                // Reload the list
                loadRecurringExpenses(userId)

                _error.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error adding recurring expense", e)
                _error.value = "Failed to add recurring expense: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateRecurringExpense(expense: RecurringExpenseDto, userId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true

                recurringExpenseRepository.updateRecurringExpense(expense)
                Log.d(TAG, "Updated recurring expense: ${expense.id}")

                // Reload the list
                loadRecurringExpenses(userId)

                _error.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error updating recurring expense", e)
                _error.value = "Failed to update recurring expense: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteRecurringExpense(expenseId: String, userId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true

                recurringExpenseRepository.deleteRecurringExpense(expenseId)
                Log.d(TAG, "Deleted recurring expense: $expenseId")

                // Reload the list
                loadRecurringExpenses(userId)

                _error.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting recurring expense", e)
                _error.value = "Failed to delete recurring expense: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleRecurringExpenseActive(expense: RecurringExpenseDto, userId: String) {
        viewModelScope.launch {
            try {
                val updatedExpense = expense.copy(isActive = !expense.isActive)
                updateRecurringExpense(updatedExpense, userId)

                Log.d(TAG, "Toggled active status for recurring expense: ${expense.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling active status", e)
                _error.value = "Failed to update status: ${e.message}"
            }
        }
    }

    /**
     * Process due recurring expenses - creates actual expense entries
     */
    fun processDueRecurringExpenses(userId: String, onExpenseCreated: ((ExpenseDto) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val dueExpenses = recurringExpenseRepository.getRecurringExpensesDueToday(userId)

                for (recurringExpense in dueExpenses) {
                    if (recurringExpense.automaticallyGenerate && recurringExpense.isActive) {
                        // Create the actual expense
                        val expense = ExpenseDto(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            amount = recurringExpense.amount,
                            category = recurringExpense.category,
                            description = "${recurringExpense.description} (Recurring)",
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
                        recurringExpenseRepository.markAsProcessed(recurringExpense.id, recurringExpense.nextPaymentDate)

                        // Notify callback
                        onExpenseCreated?.invoke(savedExpense)

                        Log.d(TAG, "Auto-generated expense from recurring: ${recurringExpense.description}")
                    }
                }

                // Reload after processing
                loadRecurringExpenses(userId)

            } catch (e: Exception) {
                Log.e(TAG, "Error processing due recurring expenses", e)
                _error.value = "Failed to process due expenses: ${e.message}"
            }
        }
    }

    /**
     * Manually generate an expense from a recurring expense
     */
    fun generateExpenseNow(recurringExpense: RecurringExpenseDto, userId: String, onExpenseCreated: (ExpenseDto) -> Unit) {
        viewModelScope.launch {
            try {
                val expense = ExpenseDto(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    amount = recurringExpense.amount,
                    category = recurringExpense.category,
                    description = recurringExpense.description,
                    timestamp = LocalDateTime.now(),
                    receiptImageUrl = null
                )

                val expenseId = expenseRepository.addExpense(expense)
                val savedExpense = expense.copy(id = expenseId)

                // Update next payment date
                val nextDate = recurringExpense.frequency.getNextDate(recurringExpense.nextPaymentDate)
                recurringExpenseRepository.updateNextPaymentDate(recurringExpense.id, nextDate)

                // Mark as processed
                recurringExpenseRepository.markAsProcessed(recurringExpense.id, recurringExpense.nextPaymentDate)

                // Reload
                loadRecurringExpenses(userId)

                onExpenseCreated(savedExpense)

                Log.d(TAG, "Manually generated expense from recurring: ${recurringExpense.description}")

            } catch (e: Exception) {
                Log.e(TAG, "Error generating expense", e)
                _error.value = "Failed to generate expense: ${e.message}"
            }
        }
    }

    /**
     * Get summary stats for recurring expenses
     */
    fun getRecurringExpenseSummary(): Map<String, Double> {
        val expenses = _recurringExpenses.value.filter { it.isActive }

        val monthlyTotal = expenses.filter { it.frequency.name == "MONTHLY" }.sumOf { it.amount }
        val weeklyTotal = expenses.filter { it.frequency.name == "WEEKLY" }.sumOf { it.amount }
        val yearlyTotal = expenses.filter { it.frequency.name == "YEARLY" }.sumOf { it.amount }
        val quarterlyTotal = expenses.filter { it.frequency.name == "QUARTERLY" }.sumOf { it.amount }
        val biweeklyTotal = expenses.filter { it.frequency.name == "BIWEEKLY" }.sumOf { it.amount }

        // Convert everything to monthly equivalent
        val monthlyEquivalent = monthlyTotal +
                (weeklyTotal * 4.33) + // Approximate weeks per month
                (biweeklyTotal * 2.17) + // Approximate bi-weeks per month
                (quarterlyTotal / 3) +
                (yearlyTotal / 12)

        return mapOf(
            "monthlyEquivalent" to monthlyEquivalent,
            "totalActive" to expenses.size.toDouble(),
            "dueThisWeek" to expenses.count {
                val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(),
                    it.nextPaymentDate
                )
                daysUntil in 0..7
            }.toDouble()
        )
    }

    fun clearError() {
        _error.value = null
    }

    fun testFirestoreConnection(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== STARTING FIRESTORE TEST ===")

                // Test 1: Try to read from Firestore
                Log.d(TAG, "Test 1: Reading existing data...")
                val existingExpenses = recurringExpenseRepository.getRecurringExpensesByUser(userId)
                Log.d(TAG, "Found ${existingExpenses.size} existing recurring expenses")

                // Test 2: Try to create a simple test expense
                Log.d(TAG, "Test 2: Creating test expense...")
                val testExpense = RecurringExpenseDto(
                    id = "",
                    userId = userId,
                    amount = 10.0,
                    category = "Test",
                    description = "Test Recurring Expense - ${System.currentTimeMillis()}",
                    frequency = com.example.bachelor_frontend.classes.RecurrenceFrequency.MONTHLY,
                    startDate = java.time.LocalDate.now(),
                    nextPaymentDate = java.time.LocalDate.now(),
                    isActive = true,
                    automaticallyGenerate = false,
                    createdAt = LocalDateTime.now()
                )

                Log.d(TAG, "Test expense data: $testExpense")

                val newId = recurringExpenseRepository.addRecurringExpense(testExpense)
                Log.d(TAG, "Successfully created test expense with ID: $newId")

                // Test 3: Try to read back the data
                Log.d(TAG, "Test 3: Reading back data...")
                val updatedExpenses = recurringExpenseRepository.getRecurringExpensesByUser(userId)
                Log.d(TAG, "Now found ${updatedExpenses.size} recurring expenses")

                // Test 4: Update UI state
                Log.d(TAG, "Test 4: Updating UI state...")
                _recurringExpenses.value = updatedExpenses

                Log.d(TAG, "=== FIRESTORE TEST COMPLETED SUCCESSFULLY ===")
                _error.value = "Test completed! Check logs for details."

            } catch (e: Exception) {
                Log.e(TAG, "=== FIRESTORE TEST FAILED ===", e)
                _error.value = "Test failed: ${e.message}"
            }
        }
    }
}