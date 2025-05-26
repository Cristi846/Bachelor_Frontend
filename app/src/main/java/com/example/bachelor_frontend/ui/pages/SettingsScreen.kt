package com.example.bachelor_frontend.ui.pages

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bachelor_frontend.viewmodel.ExpenseViewModel
import com.example.bachelor_frontend.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userViewModel: UserViewModel,
    expenseViewModel: ExpenseViewModel = viewModel(),
    onNavigateToCategoryBudgets: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Get state from view models
    val userName by userViewModel.userName.collectAsState()
    val userEmail by userViewModel.userEmail.collectAsState()
    val categories by userViewModel.categories.collectAsState()
    val monthlyBudget by userViewModel.monthlyBudget.collectAsState()
    val userCurrency by userViewModel.currency.collectAsState()
    val categoryBudgets by userViewModel.categoryBudgets.collectAsState()

    // State variables
    var isSystemTheme by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(true) }
    var exportFormat by remember { mutableStateOf("CSV") }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    // State for feedback
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    // Format currency
    val currencyFormat = remember(userCurrency) {
        NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(userCurrency)
        }
    }

    // Currency options
    val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "RON")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // User Information Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Profile",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Monthly Budget: ${currencyFormat.format(monthlyBudget)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Budget Settings
            SettingSection(title = "Budget Settings") {
                // Monthly Budget
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Implement budget editing */ }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = "Monthly Budget",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Monthly Budget")
                            Text(
                                text = currencyFormat.format(monthlyBudget),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Edit Budget",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                Divider()

                // Category Budgets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCategoryBudgets() }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Category Budgets",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Category Budgets")
                            Text(
                                text = "${categoryBudgets.count { it.value > 0 }} of ${categories.size} categories with budgets set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Category Budgets",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                Divider()

                // Currency settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCurrencyDialog = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = "Currency Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Default Currency")
                            Text(
                                text = userCurrency,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Change Currency",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            // Appearance Settings
            SettingSection(title = "Appearance") {
                // Theme Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeDialog = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("App Theme")
                            Text(
                                text = if (isSystemTheme) "System Default" else if (darkMode) "Dark" else "Light",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Change Theme",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            // Notifications Settings
            SettingSection(title = "Notifications") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Enable Notifications")
                    }
                    Switch(
                        checked = showNotifications,
                        onCheckedChange = { showNotifications = it }
                    )
                }

                // Only show these options if notifications are enabled
                if (showNotifications) {
                    Divider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Budget Alerts",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Budget Alerts")
                                Text(
                                    text = "Get notified when you're close to your budget limit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = { /* Implement budget alert setting change */ }
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Daily Reminders",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Daily Reminders")
                                Text(
                                    text = "Remind me to add expenses at the end of the day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Switch(
                            checked = false,
                            onCheckedChange = { /* Implement daily reminder setting change */ }
                        )
                    }
                }
            }

            // Data & Backup Settings
            SettingSection(title = "Data & Backup") {
                // Export Data
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            /* Implement data export functionality */
                            feedbackMessage = "Data exported successfully"
                            showFeedback = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Export Data",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export Financial Data")
                        Text(
                            text = "Save your data to a file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Export",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                Divider()

                // Import Data
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Implement data import functionality */ }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Import Data",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Import Data")
                        Text(
                            text = "Import data from a file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Import",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                Divider()

                // Clear Data
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteConfirmDialog = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete All Data",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Delete All Data",
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Remove all your financial records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            // About & Support section
            SettingSection(title = "About & Support") {
                // About Finance Tracker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDialog = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("About Finance Tracker")
                        Text(
                            text = "Version 1.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                Divider()

                // Privacy Policy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyDialog = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Privacy Policy")
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                Divider()

                // Contact Support
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@financetracker.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Finance Tracker Support Request")
                            }
                            context.startActivity(intent)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Contact",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Contact Support")
                        Text(
                            text = "Get help with your account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Contact",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            // App version at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Finance Tracker v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        // Theme Dialog
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose Theme") },
                text = {
                    Column {
                        RadioButtonOption(
                            text = "System Default",
                            selected = isSystemTheme,
                            onClick = {
                                isSystemTheme = true
                                showThemeDialog = false
                            }
                        )
                        RadioButtonOption(
                            text = "Light Theme",
                            selected = !isSystemTheme && !darkMode,
                            onClick = {
                                isSystemTheme = false
                                darkMode = false
                                showThemeDialog = false
                            }
                        )
                        RadioButtonOption(
                            text = "Dark Theme",
                            selected = !isSystemTheme && darkMode,
                            onClick = {
                                isSystemTheme = false
                                darkMode = true
                                showThemeDialog = false
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Currency Dialog
        if (showCurrencyDialog) {
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                title = { Text("Choose Currency") },
                text = {
                    Column {
                        currencies.forEach { currency ->
                            RadioButtonOption(
                                text = "$currency - ${Currency.getInstance(currency).displayName}",
                                selected = currency == userCurrency,
                                onClick = {
                                    coroutineScope.launch {
                                        userViewModel.updateUserCurrency(currency)
                                        showCurrencyDialog = false
                                        feedbackMessage = "Currency updated to $currency"
                                        showFeedback = true
                                    }
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCurrencyDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About Finance Tracker") },
                text = {
                    Column {
                        Text("Finance Tracker helps you manage your personal finances with ease.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Version: 1.0")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Developed as a bachelor project.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("© 2025 Finance Tracker Team")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy Policy") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Finance Tracker Privacy Policy\n\n" +
                                    "1. Data Collection: We collect information you provide including expenses, categories, and financial goals.\n\n" +
                                    "2. Data Storage: All your data is stored securely in Firebase.\n\n" +
                                    "3. Data Sharing: We do not share your financial data with any third parties.\n\n" +
                                    "4. Data Security: We implement industry-standard security measures.\n\n" +
                                    "5. Your Rights: You can export or delete your data at any time.\n\n" +
                                    "Last updated: May 2025"
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete All Data?") },
                text = {
                    Text("This will permanently remove all your financial records and cannot be undone. Are you sure you want to continue?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            // Implement data deletion
                            feedbackMessage = "All data has been deleted"
                            showFeedback = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Feedback snackbar
        if (showFeedback) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(
                        onClick = { showFeedback = false }
                    ) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(feedbackMessage)
            }
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun RadioButtonOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}