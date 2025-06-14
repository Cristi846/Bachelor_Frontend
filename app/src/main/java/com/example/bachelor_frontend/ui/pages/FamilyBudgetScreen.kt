package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bachelor_frontend.classes.FamilyBudget
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import com.example.bachelor_frontend.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyBudgetScreen(
    familyViewModel: FamilyViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val family by familyViewModel.family.collectAsState()
    val familyAnalytics by familyViewModel.familyAnalytics.collectAsState()
    val isLoading by familyViewModel.isLoading.collectAsState()
    val error by familyViewModel.error.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }

    val isAdmin = remember(family) {
        family?.let { familyViewModel.isCurrentUserAdmin() } ?: false
    }

    // Currency formatter
    val currencyFormat = remember(family?.sharedBudget?.currency) {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(family?.sharedBudget?.currency ?: "USD")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Budget") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (isAdmin && family != null) {
                        IconButton(onClick = { showBudgetDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Budget")
                        }
                    }
                }
            )
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    // Budget overview
                    item {
                        BudgetOverviewCard(
                            family = familyData,
                            analytics = familyAnalytics,
                            currencyFormat = currencyFormat
                        )
                    }

                    if (isAdmin && familyData.sharedBudget.monthlyBudget <= 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalance,
                                        contentDescription = "Setup Budget",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Setup Family Budget",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "As an admin, you can set up the monthly budget for your family",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showBudgetDialog = true }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Setup Budget")
                                    }
                                }
                            }
                        }
                    }


                    // Category budgets
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category Budgets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (isAdmin) {
                                TextButton(onClick = { showCategoryDialog = true }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit")
                                }
                            }
                        }
                    }

                    if (familyData.sharedBudget.categoryBudgets.isEmpty()) {
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
                                        Icons.Default.AccountBalance,
                                        contentDescription = "No Budgets",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No category budgets set",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Set budgets for different categories to track spending",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )

                                    if (isAdmin) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { showCategoryDialog = true }) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Set Category Budgets")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(familyData.sharedBudget.categoryBudgets.entries.sortedBy { it.key }) { (category, budget) ->
                            val spent = familyAnalytics?.categorySpending?.get(category) ?: 0.0

                            CategoryBudgetCard(
                                category = category,
                                budget = budget,
                                spent = spent,
                                currencyFormat = currencyFormat,
                                onEditClick = if (isAdmin) {
                                    {
                                        editingCategory = category
                                        showCategoryDialog = true
                                    }
                                } else null
                            )
                        }
                    }
                } ?: run {
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
                                    text = "Create or join a family to manage shared budgets",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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

    // Edit budget dialog
    if (showBudgetDialog && family != null && isAdmin) {
        EditBudgetDialog(
            currentBudget = family!!.sharedBudget,
            onSave = { newBudget ->
                coroutineScope.launch {
                    familyViewModel.updateFamilyBudget(newBudget)
                    showBudgetDialog = false
                }
            },
            onDismiss = { showBudgetDialog = false }
        )
    }

    // Edit category budget dialog
    if (showCategoryDialog && family != null && isAdmin) {
        EditCategoryBudgetDialog(
            category = editingCategory,
            currentBudgets = family!!.sharedBudget.categoryBudgets,
            currency = family!!.sharedBudget.currency,
            onSave = { updatedBudgets ->
                val newBudget = family!!.sharedBudget.copy(categoryBudgets = updatedBudgets)
                coroutineScope.launch {
                    familyViewModel.updateFamilyBudget(newBudget)
                    showCategoryDialog = false
                    editingCategory = null
                }
            },
            onDismiss = {
                showCategoryDialog = false
                editingCategory = null
            }
        )
    }
}

@Composable
fun BudgetOverviewCard(
    family: com.example.bachelor_frontend.classes.FamilyDto,
    analytics: com.example.bachelor_frontend.classes.FamilyAnalyticsResponse?,
    currencyFormat: NumberFormat
) {
    val totalSpent = analytics?.totalFamilySpending ?: 0.0
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
                text = family.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                        text = "Total Spent",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = currencyFormat.format(totalSpent),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isOverBudget) "Over Budget:" else "Remaining:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (isOverBudget)
                        "- ${currencyFormat.format(Math.abs(remaining))}"
                    else
                        currencyFormat.format(remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.primary
                )
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
                    text = "${String.format("%.1f", progress * 100)}% of budget used",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Show over budget warning
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

@Composable
fun CategoryBudgetCard(
    category: String,
    budget: Double,
    spent: Double,
    currencyFormat: NumberFormat,
    onEditClick: (() -> Unit)?
) {
    val isOverBudget = budget > 0 && spent > budget
    val percentUsed = if (budget > 0) (spent / budget) * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface,
            contentColor = if (isOverBudget) Color.Black else MaterialTheme.colorScheme.onSurface
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverBudget) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Over Budget",
                            tint = Color.Red,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                onEditClick?.let { onClick ->
                    IconButton(onClick = onClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = currencyFormat.format(budget),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = currencyFormat.format(spent),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (budget > 0) {
                Spacer(modifier = Modifier.height(8.dp))

                val progress = (percentUsed / 100).toFloat().coerceIn(0f, 1f)
                val progressColor = when {
                    percentUsed < 70 -> MaterialTheme.colorScheme.primary
                    percentUsed < 90 -> Color(0xFFFFA000) // Amber
                    else -> Color(0xFFF44336) // Red
                }

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = progressColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${String.format("%.1f", percentUsed)}% used",
                    style = MaterialTheme.typography.bodySmall,
                    color = progressColor
                )
            }
        }
    }
}

@Composable
fun EditBudgetDialog(
    currentBudget: FamilyBudget,
    onSave: (FamilyBudget) -> Unit,
    onDismiss: () -> Unit
) {
    var monthlyBudget by remember { mutableStateOf(currentBudget.monthlyBudget.toString()) }
    var currency by remember { mutableStateOf(currentBudget.currency) }
    var showError by remember { mutableStateOf(false) }
    var showCurrencyWarning by remember { mutableStateOf(false) }

    val currencies = listOf(
        "USD" to "US Dollar",
        "EUR" to "Euro",
        "GBP" to "British Pound",
        "RON" to "Romanian Leu",
        "CAD" to "Canadian Dollar",
        "AUD" to "Australian Dollar",
        "JPY" to "Japanese Yen",
        "CHF" to "Swiss Franc",
        "SEK" to "Swedish Krona",
        "NOK" to "Norwegian Krone"
    )

    // Show warning if currency is being changed
    LaunchedEffect(currency) {
        showCurrencyWarning = currency != currentBudget.currency
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Edit Family Budget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = monthlyBudget,
                    onValueChange = {
                        monthlyBudget = it
                        showError = false
                    },
                    label = { Text("Monthly Budget") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = showError,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (showError) {
                            Text("Please enter a valid amount")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Family Currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Current currency display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CurrencyExchange,
                            contentDescription = "Currency",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Selected Currency",
                                style = MaterialTheme.typography.labelMedium
                            )
                            val selectedCurrencyName = currencies.find { it.first == currency }?.second ?: currency
                            Text(
                                text = "$currency - $selectedCurrencyName",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Currency selection chips
                Text(
                    text = "Available Currencies",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Currency grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.chunked(2).forEach { currencyRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currencyRow.forEach { (currencyCode, currencyName) ->
                                FilterChip(
                                    selected = currency == currencyCode,
                                    onClick = { currency = currencyCode },
                                    label = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = currencyCode,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = currencyName,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }

                            // Add empty space if row has only 1 item
                            if (currencyRow.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Currency change warning
                if (showCurrencyWarning) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Currency Change Warning",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Changing the family currency will affect all family members and existing expenses display. The amounts themselves won't be converted.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val budgetValue = monthlyBudget.toDoubleOrNull()
                            if (budgetValue != null && budgetValue >= 0) {
                                val newBudget = currentBudget.copy(
                                    monthlyBudget = budgetValue,
                                    currency = currency
                                )
                                onSave(newBudget)
                            } else {
                                showError = true
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun EditCategoryBudgetDialog(
    category: String?,
    currentBudgets: Map<String, Double>,
    currency: String,
    onSave: (Map<String, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf("Food", "Transportation", "Housing", "Entertainment", "Utilities", "Healthcare", "Shopping", "Other")
    var budgets by remember { mutableStateOf(currentBudgets.toMutableMap()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (category != null) "Edit $category Budget" else "Set Category Budgets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                val categoriesToShow = if (category != null) listOf(category) else categories

                categoriesToShow.forEach { cat ->
                    var budgetText by remember { mutableStateOf((budgets[cat] ?: 0.0).toString()) }

                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = {
                            budgetText = it
                            val value = it.toDoubleOrNull()
                            if (value != null && value >= 0) {
                                budgets[cat] = value
                            }
                        },
                        label = { Text("$cat Budget") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onSave(budgets.toMap())
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}