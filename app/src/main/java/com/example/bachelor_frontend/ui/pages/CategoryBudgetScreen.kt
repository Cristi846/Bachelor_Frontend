package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bachelor_frontend.ui.compose.BudgetDistributionDialog
import com.example.bachelor_frontend.ui.compose.CategoryBudgetEditDialog
import com.example.bachelor_frontend.ui.compose.CategoryBudgetItem
import com.example.bachelor_frontend.viewmodel.ExpenseViewModel
import com.example.bachelor_frontend.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBudgetScreen(
    userViewModel: UserViewModel,
    expenseViewModel: ExpenseViewModel,
    userId: String
) {
    val coroutineScope = rememberCoroutineScope()

    // State from ViewModels
    val categories by userViewModel.categories.collectAsState()
    val categoryBudgets by userViewModel.categoryBudgets.collectAsState()
    val categorySpending by expenseViewModel.categorySpending.collectAsState()
    val categoryAnalysis by expenseViewModel.categoryAnalysis.collectAsState()
    val overBudgetCategories by expenseViewModel.overBudgetCategories.collectAsState()
    val budgetSuggestions by expenseViewModel.categoryBudgetSuggestions.collectAsState()
    val currency by userViewModel.currency.collectAsState()
    val monthlyBudget by userViewModel.monthlyBudget.collectAsState()
    val loading by userViewModel.loading.collectAsState()

    // Local state
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showDistributionDialog by remember { mutableStateOf(false) }

    // Format for currency display
    val currencyFormat = remember(currency) {
        NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }
    }

    // Calculate total budget and spending
    val totalBudget = categoryBudgets.values.sum()
    val totalSpent = categorySpending.values.sum()
    val totalRemaining = totalBudget - totalSpent
    val isOverTotalBudget = totalSpent > totalBudget && totalBudget > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Category Budgets") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (monthlyBudget > 0) {
                FloatingActionButton(
                    onClick = { showDistributionDialog = true }
                ) {
                    Icon(Icons.Default.AutoGraph, contentDescription = "Distribute Budget")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Summary section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOverTotalBudget) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Budget Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (monthlyBudget > 0) {
                                    TextButton(
                                        onClick = { showDistributionDialog = true }
                                    ) {
                                        Icon(
                                            Icons.Default.AutoGraph,
                                            contentDescription = "Distribute Budget",
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text("Distribute Budget")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Monthly budget info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Monthly Budget",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = currencyFormat.format(monthlyBudget),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Allocated to Categories",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = currencyFormat.format(totalBudget),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (totalBudget > monthlyBudget) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Allocation progress bar
                            if (monthlyBudget > 0) {
                                val allocatedPercentage = (totalBudget / monthlyBudget * 100).coerceIn(0.0, 100.0)

                                LinearProgressIndicator(
                                    progress = (allocatedPercentage / 100).toFloat(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${String.format("%.1f", allocatedPercentage)}% of monthly budget allocated to categories",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                if (totalBudget < monthlyBudget) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "You have ${currencyFormat.format(monthlyBudget - totalBudget)} of your monthly budget that isn't allocated to any category.",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                } else if (totalBudget > monthlyBudget) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFEE8E8)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Your category budgets exceed your monthly budget by ${currencyFormat.format(totalBudget - monthlyBudget)}.",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 16.dp))

                            // Spending summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total Budget",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = currencyFormat.format(totalBudget),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Spent",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = currencyFormat.format(totalSpent),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (isOverTotalBudget) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isOverTotalBudget) "Over Budget:" else "Remaining:",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = if (isOverTotalBudget)
                                        "- ${currencyFormat.format(Math.abs(totalRemaining))}"
                                    else
                                        currencyFormat.format(totalRemaining),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverTotalBudget) Color.Red else MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (totalBudget > 0) {
                                val percentUsed = (totalSpent / totalBudget * 100).coerceIn(0.0, 100.0)
                                val progressColor = when {
                                    percentUsed < 70 -> MaterialTheme.colorScheme.primary
                                    percentUsed < 90 -> Color(0xFFFFA000) // Amber
                                    else -> Color(0xFFF44336) // Red
                                }

                                LinearProgressIndicator(
                                    progress = (percentUsed / 100).toFloat(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = progressColor
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${String.format("%.1f", percentUsed)}% of total budget used",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Budget alert
                            if (overBudgetCategories.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color.Red,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )

                                    Text(
                                        text = "${overBudgetCategories.size} ${if (overBudgetCategories.size == 1) "category is" else "categories are"} over budget",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Red
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Category Budgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Category budget items
                items(categories.sorted()) { category ->
                    val analysis = categoryAnalysis[category]
                    val budget = categoryBudgets[category] ?: 0.0
                    val spent = analysis?.get("spent") ?: 0.0

                    CategoryBudgetItem(
                        category = category,
                        budget = budget,
                        spent = spent,
                        currency = currency,
                        onEditClick = {
                            editingCategory = category
                        }
                    )
                }

                // Bottom space
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Show loading indicator
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Show snackbar
            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    }

    // Category budget edit dialog
    editingCategory?.let { category ->
        val currentBudget = categoryBudgets[category] ?: 0.0

        CategoryBudgetEditDialog(
            category = category,
            currentBudget = currentBudget,
            currency = currency,
            onBudgetChange = { newBudget ->
                coroutineScope.launch {
                    userViewModel.updateCategoryBudget(category, newBudget)
                    snackbarMessage = "Budget for $category updated to ${currencyFormat.format(newBudget)}"
                    showSnackbar = true
                }
            },
            onDismiss = {
                editingCategory = null
            }
        )
    }

    // Budget distribution dialog
    if (showDistributionDialog) {
        BudgetDistributionDialog(
            categories = categories,
            totalBudget = monthlyBudget,
            categorySpending = categorySpending,
            currency = currency,
            onDistribute = { newBudgets ->
                coroutineScope.launch {
                    // Update each category budget
                    for ((category, budget) in newBudgets) {
                        userViewModel.updateCategoryBudget(category, budget)
                    }

                    snackbarMessage = "Budget distributed across categories"
                    showSnackbar = true
                }
            },
            onDismiss = {
                showDistributionDialog = false
            }
        )
    }
}