package com.example.bachelor_frontend.repository

import com.example.bachelor_frontend.classes.ExpenseDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class ExpenseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val expensesCollection = "expenses"

    suspend fun getExpensesByUser(userId: String): List<ExpenseDto> {
        val snapshot = db.collection(expensesCollection)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            try {
                val id = document.id
                val amount = document.getDouble("amount") ?: 0.0
                val category = document.getString("category") ?: "Other"
                val description = document.getString("description") ?: ""
                val timestamp = document.getTimestamp("timestamp")?.toDate()?.toInstant()
                    ?.atZone(ZoneId.systemDefault())
                    ?.toLocalDateTime() ?: LocalDateTime.now()
                val receiptImageUrl = document.getString("receiptImageUrl")

                ExpenseDto(
                    id = id,
                    userId = userId,
                    amount = amount,
                    category = category,
                    description = description,
                    timestamp = timestamp,
                    receiptImageUrl = receiptImageUrl
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getExpensesWithReceipts(userId: String): List<ExpenseDto> {
        val snapshot = db.collection(expensesCollection)
            .whereEqualTo("userId", userId)
            .whereNotEqualTo("receiptImageUrl", null)
            .orderBy("receiptImageUrl", Query.Direction.ASCENDING)  // Required for inequality filter
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            try {
                val id = document.id
                val amount = document.getDouble("amount") ?: 0.0
                val category = document.getString("category") ?: "Other"
                val description = document.getString("description") ?: ""
                val timestamp = document.getTimestamp("timestamp")?.toDate()?.toInstant()
                    ?.atZone(ZoneId.systemDefault())
                    ?.toLocalDateTime() ?: LocalDateTime.now()
                val receiptImageUrl = document.getString("receiptImageUrl")

                ExpenseDto(
                    id = id,
                    userId = userId,
                    amount = amount,
                    category = category,
                    description = description,
                    timestamp = timestamp,
                    receiptImageUrl = receiptImageUrl
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getExpensesByCategory(userId: String, category: String): List<ExpenseDto> {
        val snapshot = db.collection(expensesCollection)
            .whereEqualTo("userId", userId)
            .whereEqualTo("category", category)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        // Same mapping logic as above
        return snapshot.documents.mapNotNull { document ->
            try {
                val id = document.id
                val amount = document.getDouble("amount") ?: 0.0
                val category = document.getString("category") ?: "Other"
                val description = document.getString("description") ?: ""
                val timestamp = document.getTimestamp("timestamp")?.toDate()?.toInstant()
                    ?.atZone(ZoneId.systemDefault())
                    ?.toLocalDateTime() ?: LocalDateTime.now()
                val receiptImageUrl = document.getString("receiptImageUrl")

                ExpenseDto(
                    id = id,
                    userId = userId,
                    amount = amount,
                    category = category,
                    description = description,
                    timestamp = timestamp,
                    receiptImageUrl = receiptImageUrl
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getExpensesByDateRange(
        userId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<ExpenseDto> {
        val startTimestamp = Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant())
        val endTimestamp = Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant())

        val snapshot = db.collection(expensesCollection)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .whereLessThanOrEqualTo("timestamp", endTimestamp)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        // Same mapping logic as above
        return snapshot.documents.mapNotNull { document ->
            try {
                val id = document.id
                val amount = document.getDouble("amount") ?: 0.0
                val category = document.getString("category") ?: "Other"
                val description = document.getString("description") ?: ""
                val timestamp = document.getTimestamp("timestamp")?.toDate()?.toInstant()
                    ?.atZone(ZoneId.systemDefault())
                    ?.toLocalDateTime() ?: LocalDateTime.now()
                val receiptImageUrl = document.getString("receiptImageUrl")

                ExpenseDto(
                    id = id,
                    userId = userId,
                    amount = amount,
                    category = category,
                    description = description,
                    timestamp = timestamp,
                    receiptImageUrl = receiptImageUrl
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    // Additional methods for CRUD operations
    suspend fun addExpense(expense: ExpenseDto): String {
        val expenseData = hashMapOf(
            "userId" to expense.userId,
            "amount" to expense.amount,
            "category" to expense.category,
            "description" to expense.description,
            "timestamp" to Date.from(expense.timestamp.atZone(ZoneId.systemDefault()).toInstant()),
            "receiptImageUrl" to expense.receiptImageUrl
        )

        val documentRef = db.collection(expensesCollection).add(expenseData).await()
        return documentRef.id
    }

    suspend fun updateExpense(expense: ExpenseDto) {
        val expenseData = hashMapOf(
            "userId" to expense.userId,
            "amount" to expense.amount,
            "category" to expense.category,
            "description" to expense.description,
            "timestamp" to Date.from(expense.timestamp.atZone(ZoneId.systemDefault()).toInstant()),
            "receiptImageUrl" to expense.receiptImageUrl
        )

        db.collection(expensesCollection).document(expense.id).set(expenseData).await()
    }

    suspend fun deleteExpense(expenseId: String) {
        db.collection(expensesCollection).document(expenseId).delete().await()
    }


}