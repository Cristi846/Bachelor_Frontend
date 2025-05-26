package com.example.bachelor_frontend.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.NumberFormat
import java.util.*

@Composable
fun CategoryBudgetEditDialog(
    category: String,
    currentBudget: Double,
    currency: String,
    onBudgetChange: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var budgetValue by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "") }
    var showError by remember { mutableStateOf(false) }

    // Format for currency display
    val currencyFormat = remember(currency) {
        NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Budget for $category",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Budget input field
                OutlinedTextField(
                    value = budgetValue,
                    onValueChange = {
                        budgetValue = it
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

                // Current budget display
                if (currentBudget > 0) {
                    Text(
                        text = "Current budget: ${currencyFormat.format(currentBudget)}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val newBudget = budgetValue.toDoubleOrNull()
                            if (newBudget != null && newBudget >= 0) {
                                onBudgetChange(newBudget)
                                onDismiss()
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