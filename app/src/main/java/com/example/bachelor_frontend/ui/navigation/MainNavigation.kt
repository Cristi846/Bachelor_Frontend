package com.example.bachelor_frontend.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.repository.ExpenseRepository
import com.example.bachelor_frontend.ui.compose.ReceiptScannerScreen
import com.example.bachelor_frontend.ui.pages.*
import com.example.bachelor_frontend.viewmodel.ExpenseViewModel
import com.example.bachelor_frontend.viewmodel.UserViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Analytics : Screen("analytics", "Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics)
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)

    // These screens are not in the bottom navigation
    data object AddExpense : Screen("add_expense", "Add Expense", Icons.Filled.Add, Icons.Outlined.Add)
    data object EditExpense : Screen("edit_expense/{expenseId}", "Edit Expense", Icons.Filled.Edit, Icons.Outlined.Edit)
    data object ReceiptScanner : Screen("receipt_scanner", "Receipt Scanner", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner)
    data object CategoryBudgets : Screen("category_budgets", "Category Budgets", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)
    data object ExpenseChat : Screen("expense_chat", "Expense Chat", Icons.Filled.Chat, Icons.Outlined.Chat)

    data object SmartReceiptAnalysis : Screen("smart_receipt_analysis", "Smart Receipt Analysis", Icons.Filled.InsertChart, Icons.Outlined.InsertChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    expenseViewModel: ExpenseViewModel,
    userViewModel: UserViewModel,
    userId: String,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val expenses by expenseViewModel.expenses.collectAsState()
    val monthlyBudget by expenseViewModel.monthlyBudget.collectAsState()
    val categories by expenseViewModel.categories.collectAsState()

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Analytics,
        Screen.Profile,
        Screen.Settings
    )

    var selectedExpense by remember { mutableStateOf<ExpenseDto?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true) {
                                    screen.selectedIcon
                                } else {
                                    screen.unselectedIcon
                                },
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    expenses = expenses,
                    monthlyBudget = monthlyBudget,
                    expenseViewModel = expenseViewModel,
                    userViewModel = userViewModel,
                    onNavigateToCategoryBudgets = {
                        navController.navigate(Screen.CategoryBudgets.route)
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    userViewModel = userViewModel,
                    onSignOut = onSignOut
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    userViewModel = userViewModel,
                    expenseViewModel = expenseViewModel,
                    onNavigateToCategoryBudgets = {
                        navController.navigate(Screen.CategoryBudgets.route)
                    }
                )
            }

            composable(Screen.AddExpense.route) {
                AddExpenseScreen(
                    categories = categories,
                    userId = userId,
                    onSaveExpense = { expense ->
                        expenseViewModel.addExpense(expense)
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    onScanReceipt = {
                        navController.navigate(Screen.ReceiptScanner.route)
                    }
                )
            }

            composable("edit_expense/{expenseId}") { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
                val expense = expenses.find { it.id == expenseId } ?: selectedExpense

                if (expense != null) {
                    AddExpenseScreen(
                        categories = categories,
                        userId = userId,
                        initialExpense = expense,
                        onSaveExpense = { updatedExpense ->
                            expenseViewModel.addExpense(updatedExpense)
                            navController.popBackStack()
                        },
                        onCancel = {
                            navController.popBackStack()
                        },
                        onScanReceipt = {
                            navController.navigate(Screen.ReceiptScanner.route)
                        }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            // Receipt scanner screen
            composable(Screen.ReceiptScanner.route) {
                ReceiptScannerScreen(
                    onCaptureSuccess = { expenseFromReceipt ->
                        // Add the expense created from the receipt scan
                        expenseViewModel.addExpense(expenseFromReceipt.copy(userId = userId))
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }

            // Category Budget screen
            composable(Screen.CategoryBudgets.route) {
                CategoryBudgetScreen(
                    userViewModel = userViewModel,
                    expenseViewModel = expenseViewModel,
                    userId = userId
                )
            }

            // Home screen - FIXED
            composable(Screen.Home.route) {
                HomeScreen(
                    expenses = expenses,
                    monthlyBudget = monthlyBudget,
                    onAddExpenseClick = {
                        selectedExpense = null
                        navController.navigate(Screen.AddExpense.route)
                    },
                    onExpenseClick = { expense ->
                        selectedExpense = expense
                        navController.navigate("edit_expense/${expense.id}")
                    },
                    onScanReceiptClick = {
                        navController.navigate(Screen.ReceiptScanner.route)
                    },
                    onChatExpenseClick = {
                        navController.navigate(Screen.ExpenseChat.route)
                    }
                )
            }

            // Chat screen - AI-powered
            composable(Screen.ExpenseChat.route) {
                EnhancedExpenseChatScreen(
                    onExpenseCreated = { expense ->
                        expenseViewModel.addExpense(expense.copy(userId = userId))
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    userId = userId,
                    userCurrency = userViewModel.currency.collectAsState().value,
                    useBackend = false // Enable AI backend
                )
            }
        }
    }
}