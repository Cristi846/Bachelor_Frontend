package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
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
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.viewmodel.ExpenseViewModel
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
    onNavigateToCategoryBudgets: () -> Unit
) {
    // State from ViewModels
    val categorySpending by expenseViewModel.categorySpending.collectAsState()
    val categoryBudgets by userViewModel.categoryBudgets.collectAsState()
    val categoryAnalysis by expenseViewModel.categoryAnalysis.collectAsState()
    val overBudgetCategories by expenseViewModel.overBudgetCategories.collectAsState()
    val currency by userViewModel.currency.collectAsState()

    // State for selected month (default to current month)
    var selectedMonth by remember { mutableStateOf(LocalDateTime.now().month) }
    val selectedYear by remember { mutableStateOf(LocalDateTime.now().year) }

    // Call the ViewModel to update data when selection changes
    LaunchedEffect(selectedMonth, selectedYear) {
        expenseViewModel.setSelectedMonth(selectedMonth, selectedYear)
    }

    // Format for currency display
    val currencyFormat = remember(currency) {
        NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }
    }

    // Calculate total spending
    val totalSpending = categorySpending.values.sum()

    // Generate colors for the pie chart
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
            // Month selector
            item {
                MonthSelector(
                    selectedMonth = selectedMonth,
                    onMonthSelected = { selectedMonth = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Spending summary
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
                            color = if (totalSpending > monthlyBudget && monthlyBudget > 0)
                                Color(0xFFF44336) else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (monthlyBudget > 0) {
                            val progress = (totalSpending / monthlyBudget).coerceIn(0.0, 1.0).toFloat()
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
                                text = "${(progress * 100).toInt()}% of monthly budget used",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Category Budget Summary
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
                        // Header with button to budget screen
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

                        // Show number of categories over budget
                        if (overBudgetCategories.isNotEmpty()) {
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
                                    text = "${overBudgetCategories.size} ${if (overBudgetCategories.size == 1) "category is" else "categories are"} over budget",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Red
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // List of over budget categories
                        if (overBudgetCategories.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                overBudgetCategories.forEach { category ->
                                    val analysis = categoryAnalysis[category]
                                    val budget = analysis?.get("budget") ?: 0.0
                                    val spent = analysis?.get("spent") ?: 0.0
                                    val overAmount = spent - budget
                                    val percentOver = (overAmount / budget) * 100

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
                        } else if (categoryBudgets.any { it.value > 0 }) {
                            // All categories under budget
                            Text(
                                text = "All category spending is within budget",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            // No budgets set
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

            // Pie Chart
            item {
                if (categorySpending.isEmpty()) {
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

                    // Draw pie chart
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val surfaceColor = MaterialTheme.colorScheme.surface
                        // Main pie chart
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.minDimension / 2
                            val center = Offset(size.width / 2, size.height / 2)

                            var startAngle = 0f

                            categorySpending.entries.sortedByDescending { it.value }.forEachIndexed { index, (_, amount) ->
                                val sweepAngle = (amount / totalSpending * 360).toFloat()
                                val color = colors[index % colors.size]

                                // Draw pie slice
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2, radius * 2)
                                )

                                // Draw outline
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

                            // Draw inner circle
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

            // Category legend and breakdown
            if (categorySpending.isNotEmpty()) {
                items(categorySpending.entries.sortedByDescending { it.value }.toList()) { (category, amount) ->
                    val index = categorySpending.entries.sortedByDescending { it.value }.toList().indexOfFirst { it.key == category }
                    val color = colors[index % colors.size]
                    val percentage = amount / totalSpending * 100

                    // Get budget info for category
                    val budget = categoryBudgets[category] ?: 0.0
                    val isOverBudget = budget > 0 && amount > budget
                    val percentOfBudget = if (budget > 0) (amount / budget) * 100 else 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOverBudget) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
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
                                        .background(color)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = currencyFormat.format(amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "(${String.format("%.1f", percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }

                            // Show budget comparison if budget is set
                            if (budget > 0) {
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Budget: ${currencyFormat.format(budget)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = if (isOverBudget)
                                            "${String.format("%.1f", percentOfBudget)}% of budget (${currencyFormat.format(amount - budget)} over)"
                                        else
                                            "${String.format("%.1f", percentOfBudget)}% of budget",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOverBudget) Color.Red else Color.Gray
                                    )
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
                                    }
                                )
                            }
                        }
                    }
                }

                // Budget recommendations
                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    if (overBudgetCategories.isNotEmpty() && monthlyBudget > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3E0) // Light Orange
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

                                if (totalSpending > monthlyBudget) {
                                    Text(
                                        text = "Your total spending exceeds your monthly budget by ${currencyFormat.format(totalSpending - monthlyBudget)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Show recommendations for over-budget categories
                                overBudgetCategories.forEach { category ->
                                    val analysis = categoryAnalysis[category]
                                    val budget = analysis?.get("budget") ?: 0.0
                                    val spent = analysis?.get("spent") ?: 0.0

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