package com.example.bachelor_frontend.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bachelor_frontend.classes.RecurringExpenseDto
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@Composable
fun RecurringExpenseItem(
    recurringExpense: RecurringExpenseDto,
    currency: String,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = remember(currency) {
        NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val today = LocalDate.now()
    val daysUntilNext = ChronoUnit.DAYS.between(today, recurringExpense.nextPaymentDate)

    val isOverdue = recurringExpense.nextPaymentDate.isBefore(today)
    val isDueToday = recurringExpense.nextPaymentDate.isEqual(today)
    val isDueSoon = daysUntilNext <= 3 && daysUntilNext > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !recurringExpense.isActive -> MaterialTheme.colorScheme.surfaceVariant
                isOverdue -> Color(0xFFFFF3E0) // Light orange
                isDueToday -> Color(0xFFE8F5E8) // Light green
                isDueSoon -> Color(0xFFF0F4FF) // Light blue
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with amount and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!recurringExpense.isActive) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Paused",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                    } else if (isOverdue) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Overdue",
                            tint = Color(0xFFE65100), // Dark orange
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                    } else if (isDueToday) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Due Today",
                            tint = Color(0xFF2E7D32), // Dark green
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                    }

                    Text(
                        text = currencyFormat.format(recurringExpense.amount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (!recurringExpense.isActive)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.primary
                    )
                }

                // Actions menu
                var showMenu by remember { mutableStateOf(false) }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(if (recurringExpense.isActive) "Pause" else "Resume")
                            },
                            onClick = {
                                showMenu = false
                                onToggleActive()
                            },
                            leadingIcon = {
                                Icon(
                                    if (recurringExpense.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recurringExpense.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (!recurringExpense.isActive)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = recurringExpense.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Frequency badge
                AssistChip(
                    onClick = { },
                    label = { Text(recurringExpense.frequency.displayName) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (!recurringExpense.isActive)
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = if (!recurringExpense.isActive)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Next payment info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Next Payment",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Text(
                        text = recurringExpense.nextPaymentDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            !recurringExpense.isActive -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            isOverdue -> Color(0xFFE65100)
                            isDueToday -> Color(0xFF2E7D32)
                            isDueSoon -> Color(0xFF1565C0)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                // Status text
                if (recurringExpense.isActive) {
                    Text(
                        text = when {
                            isOverdue -> "Overdue"
                            isDueToday -> "Due Today"
                            isDueSoon -> "Due in $daysUntilNext days"
                            daysUntilNext <= 7 -> "Due in $daysUntilNext days"
                            else -> "Due in $daysUntilNext days"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isOverdue -> Color(0xFFE65100)
                            isDueToday -> Color(0xFF2E7D32)
                            isDueSoon -> Color(0xFF1565C0)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        }
                    )
                } else {
                    Text(
                        text = "Paused",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Additional info for end date
            recurringExpense.endDate?.let { endDate ->
                if (endDate.isAfter(today)) {
                    Spacer(modifier = Modifier.height(8.dp))

                    val daysUntilEnd = ChronoUnit.DAYS.between(today, endDate)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ends on ${endDate.format(dateFormatter)} ($daysUntilEnd days remaining)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Auto-generate indicator
            if (!recurringExpense.automaticallyGenerate) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Reminder only",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reminder only - won't auto-generate expenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}