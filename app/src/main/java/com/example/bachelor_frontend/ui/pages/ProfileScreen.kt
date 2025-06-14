package com.example.bachelor_frontend.ui.pages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import com.example.bachelor_frontend.viewmodel.UserViewModel
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.launch
import com.example.bachelor_frontend.service.PDFExportService
import com.example.bachelor_frontend.ui.compose.ExportOptionsDialog
import com.example.bachelor_frontend.viewmodel.AuthUiState
import com.example.bachelor_frontend.viewmodel.AuthViewModel
import com.example.bachelor_frontend.viewmodel.ExpenseViewModel
import java.time.LocalDateTime
import java.time.Month
import java.time.format.TextStyle
import com.example.bachelor_frontend.viewmodel.RecurringExpenseViewModel
import com.example.bachelor_frontend.classes.RecurringExpenseDto
import com.example.bachelor_frontend.ui.compose.RecurringExpenseDialog
import com.example.bachelor_frontend.ui.compose.RecurringExpenseItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    familyViewModel: FamilyViewModel = viewModel(),
    expenseViewModel: ExpenseViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    recurringExpenseViewModel: RecurringExpenseViewModel = viewModel(),
    onSignOut: () -> Unit,
    onNavigateToCreateFamily: () -> Unit = {},
    onNavigateToFamilyManagement: () -> Unit = {},
    onNavigateToFamilyBudget: () -> Unit = {},
    onNavigateToFamilyExpenses: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val auth = FirebaseAuth.getInstance()

    val userName by userViewModel.userName.collectAsState()
    val userEmail by userViewModel.userEmail.collectAsState()
    val userPhotoUrl by userViewModel.userPhotoUrl.collectAsState()
    val monthlyBudget by userViewModel.monthlyBudget.collectAsState()
    val userCurrency by userViewModel.currency.collectAsState()
    val categories by userViewModel.categories.collectAsState()
    val memberSince by userViewModel.memberSince.collectAsState()
    val categoryBudgets by userViewModel.categoryBudgets.collectAsState()

    val expenses by expenseViewModel.expenses.collectAsState()

    val recurringExpenses by recurringExpenseViewModel.recurringExpenses.collectAsState()
    val recurringExpensesDueToday by recurringExpenseViewModel.dueToday.collectAsState()
    val recurringExpensesLoading by recurringExpenseViewModel.loading.collectAsState()
    val recurringExpensesError by recurringExpenseViewModel.error.collectAsState()

    // Recurring expenses dialogs
    var showRecurringExpensesDialog by remember { mutableStateOf(false) }
    var showAddRecurringExpenseDialog by remember { mutableStateOf(false) }
    var editingRecurringExpense by remember { mutableStateOf<RecurringExpenseDto?>(null) }

    val family by familyViewModel.family.collectAsState()
    val pendingInvitations by familyViewModel.pendingInvitations.collectAsState()
    val isLoadingFamily by familyViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        familyViewModel.refreshFamilyData()
        // Load recurring expenses when profile opens
        auth.currentUser?.let { user ->
            Log.d("ProfileScreen", "Loading recurring expenses for user: ${user.uid}")
            recurringExpenseViewModel.loadRecurringExpenses(user.uid)
        }
    }

    LaunchedEffect(Unit) {
        familyViewModel.refreshFamilyData()
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    val pdfExportService = remember { PDFExportService(context) }

    var showPasswordResetDialog by remember { mutableStateOf(false) }
    val authUiState by authViewModel.authUiState.collectAsState()

    var isEditingProfile by remember { mutableStateOf(false) }
    var isEditingEmail by remember { mutableStateOf(false) }
    var isEditingBudget by remember { mutableStateOf(false) }
    var isEditingCategories by remember { mutableStateOf(false) }

    var editEmail by remember { mutableStateOf(userEmail) }
    var editName by remember { mutableStateOf(userName) }
    var editBudget by remember { mutableStateOf(monthlyBudget.toString()) }
    var newCategory by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(userCurrency) }

    LaunchedEffect(userName) { editName = userName }
    LaunchedEffect(monthlyBudget) { editBudget = monthlyBudget.toString() }
    LaunchedEffect(userCurrency) { selectedCurrency = userCurrency }

    LaunchedEffect(authUiState) {
        when (authUiState) {
            is AuthUiState.PasswordResetEmailSent -> {
                showPasswordResetDialog = false
                authViewModel.resetErrorState()
            }
            else -> { /* Handle other states if needed */ }
        }
    }

    var showFeedback by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    val locale = when (userCurrency) {
        "RON" -> Locale("ro", "RO")
        "EUR" -> Locale.GERMANY
        "GBP" -> Locale.UK
        else -> Locale.US
    }

    val numberFormat = remember(userCurrency) {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(userCurrency)
        }
    }

    val formattedBudget = remember(monthlyBudget, userCurrency) {
        numberFormat.format(monthlyBudget)
    }

    var photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    userViewModel.updateUserPhoto(it.toString())
                    feedbackMessage = "Profile photo updated successfully!"
                    showFeedback = true
                } catch (e: Exception) {
                    feedbackMessage = "Failed to update photo: ${e.message}"
                    showFeedback = true
                }
            }
        }
    }

    val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "RON")

    fun exportMonthlyPDF(month: Month, year: Int, sendEmail: Boolean) {
        coroutineScope.launch {
            try {
                isExporting = true

                // Filter expenses for the selected month
                val monthExpenses = expenses.filter { expense ->
                    expense.timestamp.month == month && expense.timestamp.year == year
                }

                if (sendEmail) {
                    // Use the improved export and email method
                    val pdfFile = pdfExportService.exportAndEmailReport(
                        expenses = monthExpenses,
                        monthlyBudget = monthlyBudget,
                        categoryBudgets = categoryBudgets,
                        currency = userCurrency,
                        month = month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        year = year,
                        userName = userName,
                        userEmail = userEmail,
                        exportType = PDFExportService.ExportType.PDF
                    )
                    feedbackMessage = "📧 Email app opened with your $month $year report!\nCheck your sent folder after sending."
                } else {
                    // Generate and share without email
                    val pdfFile = pdfExportService.generateMonthlyPDFReport(
                        expenses = monthExpenses,
                        monthlyBudget = monthlyBudget,
                        categoryBudgets = categoryBudgets,
                        currency = userCurrency,
                        month = month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        year = year,
                        userName = userName
                    )

                    pdfExportService.sharePDFReport(pdfFile, isCSV = false)
                    feedbackMessage = "📄 PDF report generated and ready to share!"
                }

                showFeedback = true

            } catch (e: Exception) {
                feedbackMessage = "❌ Export failed: ${e.message}"
                showFeedback = true
            } finally {
                isExporting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
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
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                photoPickerLauncher.launch("image/*")
                            }
                    ) {
                        if (!userPhotoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(userPhotoUrl) // Direct use of userPhotoUrl
                                    .crossfade(true)
                                    .placeholder(androidx.core.R.drawable.ic_call_answer) // Add placeholder
                                    .error(androidx.core.R.drawable.ic_call_decline) // Add error drawable
                                    .build(),
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Change Photo",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .padding(8.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Member since: $memberSince",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
            // FAMILY MANAGEMENT SECTION - NEW!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Family Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Show pending invitations if any
                    if (pendingInvitations.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Mail,
                                    contentDescription = "Invitations",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${pendingInvitations.size} pending family invitation(s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    family?.let { familyData ->
                        // User is in a family
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToFamilyManagement() }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Family",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = familyData.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${familyData.members.size} members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Family",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        Divider()

                        // Family Budget
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToFamilyBudget() }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Family Budget",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Family Budget",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Manage shared budgets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Budget",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        Divider()

                        // Family Expenses
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToFamilyExpenses() }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "Family Expenses",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Family Expenses",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "View shared expenses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Expenses",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                    } ?: run {
                        // User is not in a family - show create/join options
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCreateFamily() }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Create Family",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Create Family",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Start sharing budgets with family",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Create",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        if (pendingInvitations.isNotEmpty()) {
                            Divider()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToFamilyManagement() }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mail,
                                    contentDescription = "Pending Invitations",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pending Invitations",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Accept or decline family invitations",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "View Invitations",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    // Loading indicator for family data
                    if (isLoadingFamily) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading family data...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    if (isEditingEmail) {
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { isEditingEmail = false }
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    isEditingEmail = false
                                    coroutineScope.launch {
                                        userViewModel.updateUserEmail(userEmail)
                                        feedbackMessage = "Email updated successfully"
                                        showFeedback = true
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Email",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            IconButton(onClick = {
                                isEditingEmail = true
                                editEmail = userEmail
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (isEditingProfile) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { isEditingProfile = false }
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    isEditingProfile = false
                                    coroutineScope.launch {
                                        userViewModel.updateUserName(editName)
                                        feedbackMessage = "Profile updated successfully"
                                        showFeedback = true
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Name",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            IconButton(onClick = {
                                isEditingProfile = true
                                editName = userName
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Budget Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    if (isEditingBudget) {
                        OutlinedTextField(
                            value = editBudget,
                            onValueChange = { editBudget = it },
                            label = { Text("Monthly Budget") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AttachMoney,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            Text(
                                text = "Currency:",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currencies.forEach { currency ->
                                    FilterChip(
                                        selected = selectedCurrency == currency,
                                        onClick = { selectedCurrency = currency },
                                        label = { Text(currency) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { isEditingBudget = false }
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    isEditingBudget = false
                                    val budgetValue = editBudget.toDoubleOrNull() ?: monthlyBudget
                                    coroutineScope.launch {
                                        userViewModel.updateMonthlyBudget(budgetValue)
                                        userViewModel.updateUserCurrency(selectedCurrency)
                                        feedbackMessage = "Budget updated successfully"
                                        showFeedback = true
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Budget ($userCurrency)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                Text(
                                    text = formattedBudget,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = {
                                isEditingBudget = true
                                editBudget = monthlyBudget.toString()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Recurring Expenses Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showRecurringExpensesDialog = true
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring Expenses",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Recurring Expenses",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Manage subscriptions and regular payments",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Manage",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Text(
                            text = "Expense Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { isEditingCategories = !isEditingCategories }) {
                            Icon(
                                imageVector = if (isEditingCategories) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditingCategories) "Done" else "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    if (isEditingCategories) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCategory,
                                onValueChange = { newCategory = it },
                                label = { Text("Add Category") },
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    if (newCategory.isNotBlank() && !categories.contains(newCategory)) {
                                        coroutineScope.launch {
                                            userViewModel.addCategory(newCategory)
                                            newCategory = ""
                                        }
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            categories.forEach { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    if (isEditingCategories && categories.size > 1) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    userViewModel.removeCategory(category)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                if (category != categories.last()) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showExportDialog = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export Data",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Export Data",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPasswordResetDialog = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Change Password",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Change Password",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSignOut() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Finance Tracker v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        if (showFeedback) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(
                        onClick = { showFeedback = false }
                    ) {
                        Text("Dismiss")
                    }
                },
                dismissAction = {
                    IconButton(onClick = { showFeedback = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss"
                        )
                    }
                }
            ) {
                Text(feedbackMessage)
            }
        }

        if (showExportDialog) {
            ExportOptionsDialog(
                onDismiss = { showExportDialog = false },
                onExportPDF = { month, year, sendEmail ->
                    exportMonthlyPDF(month, year, sendEmail)
                    showExportDialog = false
                },
                isLoading = isExporting
            )
        }
        if (showRecurringExpensesDialog) {
            Dialog(onDismissRequest = { showRecurringExpensesDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recurring Expenses",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(onClick = { showRecurringExpensesDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        if (recurringExpensesLoading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading recurring expenses...")
                            }
                        }

                        // Add error display
                        recurringExpensesError?.let { error ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Error: $error",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Summary card
                        if (recurringExpenses.isNotEmpty()) {
                            val summary = recurringExpenseViewModel.getRecurringExpenseSummary()
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "Monthly Equivalent: ",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${summary["totalActive"]?.toInt() ?: 0} active • ${summary["dueThisWeek"]?.toInt() ?: 0} due this week",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // Due today section
                        if (recurringExpensesDueToday.isNotEmpty()) {
                            Text(
                                text = "Due Today (${recurringExpensesDueToday.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            recurringExpensesDueToday.forEach { expense ->
                                RecurringExpenseItem(
                                    recurringExpense = expense,
                                    currency = userCurrency,
                                    onEdit = {
                                        editingRecurringExpense = expense
                                        showAddRecurringExpenseDialog = true
                                    },
                                    onToggleActive = {
                                        auth.currentUser?.let { user ->
                                            recurringExpenseViewModel.toggleRecurringExpenseActive(
                                                expense,
                                                user.uid
                                            )
                                        }
                                    },
                                    onDelete = {
                                        auth.currentUser?.let { user ->
                                            recurringExpenseViewModel.deleteRecurringExpense(
                                                expense.id,
                                                user.uid
                                            )
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Button(
                                onClick = {
                                    auth.currentUser?.let { user ->
                                        recurringExpenseViewModel.processDueRecurringExpenses(user.uid) { expense ->
                                            expenseViewModel.addExpense(expense)
                                            feedbackMessage =
                                                "Generated expense: ${expense.description}"
                                            showFeedback = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Process All Due Expenses")
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // All recurring expenses
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "All Recurring Expenses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedButton(
                                onClick = {
                                    editingRecurringExpense = null
                                    showAddRecurringExpenseDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add",
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (recurringExpenses.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Repeat,
                                        contentDescription = "No recurring expenses",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No recurring expenses yet",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Add subscriptions, rent, or other regular payments",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(recurringExpenses.filter {
                                    !recurringExpensesDueToday.contains(
                                        it
                                    )
                                }) { expense ->
                                    RecurringExpenseItem(
                                        recurringExpense = expense,
                                        currency = userCurrency,
                                        onEdit = {
                                            editingRecurringExpense = expense
                                            showAddRecurringExpenseDialog = true
                                        },
                                        onToggleActive = {
                                            auth.currentUser?.let { user ->
                                                recurringExpenseViewModel.toggleRecurringExpenseActive(
                                                    expense,
                                                    user.uid
                                                )
                                            }
                                        },
                                        onDelete = {
                                            auth.currentUser?.let { user ->
                                                recurringExpenseViewModel.deleteRecurringExpense(
                                                    expense.id,
                                                    user.uid
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add/Edit Recurring Expense Dialog
        if (showAddRecurringExpenseDialog) {
            RecurringExpenseDialog(
                categories = categories,
                currency = userCurrency,
                recurringExpense = editingRecurringExpense,
                onSave = { expense ->
                    auth.currentUser?.let { user ->
                        Log.d("ProfileScreen", "Saving recurring expense: ${expense.description}")
                        if (editingRecurringExpense != null) {
                            recurringExpenseViewModel.updateRecurringExpense(expense, user.uid)
                        } else {
                            recurringExpenseViewModel.addRecurringExpense(expense, user.uid)
                        }

                        feedbackMessage = if (editingRecurringExpense != null)
                            "Recurring expense updated"
                        else
                            "Recurring expense added"
                        showFeedback = true
                    }
                    showAddRecurringExpenseDialog = false
                    editingRecurringExpense = null
                },
                onDismiss = {
                    showAddRecurringExpenseDialog = false
                    editingRecurringExpense = null
                }
            )
        }


        if (showPasswordResetDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPasswordResetDialog = false
                    authViewModel.resetErrorState()
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Change Password")
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "We'll send a password reset link to:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Click the link in the email to set a new password. You'll need to sign in again after changing your password.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        // Show error if there is one
                        if (authUiState is AuthUiState.Error) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = (authUiState as AuthUiState.Error).message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            authViewModel.sendPasswordResetEmail(userEmail)
                        },
                        enabled = authUiState !is AuthUiState.Loading
                    ) {
                        if (authUiState is AuthUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(if (authUiState is AuthUiState.Loading) "Sending..." else "Send Reset Email")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPasswordResetDialog = false
                            authViewModel.resetErrorState()
                        },
                        enabled = authUiState !is AuthUiState.Loading
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}