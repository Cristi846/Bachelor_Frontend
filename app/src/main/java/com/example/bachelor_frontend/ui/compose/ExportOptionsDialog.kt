// ExportOptionsDialog.kt
package com.example.bachelor_frontend.ui.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDateTime
import java.time.Month
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onExportPDF: (month: Month, year: Int, sendEmail: Boolean) -> Unit,
    isLoading: Boolean = false
) {
    var selectedMonth by remember { mutableStateOf(LocalDateTime.now().month) }
    var selectedYear by remember { mutableStateOf(LocalDateTime.now().year) }
    var sendViaEmail by remember { mutableStateOf(true) } // Default to email
    var exportFormat by remember { mutableStateOf(ExportFormat.PDF) }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Export & Email Report",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email delivery section (more prominent)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sendViaEmail,
                                onCheckedChange = { sendViaEmail = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "📧 Send to my email",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Report will be sent directly to your registered email address",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (!sendViaEmail) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Share via other apps instead",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Format Selection
                Text(
                    text = "Export Format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = exportFormat == ExportFormat.PDF,
                        onClick = { exportFormat = ExportFormat.PDF },
                        label = { Text("📄 PDF Report") },
                        leadingIcon = {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        }
                    )
                    FilterChip(
                        selected = exportFormat == ExportFormat.CSV,
                        onClick = { exportFormat = ExportFormat.CSV },
                        label = { Text("📊 CSV Data") },
                        leadingIcon = {
                            Icon(Icons.Default.TableChart, contentDescription = null)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Month and Year Selection
                Text(
                    text = "Select Period",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Month Selector
                Text(
                    text = "Month",
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Month.values().forEach { month ->
                        FilterChip(
                            selected = selectedMonth == month,
                            onClick = { selectedMonth = month },
                            label = {
                                Text(month.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Year Selector
                Text(
                    text = "Year",
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentYear = LocalDateTime.now().year
                    (currentYear - 2..currentYear).forEach { year ->
                        FilterChip(
                            selected = selectedYear == year,
                            onClick = { selectedYear = year },
                            label = { Text(year.toString()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Preview Info
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
                            text = "📋 Export Summary",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Period: ${selectedMonth.getDisplayName(TextStyle.FULL, Locale.getDefault())} $selectedYear",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Format: ${exportFormat.displayName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Delivery: ${if (sendViaEmail) "Email" else "Share"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (exportFormat == ExportFormat.PDF) {
                            Text(
                                text = "• Includes: Budget analysis, insights, charts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onExportPDF(selectedMonth, selectedYear, sendViaEmail)
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                if (sendViaEmail) Icons.Default.Email else Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            if (isLoading) "Generating..."
                            else if (sendViaEmail) "📧 Send Email"
                            else "📤 Export ${exportFormat.displayName}"
                        )
                    }
                }
            }
        }
    }
}

enum class ExportFormat(val displayName: String) {
    PDF("PDF"),
    CSV("CSV")
}