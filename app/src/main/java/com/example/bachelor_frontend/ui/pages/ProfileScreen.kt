package com.example.bachelor_frontend.ui.pages

import android.net.Uri
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bachelor_frontend.viewmodel.UserViewModel
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // User state
    val userName by userViewModel.userName.collectAsState()
    val userEmail by userViewModel.userEmail.collectAsState()
    val userPhotoUrl by userViewModel.userPhotoUrl.collectAsState()
    val monthlyBudget by userViewModel.monthlyBudget.collectAsState()
    val userCurrency by userViewModel.currency.collectAsState()
    val categories by userViewModel.categories.collectAsState()
    val memberSince by userViewModel.memberSince.collectAsState()

    // Editable states
    var isEditingProfile by remember { mutableStateOf(false) }
    var isEditingBudget by remember { mutableStateOf(false) }
    var isEditingCategories by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf(userName) }
    var editBudget by remember { mutableStateOf(monthlyBudget.toString()) }
    var newCategory by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(userCurrency) }

    // Update editable states when user data changes
    LaunchedEffect(userName) { editName = userName }
    LaunchedEffect(monthlyBudget) { editBudget = monthlyBudget.toString() }
    LaunchedEffect(userCurrency) { selectedCurrency = userCurrency }

    // Photo picker
    var photoUri by remember { mutableStateOf<Uri?>(userPhotoUrl?.let { Uri.parse(it) }) }

    // Update photoUri when userPhotoUrl changes
    LaunchedEffect(userPhotoUrl) {
        photoUri = userPhotoUrl?.let { Uri.parse(it) }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            photoUri = it
            coroutineScope.launch {
                userViewModel.updateUserPhoto(it.toString())
            }
        }
    }

    // State for feedback
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    // Format budget for display
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

    // Available currencies
    val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "RON")

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
        ) { // Profile header
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
                    // Profile image
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                photoPickerLauncher.launch("image/*")
                            }
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoUri)
                                    .crossfade(true)
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

                        // Camera icon overlay for changing photo
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

                    // User name
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // User email
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Member since
                    Text(
                        text = "Member since: $memberSince",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }

            // Profile sections
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
                    // Personal information section
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    if (isEditingProfile) {
                        // Edit mode
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
                        // View mode
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
                        }
                    }
                }
            }

            // Budget section
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
                        // Edit budget mode
                        OutlinedTextField(
                            value = editBudget,
                            onValueChange = { editBudget = it },
                            label = { Text("Monthly Budget") },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        // Currency selector
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
                        // View budget mode
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
            }

            // Categories section
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

                    // Add new category
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

                    // Categories list
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

            // Account actions section
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
                            .clickable { /* Export data */ }
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
                            .clickable { /* Change password */ }
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

            // Version info at bottom
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
    }
}

@Composable
fun LazyRow(
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(content = content)
    }
}