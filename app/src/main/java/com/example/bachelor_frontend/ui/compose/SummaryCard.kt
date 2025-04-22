package com.example.bachelor_frontend.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    budgetAmount: Double? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )

            if (budgetAmount != null) {
                Spacer(modifier = Modifier.height(8.dp))

                val progress = (amount / budgetAmount).coerceIn(0.0, 1.0).toFloat()
                val progressColor = when {
                    progress < 0.7 -> MaterialTheme.colorScheme.primary
                    progress < 0.9 -> Color(0xFFFFA000) // Amber
                    else -> Color(0xFFF44336) // Red
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = progressColor
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Budget: $${String.format("%.2f", budgetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}