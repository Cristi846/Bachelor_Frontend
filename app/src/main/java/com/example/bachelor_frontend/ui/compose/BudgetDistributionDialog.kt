package com.example.bachelor_frontend.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.NumberFormat
import java.util.*

@Composable
fun BudgetDistributionDialog(
    categories: List<String>,
    totalBudget: Double,
    categorySpending: Map<String, Double>,
    currency: String,
    onDistribute: (Map<String, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    var distributionMethod by remember { mutableStateOf(DistributionMethod.EQUAL) }

    val currencyFormat = remember(currency) {
        NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }
    }

    val equalDistribution = calculateEqualDistribution(categories, totalBudget)
    val spendingBasedDistribution = calculateSpendingBasedDistribution(categories, totalBudget, categorySpending)

    val selectedDistribution = when (distributionMethod) {
        DistributionMethod.EQUAL -> equalDistribution
        DistributionMethod.SPENDING_BASED -> spendingBasedDistribution
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Distribute Monthly Budget",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        Text(
                            text = "This will distribute your total monthly budget of ${currencyFormat.format(totalBudget)} across categories.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Distribution Method",
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = distributionMethod == DistributionMethod.EQUAL,
                        onClick = { distributionMethod = DistributionMethod.EQUAL },
                        label = { Text("Equal Distribution") }
                    )

                    FilterChip(
                        selected = distributionMethod == DistributionMethod.SPENDING_BASED,
                        onClick = { distributionMethod = DistributionMethod.SPENDING_BASED },
                        label = { Text("Spending-Based") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    selectedDistribution.entries.sortedBy { it.key }.forEach { (category, amount) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = currencyFormat.format(amount),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (distributionMethod) {
                    DistributionMethod.EQUAL -> {
                        Text(
                            text = "Equal distribution allocates the same budget amount to each category.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    DistributionMethod.SPENDING_BASED -> {
                        Text(
                            text = "Spending-based distribution allocates budget based on your historical spending patterns.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            onDistribute(selectedDistribution)
                            onDismiss()
                        }
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

enum class DistributionMethod {
    EQUAL,
    SPENDING_BASED
}

private fun calculateEqualDistribution(
    categories: List<String>,
    totalBudget: Double
): Map<String, Double> {
    if (categories.isEmpty() || totalBudget <= 0) {
        return emptyMap()
    }

    val amountPerCategory = totalBudget / categories.size

    return categories.associateWith { amountPerCategory }
}

private fun calculateSpendingBasedDistribution(
    categories: List<String>,
    totalBudget: Double,
    categorySpending: Map<String, Double>
): Map<String, Double> {
    if (categories.isEmpty() || totalBudget <= 0) {
        return emptyMap()
    }

    val distribution = mutableMapOf<String, Double>()
    val totalSpent = categorySpending.values.sum().takeIf { it > 0 } ?: 1.0

    categories.forEach { category ->
        val spent = categorySpending[category] ?: 0.0
        val percentage = spent / totalSpent
        val budget = totalBudget * percentage

        distribution[category] = if (spent > 0) budget else totalBudget * 0.01
    }

    val sum = distribution.values.sum()
    if (sum > 0 && sum != totalBudget) {
        val factor = totalBudget / sum
        distribution.keys.forEach { category ->
            distribution[category] = distribution[category]!! * factor
        }
    }

    return distribution
}