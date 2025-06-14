package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.bachelor_frontend.classes.BudgetType
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.ui.compose.ExpenseItem
import com.example.bachelor_frontend.ui.compose.SummaryCard
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    expenses: List<ExpenseDto>,
    monthlyBudget: Double,
    familyViewModel: FamilyViewModel,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (ExpenseDto) -> Unit,
    onScanReceiptClick: () -> Unit,
    onChatExpenseClick: () -> Unit = {},
    onDocumentScanClick: () -> Unit = {}
) {
    val currentMonth = remember { LocalDateTime.now().month }
    val currentYear = remember { LocalDateTime.now().year }

    val firstDayOfMonth = remember {
        LocalDateTime.of(currentYear, currentMonth, 1, 0, 0)
    }

    val lastDayOfMonth = remember {
        LocalDateTime.of(currentYear, currentMonth, 1, 23, 59)
            .with(TemporalAdjusters.lastDayOfMonth())
    }

    val currentMonthExpenses = expenses.filter { expense ->
        expense.timestamp.isAfter(firstDayOfMonth) && expense.timestamp.isBefore(lastDayOfMonth)
    }

    val totalSpent = currentMonthExpenses.sumOf { it.amount }
    val remaining = (monthlyBudget - totalSpent).coerceAtLeast(0.0)

    val userFamily by familyViewModel.family.collectAsState()

    var selectedBudgetFilter by remember { mutableStateOf("All") }
    val filteredExpenses = expenses.filter { expense ->
        when(selectedBudgetFilter){
            "Personal" -> expense.budgetType == BudgetType.PERSONAL
            "Family" -> expense.budgetType == BudgetType.FAMILY
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onChatExpenseClick) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Chat Assistant",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column {

                SmallFloatingActionButton(
                    onClick = {onChatExpenseClick()},
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat Expense")
                }
                SmallFloatingActionButton(
                    onClick = { onScanReceiptClick() },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Scan Receipt")
                }

                FloatingActionButton(
                    onClick = { onAddExpenseClick() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                SummaryCard(
                    title = "Monthly Budget",
                    amount = monthlyBudget
                )

                SummaryCard(
                    title = "Total Spent (${currentMonth.name})",
                    amount = totalSpent,
                    budgetAmount = monthlyBudget,
                    color = if (totalSpent > monthlyBudget) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                )

                SummaryCard(
                    title = "Remaining Budget",
                    amount = remaining,
                    color = if (remaining > monthlyBudget * 0.2) MaterialTheme.colorScheme.primary else Color(0xFFF44336)
                )

                if (userFamily != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedBudgetFilter == "All",
                            onClick = { selectedBudgetFilter = "All" },
                            label = { Text("All Expenses") },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedBudgetFilter == "Personal",
                            onClick = { selectedBudgetFilter = "Personal" },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Personal")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedBudgetFilter == "Family",
                            onClick = { selectedBudgetFilter = "Family" },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Family")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No expenses recorded for ${currentMonth.name}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = onAddExpenseClick) {
                                    Text("Add Expense")
                                }

                                OutlinedButton(onClick = onScanReceiptClick) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Scan Receipt",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan Receipt")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredExpenses.sortedByDescending { it.timestamp }) { expense ->
                    ExpenseItem(
                        expense = expense,
                        onClick = onExpenseClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}