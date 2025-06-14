package com.example.bachelor_frontend.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.bachelor_frontend.classes.RecurringExpenseDto
import com.example.bachelor_frontend.classes.RecurrenceFrequency
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseDialog(
    categories: List<String>,
    currency: String,
    recurringExpense: RecurringExpenseDto? = null, // null for new, non-null for edit
    onSave: (RecurringExpenseDto) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(recurringExpense?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(recurringExpense?.category ?: categories.firstOrNull() ?: "Other") }
    var description by remember { mutableStateOf(recurringExpense?.description ?: "") }
    var selectedFrequency by remember { mutableStateOf(recurringExpense?.frequency ?: RecurrenceFrequency.MONTHLY) }
    var startDate by remember { mutableStateOf(recurringExpense?.startDate ?: LocalDate.now()) }
    var hasEndDate by remember { mutableStateOf(recurringExpense?.endDate != null) }
    var endDate by remember { mutableStateOf(recurringExpense?.endDate ?: LocalDate.now().plusYears(1)) }
    var automaticallyGenerate by remember { mutableStateOf(recurringExpense?.automaticallyGenerate ?: true) }
    var isActive by remember { mutableStateOf(recurringExpense?.isActive ?: true) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val currencyFormat = remember(currency) {
        NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = if (recurringExpense != null) "Edit Recurring Expense" else "Add Recurring Expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Amount input
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        showError = false
                    },
                    label = { Text("Amount") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = showError && amount.toDoubleOrNull() == null,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )

                // Category selection
                var showCategoryDropdown by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Netflix subscription, Rent, etc.") }
                )

                // Frequency selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecurrenceFrequency.values().take(3).forEach { frequency ->
                            FilterChip(
                                selected = selectedFrequency == frequency,
                                onClick = { selectedFrequency = frequency },
                                label = { Text(frequency.displayName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecurrenceFrequency.values().drop(3).forEach { frequency ->
                            FilterChip(
                                selected = selectedFrequency == frequency,
                                onClick = { selectedFrequency = frequency },
                                label = { Text(frequency.displayName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Start date
                OutlinedTextField(
                    value = startDate.format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Date") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Change date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // End date option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasEndDate,
                        onCheckedChange = { hasEndDate = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set end date")
                }

                if (hasEndDate) {
                    OutlinedTextField(
                        value = endDate.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Date") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showEndDatePicker = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Change date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Options
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = automaticallyGenerate,
                            onCheckedChange = { automaticallyGenerate = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Automatically generate expenses")
                            Text(
                                text = "Create expenses automatically on due dates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (recurringExpense != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isActive,
                                onCheckedChange = { isActive = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Active")
                                Text(
                                    text = "Uncheck to pause this recurring expense",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Preview
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        val nextPayment = if (recurringExpense != null)
                            selectedFrequency.getNextDate(recurringExpense.nextPaymentDate)
                        else selectedFrequency.getNextDate(startDate)

                        Text(
                            text = "${currencyFormat.format(amountValue)} • $selectedCategory • ${selectedFrequency.displayName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Next payment: ${nextPayment.format(dateFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Error message
                if (showError && errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Action buttons
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
                            val amountValue = amount.toDoubleOrNull()
                            when {
                                amountValue == null || amountValue <= 0 -> {
                                    errorMessage = "Please enter a valid amount"
                                    showError = true
                                }
                                description.isBlank() -> {
                                    errorMessage = "Please enter a description"
                                    showError = true
                                }
                                hasEndDate && endDate.isBefore(startDate) -> {
                                    errorMessage = "End date must be after start date"
                                    showError = true
                                }
                                else -> {
                                    val expense = recurringExpense?.copy(
                                        amount = amountValue,
                                        category = selectedCategory,
                                        description = description,
                                        frequency = selectedFrequency,
                                        startDate = startDate,
                                        endDate = if (hasEndDate) endDate else null,
                                        automaticallyGenerate = automaticallyGenerate,
                                        isActive = isActive
                                    ) ?: RecurringExpenseDto(
                                        amount = amountValue,
                                        category = selectedCategory,
                                        description = description,
                                        frequency = selectedFrequency,
                                        startDate = startDate,
                                        endDate = if (hasEndDate) endDate else null,
                                        nextPaymentDate = startDate,
                                        automaticallyGenerate = automaticallyGenerate,
                                        isActive = true
                                    )
                                    onSave(expense)
                                }
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                startDate = selectedDate
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
            initialDate = startDate
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                endDate = selectedDate
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
            initialDate = endDate
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    onDateSelected(selectedDate)
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}