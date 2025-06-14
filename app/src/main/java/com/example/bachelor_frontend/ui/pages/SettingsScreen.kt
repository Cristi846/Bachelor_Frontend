package com.example.bachelor_frontend.ui.pages

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import com.example.bachelor_frontend.utils.NotificationPreferences
import com.example.bachelor_frontend.utils.NotificationScheduler
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

    // Notification preferences
    val notificationPreferences = remember { NotificationPreferences(context) }
    val notificationScheduler = remember { NotificationScheduler(context) }

    val notificationsEnabled by notificationPreferences.notificationsEnabled.collectAsState()
    val budgetAlertsEnabled by notificationPreferences.budgetAlertsEnabled.collectAsState()
    val dailyRemindersEnabled by notificationPreferences.dailyRemindersEnabled.collectAsState()
    val budgetAlertThreshold by notificationPreferences.budgetAlertThreshold.collectAsState()
    val reminderTimeHour by notificationPreferences.reminderTimeHour.collectAsState()
    val reminderTimeMinute by notificationPreferences.reminderTimeMinute.collectAsState()

    // State variables
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Permission launcher for notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationPreferences.setNotificationsEnabled(true)
        }
    }

    // State for feedback
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    // Format currency
    val currencyFormat = remember(userCurrency) {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(userCurrency)
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
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationPreferences.setNotificationsEnabled(enabled)
                                if (!enabled) {
                                    // Cancel daily reminders when notifications are disabled
                                    notificationScheduler.cancelDailyReminder()
                                }
                            }
                        }
                    )
                }

                // Only show these options if notifications are enabled
                if (notificationsEnabled) {
                    Divider()

                    // Budget Alerts
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Budget Alerts")
                                Text(
                                    text = "Get notified at ${budgetAlertThreshold}% of budget limit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showThresholdDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Switch(
                                checked = budgetAlertsEnabled,
                                onCheckedChange = {
                                    notificationPreferences.setBudgetAlertsEnabled(it)
                                }
                            )
                        }
                    }

                    Divider()

                    // Daily Reminders
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Reminders")
                                Text(
                                    text = "Remind me at ${String.format("%02d:%02d", reminderTimeHour, reminderTimeMinute)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showTimePickerDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = "Set Time",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Switch(
                                checked = dailyRemindersEnabled,
                                onCheckedChange = { enabled ->
                                    notificationPreferences.setDailyRemindersEnabled(enabled)
                                    if (enabled) {
                                        notificationScheduler.scheduleDailyReminder(reminderTimeHour, reminderTimeMinute)
                                    } else {
                                        notificationScheduler.cancelDailyReminder()
                                    }
                                }
                            )
                        }
                    }
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

        // Budget Alert Threshold Dialog
        if (showThresholdDialog) {
            var thresholdValue by remember { mutableStateOf(budgetAlertThreshold.toString()) }

            AlertDialog(
                onDismissRequest = { showThresholdDialog = false },
                title = { Text("Budget Alert Threshold") },
                text = {
                    Column {
                        Text("Get notified when you reach this percentage of your budget:")

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = thresholdValue,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                    thresholdValue = it
                                }
                            },
                            label = { Text("Threshold (%)") },
                            trailingIcon = { Text("%") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Recommended: 80% (between 50% and 95%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val threshold = thresholdValue.toIntOrNull()
                            if (threshold != null && threshold in 50..95) {
                                notificationPreferences.setBudgetAlertThreshold(threshold)
                                showThresholdDialog = false
                                feedbackMessage = "Budget alert threshold updated to $threshold%"
                                showFeedback = true
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showThresholdDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Time Picker Dialog for Daily Reminders
        if (showTimePickerDialog) {
            var selectedHour by remember { mutableStateOf(reminderTimeHour) }
            var selectedMinute by remember { mutableStateOf(reminderTimeMinute) }

            AlertDialog(
                onDismissRequest = { showTimePickerDialog = false },
                title = { Text("Set Reminder Time") },
                text = {
                    Column {
                        Text("Choose when you'd like to be reminded:")

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hour", style = MaterialTheme.typography.labelMedium)
                                OutlinedTextField(
                                    value = String.format("%02d", selectedHour),
                                    onValueChange = { value ->
                                        val hour = value.toIntOrNull()
                                        if (hour != null && hour in 0..23) {
                                            selectedHour = hour
                                        }
                                    },
                                    modifier = Modifier.width(80.dp)
                                )
                            }

                            Text(":", style = MaterialTheme.typography.headlineMedium)

                            // Minute picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Minute", style = MaterialTheme.typography.labelMedium)
                                OutlinedTextField(
                                    value = String.format("%02d", selectedMinute),
                                    onValueChange = { value ->
                                        val minute = value.toIntOrNull()
                                        if (minute != null && minute in 0..59) {
                                            selectedMinute = minute
                                        }
                                    },
                                    modifier = Modifier.width(80.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick time presets
                        Text("Quick presets:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedHour == 9 && selectedMinute == 0,
                                onClick = { selectedHour = 9; selectedMinute = 0 },
                                label = { Text("9:00") }
                            )
                            FilterChip(
                                selected = selectedHour == 18 && selectedMinute == 0,
                                onClick = { selectedHour = 18; selectedMinute = 0 },
                                label = { Text("18:00") }
                            )
                            FilterChip(
                                selected = selectedHour == 20 && selectedMinute == 0,
                                onClick = { selectedHour = 20; selectedMinute = 0 },
                                label = { Text("20:00") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            notificationPreferences.setReminderTime(selectedHour, selectedMinute)
                            if (dailyRemindersEnabled) {
                                notificationScheduler.rescheduleDailyReminder(selectedHour, selectedMinute)
                            }
                            showTimePickerDialog = false
                            feedbackMessage = "Reminder time updated to ${String.format("%02d:%02d", selectedHour, selectedMinute)}"
                            showFeedback = true
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePickerDialog = false }) {
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