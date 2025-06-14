package com.example.bachelor_frontend.repository

import android.util.Log
import com.example.bachelor_frontend.classes.RecurringExpenseDto
import com.example.bachelor_frontend.classes.RecurrenceFrequency
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class RecurringExpenseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val recurringExpensesCollection = "recurringExpenses"

    companion object {
        private const val TAG = "RecurringExpenseRepo"
    }

    suspend fun addRecurringExpense(expense: RecurringExpenseDto): String {
        return try {
            val expenseData = recurringExpenseDtoToMap(expense)
            val documentRef = db.collection(recurringExpensesCollection).add(expenseData).await()
            val expenseId = documentRef.id

            // Update the document with its own ID
            val updatedExpenseData = expenseData.toMutableMap()
            updatedExpenseData["id"] = expenseId
            db.collection(recurringExpensesCollection).document(expenseId).set(updatedExpenseData).await()

            Log.d(TAG, "Successfully added recurring expense with ID: $expenseId")
            expenseId
        } catch (e: Exception) {
            Log.e(TAG, "Error adding recurring expense", e)
            throw e
        }
    }

    suspend fun getRecurringExpensesByUser(userId: String): List<RecurringExpenseDto> {
        return try {
            // Simple query without ordering to avoid index requirements
            val snapshot = db.collection(recurringExpensesCollection)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val expenses = snapshot.documents.mapNotNull { document ->
                try {
                    mapToRecurringExpenseDto(document.data!!, document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping recurring expense document", e)
                    null
                }
            }

            // Sort in memory instead of in the query
            val sortedExpenses = expenses.sortedByDescending { it.createdAt }

            Log.d(TAG, "Retrieved ${expenses.size} recurring expenses for user: $userId")
            sortedExpenses
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recurring expenses", e)
            emptyList()
        }
    }

    suspend fun getActiveRecurringExpenses(userId: String): List<RecurringExpenseDto> {
        return try {
            // Get all user expenses first, then filter in memory
            val allExpenses = getRecurringExpensesByUser(userId)
            val activeExpenses = allExpenses.filter { it.isActive }
                .sortedBy { it.nextPaymentDate }

            Log.d(TAG, "Retrieved ${activeExpenses.size} active recurring expenses for user: $userId")
            activeExpenses
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active recurring expenses", e)
            emptyList()
        }
    }

    suspend fun getRecurringExpensesDueToday(userId: String): List<RecurringExpenseDto> {
        return try {
            // Get all active expenses for the user and filter in memory
            val allExpenses = getActiveRecurringExpenses(userId)
            val today = LocalDate.now()

            val dueToday = allExpenses.filter { expense ->
                expense.nextPaymentDate.isEqual(today) || expense.nextPaymentDate.isBefore(today)
            }

            Log.d(TAG, "Retrieved ${dueToday.size} recurring expenses due today for user: $userId")
            dueToday
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recurring expenses due today", e)
            emptyList()
        }
    }

    suspend fun updateRecurringExpense(expense: RecurringExpenseDto) {
        try {
            val expenseData = recurringExpenseDtoToMap(expense)
            db.collection(recurringExpensesCollection)
                .document(expense.id)
                .set(expenseData, SetOptions.merge())
                .await()

            Log.d(TAG, "Successfully updated recurring expense: ${expense.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating recurring expense", e)
            throw e
        }
    }

    suspend fun deleteRecurringExpense(expenseId: String) {
        try {
            db.collection(recurringExpensesCollection)
                .document(expenseId)
                .delete()
                .await()

            Log.d(TAG, "Successfully deleted recurring expense: $expenseId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting recurring expense", e)
            throw e
        }
    }

    suspend fun updateNextPaymentDate(expenseId: String, nextDate: LocalDate) {
        try {
            val nextDateTimestamp = Date.from(nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

            db.collection(recurringExpensesCollection)
                .document(expenseId)
                .update("nextPaymentDate", nextDateTimestamp)
                .await()

            Log.d(TAG, "Updated next payment date for expense $expenseId to $nextDate")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating next payment date", e)
            throw e
        }
    }

    suspend fun markAsProcessed(expenseId: String, processedDate: LocalDate) {
        try {
            val processedDateTimestamp = Date.from(processedDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

            db.collection(recurringExpensesCollection)
                .document(expenseId)
                .update("lastProcessedDate", processedDateTimestamp)
                .await()

            Log.d(TAG, "Marked recurring expense $expenseId as processed on $processedDate")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking expense as processed", e)
            throw e
        }
    }

    private fun recurringExpenseDtoToMap(expense: RecurringExpenseDto): Map<String, Any?> {
        return hashMapOf(
            "id" to expense.id,
            "userId" to expense.userId,
            "amount" to expense.amount,
            "category" to expense.category,
            "description" to expense.description,
            "frequency" to expense.frequency.name,
            "startDate" to Date.from(expense.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
            "endDate" to (expense.endDate?.let { Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant()) }),
            "nextPaymentDate" to Date.from(expense.nextPaymentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
            "isActive" to expense.isActive,
            "createdAt" to Date.from(expense.createdAt.atZone(ZoneId.systemDefault()).toInstant()),
            "lastProcessedDate" to (expense.lastProcessedDate?.let { Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant()) }),
            "automaticallyGenerate" to expense.automaticallyGenerate
        )
    }

    private fun mapToRecurringExpenseDto(data: Map<String, Any>, id: String): RecurringExpenseDto {
        return RecurringExpenseDto(
            id = id,
            userId = data["userId"] as? String ?: "",
            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
            category = data["category"] as? String ?: "Other",
            description = data["description"] as? String ?: "",
            frequency = try {
                RecurrenceFrequency.valueOf(data["frequency"] as? String ?: "MONTHLY")
            } catch (e: Exception) {
                RecurrenceFrequency.MONTHLY
            },
            startDate = (data["startDate"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now(),
            endDate = (data["endDate"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDate(),
            nextPaymentDate = (data["nextPaymentDate"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now(),
            isActive = data["isActive"] as? Boolean ?: true,
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDateTime() ?: LocalDateTime.now(),
            lastProcessedDate = (data["lastProcessedDate"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDate(),
            automaticallyGenerate = data["automaticallyGenerate"] as? Boolean ?: true
        )
    }
}