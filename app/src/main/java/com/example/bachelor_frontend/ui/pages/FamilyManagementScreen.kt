package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bachelor_frontend.classes.FamilyDto
import com.example.bachelor_frontend.classes.FamilyInvitationDto
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyManagementScreen(
    familyViewModel: FamilyViewModel = viewModel(),
    onNavigateToCreateFamily: () -> Unit,
    onNavigateToFamilyBudget: () -> Unit,
    onNavigateToFamilyExpenses: () -> Unit
) {
    val family by familyViewModel.family.collectAsState()
    val pendingInvitations by familyViewModel.pendingInvitations.collectAsState()
    val isLoading by familyViewModel.isLoading.collectAsState()
    val error by familyViewModel.error.collectAsState()

    var showInviteDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<() -> Unit>({}) }
    var confirmMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        familyViewModel.refreshFamilyData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Management") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (family != null) {
                        IconButton(onClick = { familyViewModel.refreshFamilyData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                error?.let { errorMessage ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Pending invitations
                if (pendingInvitations.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Invitations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(pendingInvitations) { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            onAccept = { familyViewModel.acceptInvitation(invitation.id) },
                            onDecline = { familyViewModel.declineInvitation(invitation.id) }
                        )
                    }
                }

                // Family section
                family?.let { familyData ->
                    item {
                        Text(
                            text = "My Family",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        FamilyCard(
                            family = familyData,
                            onInviteClick = { showInviteDialog = true },
                            onBudgetClick = onNavigateToFamilyBudget,
                            onExpensesClick = onNavigateToFamilyExpenses,
                            onLeaveFamily = {
                                confirmMessage = "Are you sure you want to leave this family?"
                                confirmAction = { familyViewModel.leaveFamily() }
                                showConfirmDialog = true
                            },
                            onDeleteFamily = {
                                confirmMessage = "Are you sure you want to delete this family? This action cannot be undone."
                                confirmAction = { familyViewModel.deleteFamily() }
                                showConfirmDialog = true
                            },
                            isAdmin = familyViewModel.isCurrentUserAdmin(),
                            isCreator = familyViewModel.isCurrentUserCreator()
                        )
                    }
                } ?: run {
                    // No family - show create/join options
                    item {
                        NoFamilyCard(
                            onCreateFamily = onNavigateToCreateFamily
                        )
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Invite dialog
    if (showInviteDialog) {
        InviteUserDialog(
            onInvite = { email ->
                familyViewModel.inviteUserToFamily(email)
                showInviteDialog = false
            },
            onDismiss = { showInviteDialog = false }
        )
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Action") },
            text = { Text(confirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmAction()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InvitationCard(
    invitation: FamilyInvitationDto,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Family Invitation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You've been invited to join \"${invitation.familyName}\"",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Invited by: ${invitation.invitedByName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = "Expires: ${invitation.expiresAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Decline")
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Accept")
                }
            }
        }
    }
}

@Composable
fun FamilyCard(
    family: FamilyDto,
    onInviteClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onLeaveFamily: () -> Unit,
    onDeleteFamily: () -> Unit,
    isAdmin: Boolean,
    isCreator: Boolean
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(family.sharedBudget.currency)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = family.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Members: ${family.members.size}")
                    Text("Monthly Budget: ${currencyFormat.format(family.sharedBudget.monthlyBudget)}")
                }

                if (isAdmin) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Admin") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBudgetClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Budget")
                }

                Button(
                    onClick = onExpensesClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expenses")
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onInviteClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Invite Member")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Leave/Delete options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isCreator) {
                    OutlinedButton(
                        onClick = onLeaveFamily,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Leave Family")
                    }
                }

                if (isCreator) {
                    Button(
                        onClick = onDeleteFamily,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete Family")
                    }
                }
            }
        }
    }
}

@Composable
fun NoFamilyCard(
    onCreateFamily: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Groups,
                contentDescription = "No Family",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Family Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Create a family to share budgets and track expenses together",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onCreateFamily,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Family")
            }
        }
    }
}

@Composable
fun InviteUserDialog(
    onInvite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Family Member") },
        text = {
            Column {
                Text("Enter the email address of the person you want to invite:")

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    label = { Text("Email Address") },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank()) {
                        emailError = "Email cannot be empty"
                        return@Button
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailError = "Invalid email format"
                        return@Button
                    }

                    onInvite(email)
                }
            ) {
                Text("Send Invitation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}