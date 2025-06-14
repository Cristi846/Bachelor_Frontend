package com.example.bachelor_frontend.ui.pages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bachelor_frontend.classes.BudgetType
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import java.time.LocalDateTime
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    categories: List<String>,
    userId: String,
    onSaveExpense: (ExpenseDto, BudgetType) -> Unit,
    onCancel: () -> Unit,
    familyViewModel: FamilyViewModel,
    initialExpense: ExpenseDto? = null,
    onScanReceipt: () -> Unit,
    initialBudgetType: BudgetType = BudgetType.PERSONAL,
    onDocumentScan: () -> Unit = {}
) {
    val context = LocalContext.current

    var amount by remember { mutableStateOf(initialExpense?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(initialExpense?.category ?: categories.firstOrNull() ?: "") }
    var description by remember { mutableStateOf(initialExpense?.description ?: "") }
    var receiptImageUri by remember { mutableStateOf<Uri?>(initialExpense?.receiptImageUrl?.let { Uri.parse(it) }) }

    val shouldDefaultToFamily = initialBudgetType == BudgetType.PERSONAL

    var selectedBudgetType by remember { mutableStateOf(if(shouldDefaultToFamily) BudgetType.FAMILY else initialBudgetType)}
    val userFamily by familyViewModel.family.collectAsState()

    var showCategoryDropdown by remember { mutableStateOf(false) }

    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { receiptImageUri = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialExpense?.id?.isNotEmpty() == true) "Edit Expense" else "Add Expense"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quick Entry Options",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick = onScanReceipt,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Receipt")
                        }

                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Add expense to:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Personal Budget Option
                        FilterChip(
                            selected = selectedBudgetType == BudgetType.PERSONAL,
                            onClick = { selectedBudgetType = BudgetType.PERSONAL },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Personal")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Family Budget Option (only if user has a family)
                        if (userFamily != null) {
                            FilterChip(
                                selected = selectedBudgetType == BudgetType.FAMILY,
                                onClick = { selectedBudgetType = BudgetType.FAMILY },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Groups,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Family")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Budget info text
                    Text(
                        text = if (selectedBudgetType == BudgetType.FAMILY && userFamily != null) {
                            "Will count towards ${userFamily!!.name}'s shared budget"
                        } else {
                            "Will count towards your personal budget"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = showErrorMessage && amount.toDoubleOrNull() == null
            )

            Column {
                OutlinedTextField(
                    value = category,
                    onValueChange = { /* Read-only field */ },
                    label = { Text("Category") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryDropdown = true },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showCategoryDropdown = !showCategoryDropdown }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Category"
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f),
                    offset = DpOffset(0.dp, 0.dp),
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    categories.forEach { categoryItem ->
                        DropdownMenuItem(
                            text = { Text(categoryItem) },
                            onClick = {
                                category = categoryItem
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            if (receiptImageUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Receipt Image",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(receiptImageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Receipt Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            IconButton(
                                onClick = { receiptImageUri = null },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            if (showErrorMessage && errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        when {
                            amountValue == null -> {
                                errorMessage = "Please enter a valid amount"
                                showErrorMessage = true
                            }
                            amountValue <= 0 -> {
                                errorMessage = "Amount must be greater than zero"
                                showErrorMessage = true
                            }
                            category.isBlank() -> {
                                errorMessage = "Please select a category"
                                showErrorMessage = true
                            }
                            else -> {
                                val expense =  initialExpense?.copy(
                                        amount = amountValue,
                                        category = category,
                                        description = description,
                                        receiptImageUrl = receiptImageUri?.toString() ,
                                        budgetType = selectedBudgetType,
                                        familyId = if (selectedBudgetType == BudgetType.FAMILY) userFamily?.id else null
                                    )?: ExpenseDto(
                                        id = "",
                                        userId = userId,
                                        amount = amountValue,
                                        category = category,
                                        description = description,
                                        timestamp = LocalDateTime.now(),
                                        receiptImageUrl = receiptImageUri?.toString(),
                                        budgetType = selectedBudgetType,
                                        familyId = if (selectedBudgetType == BudgetType.FAMILY) userFamily?.id else null
                                    )

                                Log.d("AddExpenseScreen", "Creating expense: $expense")
                                Log.d("AddExpenseScreen", "Budget type: $selectedBudgetType")
                                Log.d("AddExpenseScreen", "Family ID: ${expense.familyId}")

                                onSaveExpense(expense, selectedBudgetType)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}