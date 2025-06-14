package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bachelor_frontend.classes.BudgetType
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.viewmodel.ExpenseViewModel
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import com.example.bachelor_frontend.viewmodel.UserViewModel
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.Month
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    expenses: List<ExpenseDto>,
    monthlyBudget: Double,
    expenseViewModel: ExpenseViewModel,
    userViewModel: UserViewModel,
    familyViewModel: FamilyViewModel = viewModel(),
    onNavigateToCategoryBudgets: () -> Unit
) {
    val categorySpending by expenseViewModel.categorySpending.collectAsState()
    val categoryBudgets by userViewModel.categoryBudgets.collectAsState()
    val categoryAnalysis by expenseViewModel.categoryAnalysis.collectAsState()
    val overBudgetCategories by expenseViewModel.overBudgetCategories.collectAsState()
    val currency by userViewModel.currency.collectAsState()

    var selectedMonth by remember { mutableStateOf(LocalDateTime.now().month) }
    val selectedYear by remember { mutableStateOf(LocalDateTime.now().year) }

    val userFamily by familyViewModel.family.collectAsState()
    val familyAnalytics by familyViewModel.familyAnalytics.collectAsState()
    val familyExpenses by familyViewModel.familyExpenses.collectAsState()
    var showingFamilyAnalytics by remember { mutableStateOf(false) }

    val relevantExpenses = if (showingFamilyAnalytics) {
        // Use family expenses when showing family analytics
        familyExpenses.filter { expense ->
            val expenseMonth = expense.timestamp.month
            val expenseYear = expense.timestamp.year
            expenseMonth == selectedMonth && expenseYear == selectedYear
        }
    } else {
        // Use personal expenses
        expenses.filter { expense ->
            val expenseMonth = expense.timestamp.month
            val expenseYear = expense.timestamp.year
            val monthMatches = expenseMonth == selectedMonth && expenseYear == selectedYear
            val budgetMatches = expense.budgetType == BudgetType.PERSONAL
            monthMatches && budgetMatches
        }
    }

    val filteredCategorySpending = relevantExpenses.groupBy { it.category }
        .mapValues { (_, expenses) -> expenses.sumOf { it.amount }}

    LaunchedEffect(selectedMonth, selectedYear) {
        expenseViewModel.setSelectedMonth(selectedMonth, selectedYear)
    }

    // Use family currency when showing family analytics
    val displayCurrency = if (showingFamilyAnalytics && userFamily != null) {
        userFamily!!.sharedBudget.currency
    } else {
        currency
    }

    val currencyFormat = remember(displayCurrency) {
        NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(displayCurrency)
        }
    }

    val totalSpending = relevantExpenses.sumOf { it.amount }

    val budgetToUse = if (showingFamilyAnalytics && userFamily != null) {
        userFamily!!.sharedBudget.monthlyBudget
    } else {
        monthlyBudget
    }

    val categoryBudgetsToUse = if (showingFamilyAnalytics && userFamily != null) {
        userFamily!!.sharedBudget.categoryBudgets
    } else {
        categoryBudgets
    }

    val overBudgetCategoriesFiltered = filteredCategorySpending.filter { (category, spent) ->
        val budget = categoryBudgetsToUse[category] ?: 0.0
        spent > budget && budget > 0.0
    }.keys.toList()

    val colors = remember {
        listOf(
            Color(0xFF4CAF50),  // Green
            Color(0xFF2196F3),  // Blue
            Color(0xFFFFC107),  // Amber
            Color(0xFFE91E63),  // Pink
            Color(0xFF9C27B0),  // Purple
            Color(0xFFFF5722),  // Deep Orange
            Color(0xFF795548),  // Brown
            Color(0xFF607D8B)   // Blue Grey
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Analytics") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                MonthSelector(
                    selectedMonth = selectedMonth,
                    onMonthSelected = { selectedMonth = it }
                )
                if (userFamily != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !showingFamilyAnalytics,
                            onClick = { showingFamilyAnalytics = false },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Personal Analytics")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = showingFamilyAnalytics,
                            onClick = { showingFamilyAnalytics = true },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${userFamily!!.name} Analytics")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Spent in ${selectedMonth.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currencyFormat.format(totalSpending),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (totalSpending > budgetToUse && budgetToUse > 0)
                                Color(0xFFF44336) else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (budgetToUse > 0) {
                            val progress = (totalSpending / budgetToUse).coerceIn(0.0, 1.0).toFloat()
                            val progressColor = when {
                                progress < 0.7 -> MaterialTheme.colorScheme.primary
                                progress < 0.9 -> Color(0xFFFFA000) // Amber
                                else -> Color(0xFFF44336) // Red
                            }

                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = progressColor
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${(progress * 100).toInt()}% of ${if (showingFamilyAnalytics) "family" else "monthly"} budget used",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                                text = "Category Budgets",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Button(
                                onClick = onNavigateToCategoryBudgets,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("Manage Budgets")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (overBudgetCategoriesFiltered.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0xFFFEE8E8),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color.Red,
                                    modifier = Modifier.padding(end = 8.dp)
                                )

                                Text(
                                    text = "${overBudgetCategoriesFiltered.size} ${if (overBudgetCategoriesFiltered.size == 1) "category is" else "categories are"} over budget",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Red
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (overBudgetCategoriesFiltered.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                overBudgetCategoriesFiltered.forEach { category ->
                                    val budget = categoryBudgetsToUse[category] ?: 0.0
                                    val spent = filteredCategorySpending[category] ?: 0.0
                                    val overAmount = spent - budget
                                    val percentOver = if (budget > 0) (overAmount / budget) * 100 else 0.0

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Text(
                                            text = "${currencyFormat.format(overAmount)} over (${String.format("%.1f", percentOver)}%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Red
                                        )
                                    }
                                }
                            }
                        } else if (categoryBudgetsToUse.any { it.value > 0 }) {
                            Text(
                                text = "All category spending is within budget",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Text(
                                text = "Set category budgets to track your spending more effectively",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                if (filteredCategorySpending.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No expenses recorded for ${selectedMonth.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Spending by Category",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val surfaceColor = MaterialTheme.colorScheme.surface
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.minDimension / 2
                            val center = Offset(size.width / 2, size.height / 2)

                            var startAngle = 0f

                            filteredCategorySpending.entries.sortedByDescending { it.value }.forEachIndexed { index, (_, amount) ->
                                val sweepAngle = (amount / totalSpending * 360).toFloat()
                                val color = colors[index % colors.size]

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2, radius * 2)
                                )

                                drawArc(
                                    color = Color.White,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    style = Stroke(width = 2f),
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2, radius * 2)
                                )

                                startAngle += sweepAngle
                            }

                            drawCircle(
                                color = surfaceColor,
                                radius = size.minDimension / 6,
                                center = center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            if (filteredCategorySpending.isNotEmpty()) {
                items(filteredCategorySpending.entries.sortedByDescending { it.value }.toList()) { (category, amount) ->
                    val index = filteredCategorySpending.entries.sortedByDescending { it.value }.toList().indexOfFirst { it.key == category }
                    val color = colors[index % colors.size]
                    val percentage = amount / totalSpending * 100

                    val budget = categoryBudgetsToUse[category] ?: 0.0
                    val isOverBudget = budget > 0 && amount > budget
                    val percentOfBudget = if (budget > 0) (amount / budget) * 100 else 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOverBudget) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface,
                            contentColor = if (isOverBudget) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(color, CircleShape)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOverBudget) Color.Black else MaterialTheme.colorScheme.onSurface

                                    )

                                    Text(
                                        text = "Spent: ${currencyFormat.format(amount)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = "(${String.format("%.1f", percentage)}%)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverBudget) Color.Black else MaterialTheme.colorScheme.primary
                                )
                            }

                            if (budget > 0) {
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Budget: ${currencyFormat.format(budget)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isOverBudget) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isOverBudget)
                                                "${String.format("%.1f", percentOfBudget)}% of budget (${currencyFormat.format(amount - budget)} over)"
                                            else
                                                "${String.format("%.1f", percentOfBudget)}% of budget",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isOverBudget) Color.Red else Color.Gray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                LinearProgressIndicator(
                                    progress = (percentOfBudget / 100).toFloat().coerceIn(0f, 1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = when {
                                        percentOfBudget < 70 -> MaterialTheme.colorScheme.primary
                                        percentOfBudget < 90 -> Color(0xFFFFA000) // Amber
                                        else -> Color.Red
                                    },
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }else{
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "💡 Set a budget for this category to track progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    if (overBudgetCategoriesFiltered.isNotEmpty() && budgetToUse > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3E0),
                                contentColor = Color(0xFFE65100)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Budget Recommendations",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFE65100) // Deep Orange
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (totalSpending > budgetToUse) {
                                    Text(
                                        text = "Your total spending exceeds your monthly budget by ${currencyFormat.format(totalSpending - budgetToUse)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                overBudgetCategoriesFiltered.forEach { category ->
                                    val budget = categoryBudgetsToUse[category] ?: 0.0
                                    val spent = filteredCategorySpending[category] ?: 0.0

                                    Text(
                                        text = "• Consider reducing spending on $category (${currencyFormat.format(spent - budget)} over budget)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun MonthSelector(
    selectedMonth: Month,
    onMonthSelected: (Month) -> Unit
) {
    val currentYear = LocalDateTime.now().year
    val months = Month.values().toList()

    Column {
        Text(
            text = "Select Month",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            months.forEach { month ->
                FilterChip(
                    selected = month == selectedMonth,
                    onClick = { onMonthSelected(month) },
                    label = {
                        Text(month.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}