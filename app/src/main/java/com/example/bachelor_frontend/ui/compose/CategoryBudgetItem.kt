package com.example.bachelor_frontend.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.*

@Composable
fun CategoryBudgetItem(
    category: String,
    budget: Double,
    spent: Double,
    currency: String,
    onEditClick: () -> Unit
) {
    val currencyFormat = remember(currency) {
        NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }
    }

    val percentUsed = if (budget > 0) (spent / budget) * 100 else 0.0
    val isOverBudget = budget > 0 && spent > budget
    val remainingBudget = budget - spent

    val progressColor = when {
        percentUsed < 70 -> MaterialTheme.colorScheme.primary
        percentUsed < 90 -> Color(0xFFFFA000) // Amber
        else -> Color(0xFFF44336) // Red
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
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
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Over Budget",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) Color.Black else MaterialTheme.colorScheme.onSurface

                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Budget",
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverBudget) Color.Black else MaterialTheme.colorScheme.onSurface
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
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverBudget) Color.Black else MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = currencyFormat.format(spent),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (budget > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isOverBudget) "Over by:" else "Remaining:",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverBudget) Color.Black else MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (isOverBudget)
                            "- ${currencyFormat.format(Math.abs(remainingBudget))}"
                        else
                            currencyFormat.format(remainingBudget),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    LinearProgressIndicator(
                        progress = (percentUsed / 100).toFloat().coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = progressColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${String.format("%.1f", percentUsed)}% of budget used",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverBudget) Color.Black else progressColor
                    )
                }
            } else if (spent > 0) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Set a budget for this category to track your spending",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        color = if (isOverBudget) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}