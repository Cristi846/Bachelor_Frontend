package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.ui.compose.ExpenseItem
import com.example.bachelor_frontend.viewmodel.ExpenseViewModel
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyExpensesScreen(
    familyViewModel: FamilyViewModel = viewModel(),
    expenseViewModel: ExpenseViewModel = viewModel(),
    onNavigateToAddExpense: () -> Unit
) {
    val family by familyViewModel.family.collectAsState()
    val familyExpenses by familyViewModel.familyExpenses.collectAsState()
    val familyAnalytics by familyViewModel.familyAnalytics.collectAsState()
    val familyMembers by familyViewModel.familyMembers.collectAsState()
    val isLoading by familyViewModel.isLoading.collectAsState()
    val error by familyViewModel.error.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedMember by remember { mutableStateOf("All Members") }

    // Get current month expenses
    val currentMonth = remember { LocalDateTime.now().month }
    val currentYear = remember { LocalDateTime.now().year }
    val firstDayOfMonth = remember {
        LocalDateTime.of(currentYear, currentMonth, 1, 0, 0)
    }
    val lastDayOfMonth = remember {
        LocalDateTime.of(currentYear, currentMonth, 1, 23, 59)
            .with(TemporalAdjusters.lastDayOfMonth())
    }

    val currentMonthExpenses = familyExpenses.filter { expense ->
        expense.timestamp.isAfter(firstDayOfMonth) && expense.timestamp.isBefore(lastDayOfMonth)
    }

    // Filter expenses based on selected filters
    val filteredExpenses = currentMonthExpenses.filter { expense ->
        val categoryMatch = selectedFilter == "All" || expense.category == selectedFilter
        val memberMatch = if (selectedMember == "All Members") {
            true
        }else{
            val selectedMemberData = familyMembers.values.find {
                "${it.name} (${it.email.take(10)}...)" == selectedMember
            }
            selectedMemberData?.userId == expense.userId
        }
        categoryMatch && memberMatch
    }

    // Currency formatter
    val currencyFormat = remember(family?.sharedBudget?.currency) {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(family?.sharedBudget?.currency ?: "USD")
        }
    }

    // Categories for filter
    val categories = remember(familyExpenses) {
        listOf("All") + familyExpenses.map { it.category }.distinct().sorted()
    }

    // Members for filter (would need to get member names from user data)
    val members = remember(family, familyMembers) {
        listOf("All Members") + (familyMembers.values.map { "${it.name} (${it.email.take(10)}...)" } ?: emptyList())
    }

    LaunchedEffect(Unit) {
        familyViewModel.refreshFamilyData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Expenses") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExpense,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Family Expense")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Error message
                error?.let { errorMessage ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                family?.let { familyData ->
                    // Summary card
                    item {
                        FamilyExpenseSummaryCard(
                            family = familyData,
                            analytics = familyAnalytics,
                            currentMonthExpenses = currentMonthExpenses,
                            currencyFormat = currencyFormat
                        )
                    }

                    // Filters
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Filters",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Category filter
                                Text(
                                    text = "Category",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(categories) { category ->
                                        FilterChip(
                                            selected = selectedFilter == category,
                                            onClick = { selectedFilter = category },
                                            label = { Text(category) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Member filter
                                Text(
                                    text = "Member",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(members) { member ->
                                        FilterChip(
                                            selected = selectedMember == member,
                                            onClick = { selectedMember = member },
                                            label = { Text(member) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Expenses list
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Expenses (${filteredExpenses.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (filteredExpenses.isNotEmpty()) {
                                Text(
                                    text = "Total: ${currencyFormat.format(filteredExpenses.sumOf { it.amount })}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (filteredExpenses.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = "No Expenses",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No expenses found",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Add family expenses to track spending together",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = onNavigateToAddExpense) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Expense")
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredExpenses.sortedByDescending { it.timestamp }) { expense ->
                            val memberName = familyMembers[expense.userId]?.name
                            ExpenseItem(
                                expense = expense,
                                onClick = { /* Handle expense click */ },
                                memberName = memberName
                            )
                        }
                    }

                    // Bottom spacing for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                } ?: run {
                    // No family
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Groups,
                                    contentDescription = "No Family",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No family found",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Create or join a family to track expenses together",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun FamilyExpenseSummaryCard(
    family: com.example.bachelor_frontend.classes.FamilyDto,
    analytics: com.example.bachelor_frontend.classes.FamilyAnalyticsResponse?,
    currentMonthExpenses: List<ExpenseDto>,
    currencyFormat: NumberFormat
) {
    val totalSpent = currentMonthExpenses.sumOf { it.amount }
    val monthlyBudget = family.sharedBudget.monthlyBudget
    val remaining = monthlyBudget - totalSpent
    val isOverBudget = totalSpent > monthlyBudget && monthlyBudget > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${family.name} - ${LocalDateTime.now().month.name} Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = currencyFormat.format(monthlyBudget),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = currencyFormat.format(totalSpent),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isOverBudget) "Over" else "Remaining",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (isOverBudget)
                            currencyFormat.format(Math.abs(remaining))
                        else
                            currencyFormat.format(remaining),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (monthlyBudget > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                val progress = (totalSpent / monthlyBudget).coerceIn(0.0, 1.0).toFloat()
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
                    text = "${String.format("%.1f", progress * 100)}% of budget used • ${currentMonthExpenses.size} expenses",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Over budget warning
            if (analytics?.overBudgetCategories?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEE8E8)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${analytics.overBudgetCategories.size} categories over budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}
