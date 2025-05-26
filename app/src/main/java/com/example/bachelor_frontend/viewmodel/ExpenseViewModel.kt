package com.example.bachelor_frontend.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bachelor_frontend.classes.ExpenseDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.util.*

class ExpenseViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // State flows for existing functionality
    private val _expenses = MutableStateFlow<List<ExpenseDto>>(emptyList())
    val expenses: StateFlow<List<ExpenseDto>> = _expenses.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _categorySpending = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categorySpending: StateFlow<Map<String, Double>> = _categorySpending.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(0.0)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _budgetSuggestions = MutableStateFlow<List<String>>(emptyList())
    val budgetSuggestions: StateFlow<List<String>> = _budgetSuggestions.asStateFlow()

    // New state flows for category budgets
    private val _categoryBudgets = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryBudgets: StateFlow<Map<String, Double>> = _categoryBudgets.asStateFlow()

    private val _categoryAnalysis = MutableStateFlow<Map<String, Map<String, Double>>>(emptyMap())
    val categoryAnalysis: StateFlow<Map<String, Map<String, Double>>> = _categoryAnalysis.asStateFlow()

    private val _overBudgetCategories = MutableStateFlow<List<String>>(emptyList())
    val overBudgetCategories: StateFlow<List<String>> = _overBudgetCategories.asStateFlow()

    private val _categoryBudgetSuggestions = MutableStateFlow<Map<String, String>>(emptyMap())
    val categoryBudgetSuggestions: StateFlow<Map<String, String>> = _categoryBudgetSuggestions.asStateFlow()

    // Track current selected month and year for analysis
    private val _selectedMonth = MutableStateFlow(LocalDateTime.now().month)
    val selectedMonth: StateFlow<Month> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(LocalDateTime.now().year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private var userViewModel: UserViewModel? = null

    init {
        // Initialize default values
        loadDefaultCategories()

        // If user is logged in, load their data
        auth.currentUser?.let { user ->
            loadUserData(user.uid)
            loadExpenses(user.uid)
        }
    }

    /**
     * Connect to the UserViewModel to sync data
     */
    fun connectToUserViewModel(viewModel: UserViewModel) {
        userViewModel = viewModel

        // Observe changes from UserViewModel
        viewModelScope.launch {
            viewModel.updateTrigger.collectLatest { trigger ->
                if (trigger > 0) {
                    Log.d("ExpenseViewModel", "Received update trigger: $trigger")
                    syncWithUserViewModel()
                }
            }
        }

        // Initial sync
        syncWithUserViewModel()
    }

    /**
     * Sync with UserViewModel to update shared data
     */
    private fun syncWithUserViewModel() {
        userViewModel?.let { viewModel ->
            viewModelScope.launch {
                _monthlyBudget.value = viewModel.monthlyBudget.value
                _categories.value = viewModel.categories.value
                _categoryBudgets.value = viewModel.categoryBudgets.value

                // Analyze spending vs budgets
                analyzeSpendingVsBudget()

                // Regenerate budget suggestions with new data
                generateBudgetSuggestions()
                generateCategoryBudgetSuggestions()

                Log.d("ExpenseViewModel", "Synced with UserViewModel: Budget=${_monthlyBudget.value}, Categories=${_categories.value}, CategoryBudgets=${_categoryBudgets.value}")
            }
        }
    }

    private fun loadDefaultCategories() {
        _categories.value = listOf(
            "Food",
            "Transportation",
            "Housing",
            "Entertainment",
            "Utilities",
            "Healthcare",
            "Shopping",
            "Other"
        )

        // Initialize default empty budgets
        _categoryBudgets.value = _categories.value.associateWith { 0.0 }
    }

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val userDocument = withContext(Dispatchers.IO) {
                    db.collection("users").document(userId).get().await()
                }

                if (userDocument.exists()) {
                    val budget = userDocument.getDouble("monthlyBudget") ?: 0.0
                    _monthlyBudget.value = budget

                    @Suppress("UNCHECKED_CAST")
                    val userCategories = userDocument.get("expenseCategories") as? List<String>
                    if (!userCategories.isNullOrEmpty()) {
                        _categories.value = userCategories
                    }

                    @Suppress("UNCHECKED_CAST")
                    val categoryBudgets = userDocument.get("categoryBudgets") as? Map<String, Double>
                    if (!categoryBudgets.isNullOrEmpty()) {
                        _categoryBudgets.value = categoryBudgets
                    } else {
                        // If category budgets don't exist, initialize with zeros
                        _categoryBudgets.value = _categories.value.associateWith { 0.0 }
                    }
                }

                _error.value = null
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "Error loading user data", e)
                _error.value = "Failed to load user data: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadExpenses(userId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val expensesSnapshot = withContext(Dispatchers.IO) {
                    db.collection("expenses")
                        .whereEqualTo("userId", userId)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .await()
                }

                val expensesList = expensesSnapshot.documents.mapNotNull { document ->
                    try {
                        val id = document.id
                        val amount = document.getDouble("amount") ?: 0.0
                        val category = document.getString("category") ?: "Other"
                        val description = document.getString("description") ?: ""
                        val timestampDate = document.getTimestamp("timestamp")?.toDate()
                        val timestamp = if (timestampDate != null) {
                            LocalDateTime.ofInstant(
                                timestampDate.toInstant(),
                                ZoneId.systemDefault()
                            )
                        } else {
                            LocalDateTime.now()
                        }
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
                        Log.e("ExpenseViewModel", "Error parsing expense document", e)
                        null
                    }
                }

                _expenses.value = expensesList

                // Update category spending analysis
                analyzeCategorySpending()

                // Analyze spending vs budget
                analyzeSpendingVsBudget()

                // Generate budget suggestions
                generateBudgetSuggestions()

                // Generate category budget suggestions
                generateCategoryBudgetSuggestions()

                _error.value = null
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "Error loading expenses", e)
                _error.value = "Failed to load expenses: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun addExpense(expense: ExpenseDto) {
        viewModelScope.launch {
            try {
                _loading.value = true

                // Prepare data for Firestore
                val expenseData = hashMapOf(
                    "userId" to expense.userId,
                    "amount" to expense.amount,
                    "category" to expense.category,
                    "description" to expense.description,
                    "timestamp" to Date.from(
                        expense.timestamp.atZone(ZoneId.systemDefault()).toInstant()
                    ),
                    "receiptImageUrl" to expense.receiptImageUrl
                )

                // Add to Firestore
                withContext(Dispatchers.IO) {
                    if (expense.id.isBlank()) {
                        // Create new expense
                        db.collection("expenses").add(expenseData).await()
                    } else {
                        // Update existing expense
                        db.collection("expenses").document(expense.id).set(expenseData).await()
                    }
                }

                // Reload expenses
                loadExpenses(expense.userId)

                _error.value = null
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "Error adding expense", e)
                _error.value = "Failed to save expense: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteExpense(expenseId: String, userId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true

                // Delete from Firestore
                withContext(Dispatchers.IO) {
                    db.collection("expenses").document(expenseId).delete().await()
                }

                // Reload expenses
                loadExpenses(userId)

                _error.value = null
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "Error deleting expense", e)
                _error.value = "Failed to delete expense: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateMonthlyBudget(userId: String, newBudget: Double) {
        viewModelScope.launch {
            try {
                _loading.value = true

                // Update in Firestore
                withContext(Dispatchers.IO) {
                    db.collection("users").document(userId)
                        .update("monthlyBudget", newBudget)
                        .await()
                }

                _monthlyBudget.value = newBudget

                // Regenerate budget suggestions with new budget
                generateBudgetSuggestions()

                _error.value = null
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "Error updating budget", e)
                _error.value = "Failed to update budget: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun setSelectedMonth(month: Month, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year

        // Re-analyze for the selected month
        analyzeCategorySpendingForSelectedPeriod()
        analyzeSpendingVsBudgetForSelectedPeriod()
    }

    private fun analyzeCategorySpending() {
        val expenseList = _expenses.value

        val spending = expenseList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toMap()

        _categorySpending.value = spending
    }

    private fun analyzeCategorySpendingForSelectedPeriod() {
        val month = _selectedMonth.value
        val year = _selectedYear.value
        val spending = getCategorySpendingForMonth(month, year)
        _categorySpending.value = spending
    }

    /**
     * Analyze category spending vs budget
     */
    private fun analyzeSpendingVsBudget() {
        val spending = _categorySpending.value
        val budgets = _categoryBudgets.value

        val analysis = mutableMapOf<String, Map<String, Double>>()
        val overBudget = mutableListOf<String>()

        for (category in budgets.keys) {
            val budget = budgets[category] ?: 0.0
            val spent = spending[category] ?: 0.0
            val remaining = budget - spent
            val percentUsed = if (budget > 0) (spent / budget) * 100 else 0.0

            val categoryAnalysis = mapOf(
                "budget" to budget,
                "spent" to spent,
                "remaining" to remaining,
                "percentUsed" to percentUsed
            )

            analysis[category] = categoryAnalysis

            // Track categories that are over budget
            if (spent > budget && budget > 0) {
                overBudget.add(category)
            }
        }

        _categoryAnalysis.value = analysis
        _overBudgetCategories.value = overBudget
    }

    private fun analyzeSpendingVsBudgetForSelectedPeriod() {
        val month = _selectedMonth.value
        val year = _selectedYear.value
        val spending = getCategorySpendingForMonth(month, year)
        val budgets = _categoryBudgets.value

        val analysis = mutableMapOf<String, Map<String, Double>>()
        val overBudget = mutableListOf<String>()

        for (category in budgets.keys) {
            val budget = budgets[category] ?: 0.0
            val spent = spending[category] ?: 0.0
            val remaining = budget - spent
            val percentUsed = if (budget > 0) (spent / budget) * 100 else 0.0

            val categoryAnalysis = mapOf(
                "budget" to budget,
                "spent" to spent,
                "remaining" to remaining,
                "percentUsed" to percentUsed
            )

            analysis[category] = categoryAnalysis

            // Track categories that are over budget
            if (spent > budget && budget > 0) {
                overBudget.add(category)
            }
        }

        _categoryAnalysis.value = analysis
        _overBudgetCategories.value = overBudget
    }

    private fun generateBudgetSuggestions() {
        val spending = _categorySpending.value
        val budget = _monthlyBudget.value

        val suggestions = mutableListOf<String>()
        val totalSpending = spending.values.sum()

        if (totalSpending > budget && budget > 0) {
            suggestions.add(
                "Your total spending of ${
                    String.format(
                        "%.2f",
                        totalSpending
                    )
                } exceeds your monthly budget of ${String.format("%.2f", budget)}"
            )

            // Find top 3 categories with highest spending
            spending.entries.sortedByDescending { it.value }
                .take(3)
                .forEach { entry ->
                    val category = entry.key
                    val amount = entry.value
                    val percentage = (amount / totalSpending) * 100

                    suggestions.add(
                        "You spent ${
                            String.format(
                                "%.2f",
                                amount
                            )
                        } (${
                            String.format(
                                "%.1f",
                                percentage
                            )
                        }%) on $category. Consider reducing this expense."
                    )
                }
        } else {
            suggestions.add("Your spending is within your monthly budget. Good job!")
        }

        _budgetSuggestions.value = suggestions
    }

    /**
     * Generate budget suggestions based on spending patterns
     */
    private fun generateCategoryBudgetSuggestions() {
        val spending = _categorySpending.value
        val budgets = _categoryBudgets.value
        val suggestions = mutableMapOf<String, String>()

        // Identify categories without budgets
        for (category in spending.keys) {
            val spent = spending[category] ?: 0.0
            val budget = budgets[category] ?: 0.0

            if (budget == 0.0 && spent > 0) {
                // Suggest setting a budget based on current spending
                suggestions[category] = "Consider setting a budget for $category based on your average spending of $${String.format("%.2f", spent)}"
            } else if (spent > budget && budget > 0) {
                // Category is over budget
                val overage = spent - budget
                val percentOver = (overage / budget) * 100

                suggestions[category] = "Your $category spending exceeds your budget by $${String.format("%.2f", overage)} " +
                        "(${String.format("%.1f", percentOver)}%). Consider increasing your budget or reducing expenses."
            } else if (spent < budget * 0.5 && budget > 0) {
                // Category is significantly under budget
                suggestions[category] = "You're using only ${String.format("%.1f", (spent / budget) * 100)}% " +
                        "of your $category budget. Consider reallocating some funds to other categories."
            }
        }

        _categoryBudgetSuggestions.value = suggestions
    }

    /**
     * Get spending for a specific month
     */
    fun getCategorySpendingForMonth(month: Month, year: Int): Map<String, Double> {
        val allExpenses = _expenses.value

        // Create YearMonth to compare
        val yearMonth = YearMonth.of(year, month)

        // Filter expenses for the given month and year
        val monthExpenses = allExpenses.filter {
            val expenseYearMonth = YearMonth.of(it.timestamp.year, it.timestamp.month)
            expenseYearMonth == yearMonth
        }

        // Group and sum by category
        return monthExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    /**
     * Get analysis for a specific month
     */
    fun getCategoryAnalysisForMonth(month: Month, year: Int): Map<String, Map<String, Double>> {
        val spending = getCategorySpendingForMonth(month, year)
        val budgets = _categoryBudgets.value
        val analysis = mutableMapOf<String, Map<String, Double>>()

        for (category in budgets.keys) {
            val budget = budgets[category] ?: 0.0
            val spent = spending[category] ?: 0.0
            val remaining = budget - spent
            val percentUsed = if (budget > 0) (spent / budget) * 100 else 0.0

            val categoryAnalysis = mapOf(
                "budget" to budget,
                "spent" to spent,
                "remaining" to remaining,
                "percentUsed" to percentUsed
            )

            analysis[category] = categoryAnalysis
        }

        return analysis
    }

    /**
     * Get over budget categories for a specific month
     */
    fun getOverBudgetCategoriesForMonth(month: Month, year: Int): List<String> {
        val analysis = getCategoryAnalysisForMonth(month, year)
        return analysis.filter { (_, details) ->
            val budget = details["budget"] ?: 0.0
            val spent = details["spent"] ?: 0.0
            spent > budget && budget > 0.0
        }.keys.toList()
    }

    /**
     * Update a specific category budget
     */
    fun updateCategoryBudget(userId: String, category: String, amount: Double) {
        viewModelScope.launch {
            try {
                _loading.value = true

                // Get updated budgets
                val updatedBudgets = _categoryBudgets.value.toMutableMap()
                updatedBudgets[category] = amount

                // Update in Firestore
                withContext(Dispatchers.IO) {
                    db.collection("users").document(userId)
                        .update("categoryBudgets", updatedBudgets)
                        .await()
                }

                // Update local state
                _categoryBudgets.value = updatedBudgets

                // Re-analyze with new budgets
                analyzeSpendingVsBudget()
                generateCategoryBudgetSuggestions()

                _error.value = null
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "Error updating category budget", e)
                _error.value = "Failed to update category budget: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun analyzeCategory(expenses: List<ExpenseDto>): Map<String, Any> {
        val categoryExpenses = expenses.groupBy { it.category }
        val analysis = mutableMapOf<String, Any>()

        categoryExpenses.forEach { (category, categoryExpenseList) ->
            val totalAmount = categoryExpenseList.sumOf { it.amount }
            val averageAmount = totalAmount / categoryExpenseList.size
            val transactionCount = categoryExpenseList.size
            val lastTransaction = categoryExpenseList.maxByOrNull { it.timestamp }

            // Monthly trends analysis
            val monthlySpending = categoryExpenseList
                .groupBy { YearMonth.of(it.timestamp.year, it.timestamp.month) }
                .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
                .toSortedMap()

            // Calculate trend (increasing/decreasing spending)
            val trend = if (monthlySpending.size >= 2) {
                val values = monthlySpending.values.toList()
                val recent = values.takeLast(3).average()
                val older = values.dropLast(3).takeIf { it.isNotEmpty() }?.average() ?: recent

                when {
                    recent > older * 1.1 -> "increasing"
                    recent < older * 0.9 -> "decreasing"
                    else -> "stable"
                }
            } else {
                "insufficient_data"
            }

            analysis[category] = mapOf(
                "totalAmount" to totalAmount,
                "averageAmount" to averageAmount,
                "transactionCount" to transactionCount,
                "lastTransaction" to lastTransaction?.timestamp,
                "monthlyTrend" to trend,
                "monthlySpending" to monthlySpending
            )
        }

        return analysis
    }

    /**
     * Generate seasonal spending patterns
     */
    private fun analyzeSeasonalPattern(expenses: List<ExpenseDto>): Map<String, Double> {
        val seasonalSpending = mutableMapOf(
            "Spring" to 0.0,
            "Summer" to 0.0,
            "Fall" to 0.0,
            "Winter" to 0.0
        )

        expenses.forEach { expense ->
            val season = when (expense.timestamp.month) {
                Month.MARCH, Month.APRIL, Month.MAY -> "Spring"
                Month.JUNE, Month.JULY, Month.AUGUST -> "Summer"
                Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> "Fall"
                Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> "Winter"
            }
            seasonalSpending[season] = seasonalSpending[season]!! + expense.amount
        }

        return seasonalSpending
    }

    /**
     * Analyze monthly spending trends
     */
    private fun analyzeMonthlyTrends(expenses: List<ExpenseDto>): Map<YearMonth, Double> {
        return expenses
            .groupBy { YearMonth.of(it.timestamp.year, it.timestamp.month) }
            .mapValues { (_, monthExpenses) -> monthExpenses.sumOf { it.amount } }
            .toSortedMap()
    }

    fun clearError() {
        _error.value = null
    }
}