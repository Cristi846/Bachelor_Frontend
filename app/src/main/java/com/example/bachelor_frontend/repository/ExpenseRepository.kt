package com.example.bachelor_frontend.repository

import android.util.Log
import com.example.bachelor_frontend.classes.BudgetType
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
        // Get personal expenses
        val personalSnapshot = db.collection(expensesCollection)
            .whereEqualTo("userId", userId)
            .whereEqualTo("budgetType", "PERSONAL")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        // Get family expenses where user is involved
        val familySnapshot = db.collection(expensesCollection)
            .whereEqualTo("userId", userId)
            .whereEqualTo("budgetType", "FAMILY")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        val allDocuments = personalSnapshot.documents + familySnapshot.documents

        return allDocuments.mapNotNull { document ->
            try {
                val id = document.id
                val amount = document.getDouble("amount") ?: 0.0
                val category = document.getString("category") ?: "Other"
                val description = document.getString("description") ?: ""
                val timestamp = document.getTimestamp("timestamp")?.toDate()?.toInstant()
                    ?.atZone(ZoneId.systemDefault())
                    ?.toLocalDateTime() ?: LocalDateTime.now()
                val receiptImageUrl = document.getString("receiptImageUrl")

                // Handle new fields
                val budgetTypeString = document.getString("budgetType") ?: "PERSONAL"
                val budgetType = try {
                    BudgetType.valueOf(budgetTypeString)
                } catch (e: Exception) {
                    BudgetType.PERSONAL
                }
                val familyId = document.getString("familyId")

                ExpenseDto(
                    id = id,
                    userId = userId,
                    amount = amount,
                    category = category,
                    description = description,
                    timestamp = timestamp,
                    receiptImageUrl = receiptImageUrl,
                    budgetType = budgetType,
                    familyId = familyId
                )
            } catch (e: Exception) {
                Log.e("ExpenseRepository", "Error parsing expense: ${e.message}", e)
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    suspend fun deleteFamilyExpenses(familyId: String) {
        val snapshot = db.collection(expensesCollection)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("budgetType", BudgetType.FAMILY.name)
            .get()
            .await()

        // Delete all family expenses in batch
        val batch = db.batch()
        snapshot.documents.forEach { document ->
            batch.delete(document.reference)
        }
        batch.commit().await()
    }

    suspend fun getExpensesWithReceipts(userId: String): List<ExpenseDto> {
        val snapshot = db.collection(expensesCollection)
            .whereEqualTo("userId", userId)
            .whereNotEqualTo("receiptImageUrl", null)
            .orderBy("receiptImageUrl", Query.Direction.ASCENDING)
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

                // Handle new fields
                val budgetTypeString = document.getString("budgetType") ?: "PERSONAL"
                val budgetType = try {
                    BudgetType.valueOf(budgetTypeString)
                } catch (e: Exception) {
                    BudgetType.PERSONAL
                }
                val familyId = document.getString("familyId")

                ExpenseDto(
                    id = id,
                    userId = userId,
                    amount = amount,
                    category = category,
                    description = description,
                    timestamp = timestamp,
                    receiptImageUrl = receiptImageUrl,
                    budgetType = budgetType,
                    familyId = familyId
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

                // Handle new fields
                val budgetTypeString = document.getString("budgetType") ?: "PERSONAL"
                val budgetType = try {
                    BudgetType.valueOf(budgetTypeString)
                } catch (e: Exception) {
                    BudgetType.PERSONAL
                }
                val familyId = document.getString("familyId")

                ExpenseDto(
                    id = id,
                    userId = userId,
                    amount = amount,
                    category = category,
                    description = description,
                    timestamp = timestamp,
                    receiptImageUrl = receiptImageUrl,
                    budgetType = budgetType,
                    familyId = familyId
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

                // Handle new fields
                val budgetTypeString = document.getString("budgetType") ?: "PERSONAL"
                val budgetType = try {
                    BudgetType.valueOf(budgetTypeString)
                } catch (e: Exception) {
                    BudgetType.PERSONAL
                }
                val familyId = document.getString("familyId")

                ExpenseDto(
                    id = id,
                    userId = userId,
                    amount = amount,
                    category = category,
                    description = description,
                    timestamp = timestamp,
                    receiptImageUrl = receiptImageUrl,
                    budgetType = budgetType,
                    familyId = familyId
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun addExpense(expense: ExpenseDto): String {
        val expenseData = hashMapOf(
            "userId" to expense.userId,
            "amount" to expense.amount,
            "category" to expense.category,
            "description" to expense.description,
            "timestamp" to Date.from(expense.timestamp.atZone(ZoneId.systemDefault()).toInstant()),
            "receiptImageUrl" to expense.receiptImageUrl,
            "budgetType" to expense.budgetType.name, // Add this field
            "familyId" to expense.familyId // Add this field
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
            "receiptImageUrl" to expense.receiptImageUrl,
            "budgetType" to expense.budgetType.name, // Add this field
            "familyId" to expense.familyId // Add this field
        )

        db.collection(expensesCollection).document(expense.id).set(expenseData).await()
    }

    suspend fun deleteExpense(expenseId: String) {
        db.collection(expensesCollection).document(expenseId).delete().await()
    }

    // Add method to get family expenses
    suspend fun getFamilyExpenses(familyId: String): List<ExpenseDto> {
        try {
            // First, try the simple query without ordering to avoid index issues
            val snapshot = db.collection(expensesCollection)
                .whereEqualTo("familyId", familyId)
                .whereEqualTo("budgetType", "FAMILY")
                .get() // Remove orderBy to avoid index requirement
                .await()

            val expenses = snapshot.documents.mapNotNull { document ->
                try {
                    val id = document.id
                    val userId = document.getString("userId") ?: ""
                    val amount = document.getDouble("amount") ?: 0.0
                    val category = document.getString("category") ?: "Other"
                    val description = document.getString("description") ?: ""
                    val timestamp = document.getTimestamp("timestamp")?.toDate()?.toInstant()
                        ?.atZone(ZoneId.systemDefault())
                        ?.toLocalDateTime() ?: LocalDateTime.now()
                    val receiptImageUrl = document.getString("receiptImageUrl")
                    val familyId = document.getString("familyId")

                    ExpenseDto(
                        id = id,
                        userId = userId,
                        amount = amount,
                        category = category,
                        description = description,
                        timestamp = timestamp,
                        receiptImageUrl = receiptImageUrl,
                        budgetType = BudgetType.FAMILY,
                        familyId = familyId
                    )
                } catch (e: Exception) {
                    Log.e("ExpenseRepository", "Error parsing family expense: ${e.message}", e)
                    null
                }
            }

            // Sort in memory instead of in Firestore query
            return expenses.sortedByDescending { it.timestamp }

        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error getting family expenses", e)
            // If the query fails, return empty list for now
            return emptyList()
        }
    }

    suspend fun getFamilyExpensesAlternative(familyMemberIds: List<String>): List<ExpenseDto> {
        try {
            val allFamilyExpenses = mutableListOf<ExpenseDto>()

            // Query each family member's FAMILY budget expenses
            for (memberId in familyMemberIds) {
                try {
                    val memberExpenses = db.collection(expensesCollection)
                        .whereEqualTo("userId", memberId)
                        .whereEqualTo("budgetType", "FAMILY")
                        .get()
                        .await()

                    val expenses = memberExpenses.documents.mapNotNull { document ->
                        try {
                            val id = document.id
                            val amount = document.getDouble("amount") ?: 0.0
                            val category = document.getString("category") ?: "Other"
                            val description = document.getString("description") ?: ""
                            val timestamp = document.getTimestamp("timestamp")?.toDate()?.toInstant()
                                ?.atZone(ZoneId.systemDefault())
                                ?.toLocalDateTime() ?: LocalDateTime.now()
                            val receiptImageUrl = document.getString("receiptImageUrl")
                            val familyId = document.getString("familyId")

                            ExpenseDto(
                                id = id,
                                userId = memberId,
                                amount = amount,
                                category = category,
                                description = description,
                                timestamp = timestamp,
                                receiptImageUrl = receiptImageUrl,
                                budgetType = BudgetType.FAMILY,
                                familyId = familyId
                            )
                        } catch (e: Exception) {
                            Log.e("ExpenseRepository", "Error parsing member expense: ${e.message}", e)
                            null
                        }
                    }

                    allFamilyExpenses.addAll(expenses)
                } catch (e: Exception) {
                    Log.e("ExpenseRepository", "Error getting expenses for member $memberId", e)
                }
            }

            return allFamilyExpenses.sortedByDescending { it.timestamp }

        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error getting family expenses alternative", e)
            return emptyList()
        }
    }
}