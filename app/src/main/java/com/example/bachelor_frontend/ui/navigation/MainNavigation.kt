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
import com.example.bachelor_frontend.classes.BudgetType
import com.example.bachelor_frontend.classes.ExpenseDto
import com.example.bachelor_frontend.repository.ExpenseRepository
import com.example.bachelor_frontend.ui.compose.ReceiptScannerScreen
import com.example.bachelor_frontend.ui.pages.*
import com.example.bachelor_frontend.viewmodel.AuthViewModel
import com.example.bachelor_frontend.viewmodel.ExpenseViewModel
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import com.example.bachelor_frontend.viewmodel.UserViewModel
import com.example.bachelor_frontend.viewmodel.RecurringExpenseViewModel
import androidx.navigation.navArgument
import androidx.navigation.NavType


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

    data object AddExpense : Screen("add_expense?budgetType=PERSONAL", "Add Expense", Icons.Filled.Add, Icons.Outlined.Add)
    data object EditExpense : Screen("edit_expense/{expenseId}", "Edit Expense", Icons.Filled.Edit, Icons.Outlined.Edit)
    data object ReceiptScanner : Screen("receipt_scanner", "Receipt Scanner", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner)
    data object CategoryBudgets : Screen("category_budgets", "Category Budgets", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)
    data object ExpenseChat : Screen("expense_chat", "Expense Chat", Icons.Filled.Chat, Icons.Outlined.Chat)
    data object EnhancedAIChat : Screen("enhanced_ai_chat", "Enhanced AI Chat", Icons.Filled.Psychology, Icons.Outlined.Psychology)

    data object CreateFamily : Screen("create_family", "Create Family", Icons.Filled.GroupAdd, Icons.Outlined.GroupAdd)
    data object FamilyBudget : Screen("family_budget", "Family Budget", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)
    data object FamilyExpenses : Screen("family_expenses", "Family Expenses", Icons.Filled.Receipt, Icons.Outlined.Receipt)

    data object SmartReceiptAnalysis : Screen("smart_receipt_analysis", "Smart Receipt Analysis", Icons.Filled.InsertChart, Icons.Outlined.InsertChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    expenseViewModel: ExpenseViewModel,
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel,
    familyViewModel: FamilyViewModel,
    recurringExpenseViewModel: RecurringExpenseViewModel,
    userId: String,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val expenses by expenseViewModel.expenses.collectAsState()
    val monthlyBudget by expenseViewModel.monthlyBudget.collectAsState()
    val categories by expenseViewModel.categories.collectAsState()

    val userCurrency by userViewModel.currency.collectAsState()
    val categorySpending by expenseViewModel.categorySpending.collectAsState()

    // Create a callback to handle family expense addition
    val handleFamilyExpenseAdded = { expense: ExpenseDto, budgetType: BudgetType ->
        expenseViewModel.addExpense(expense, budgetType)
        // Refresh family data after adding family expense
        if (budgetType == BudgetType.FAMILY) {
            familyViewModel.refreshAfterExpenseAdded()
        }
        navController.popBackStack()
    }

    val familyViewModel: FamilyViewModel = viewModel()
    val recurringExpenseViewModel: RecurringExpenseViewModel = viewModel()

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
                    familyViewModel = familyViewModel,
                    expenseViewModel = expenseViewModel,
                    authViewModel = authViewModel,
                    recurringExpenseViewModel = recurringExpenseViewModel,
                    onSignOut = onSignOut,
                    onNavigateToCreateFamily = {
                        navController.navigate(Screen.CreateFamily.route)
                    },
                    onNavigateToFamilyManagement = {
                        navController.navigate("family_management") // Add this route
                    },
                    onNavigateToFamilyBudget = {
                        navController.navigate(Screen.FamilyBudget.route)
                    },
                    onNavigateToFamilyExpenses = {
                        navController.navigate(Screen.FamilyExpenses.route)
                    }
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

            composable(
                route = "add_expense?budgetType={budgetType}",
                arguments = listOf(
                    navArgument("budgetType") {
                        type = NavType.StringType
                        defaultValue = "PERSONAL"
                    }
                )
            ) { backStackEntry ->
                val budgetTypeString = backStackEntry.arguments?.getString("budgetType") ?: "PERSONAL"
                val initialBudgetType = try {
                    BudgetType.valueOf(budgetTypeString)
                } catch (e: Exception) {
                    BudgetType.PERSONAL
                }

                AddExpenseScreen(
                    categories = categories,
                    userId = userId,
                    onSaveExpense = { expense, budgetType ->
                        expenseViewModel.addExpense(expense, budgetType)
                        // Refresh family data if it's a family expense
                        if (budgetType == BudgetType.FAMILY) {
                            familyViewModel.refreshAfterExpenseAdded()
                        }
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    onScanReceipt = {
                        navController.navigate(Screen.ReceiptScanner.route)
                    },
                    familyViewModel = familyViewModel,
                    initialExpense = null,
                    initialBudgetType = initialBudgetType
                )
            }

            // Family Expenses screen
            composable(Screen.FamilyExpenses.route) {
                FamilyExpensesScreen(
                    familyViewModel = familyViewModel,
                    expenseViewModel = expenseViewModel,
                    onNavigateToAddExpense = {
                        navController.navigate("add_expense?budgetType=FAMILY")
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
                        onSaveExpense = { updatedExpense, budgetType ->
                            expenseViewModel.addExpense(updatedExpense, budgetType)
                            navController.popBackStack()
                        },
                        onCancel = {
                            navController.popBackStack()
                        },
                        onScanReceipt = {
                            navController.navigate(Screen.ReceiptScanner.route)
                        },
                        familyViewModel = familyViewModel
                    )
                } else {
                    navController.popBackStack()
                }
            }

            composable(Screen.ReceiptScanner.route) {
                ReceiptScannerScreen(
                    onCaptureSuccess = { expenseFromReceipt, budgetType ->
                        // Add the expense created from the receipt scan
                        expenseViewModel.addExpense(expenseFromReceipt.copy(userId = userId), budgetType)
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    familyViewModel = familyViewModel
                )
            }

            composable(Screen.CategoryBudgets.route) {
                CategoryBudgetScreen(
                    userViewModel = userViewModel,
                    expenseViewModel = expenseViewModel,
                    userId = userId
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    expenses = expenses,
                    monthlyBudget = monthlyBudget,
                    onAddExpenseClick = {
                        selectedExpense = null
                        navController.navigate("add_expense?budgetType=PERSONAL")
                    },
                    onExpenseClick = { expense ->
                        selectedExpense = expense
                        navController.navigate("edit_expense/${expense.id}")
                    },
                    onScanReceiptClick = {
                        navController.navigate(Screen.ReceiptScanner.route)
                    },
                    onChatExpenseClick = {
                        navController.navigate(Screen.EnhancedAIChat.route)
                    },
                    familyViewModel = familyViewModel
                )
            }

            composable(Screen.ExpenseChat.route) {
                EnhancedExpenseChatScreen(
                    onExpenseCreated = { expense, budgetType ->
                        expenseViewModel.addExpense(expense.copy(userId = userId), budgetType)
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    userId = userId,
                    userCurrency = userViewModel.currency.collectAsState().value,
                    useBackend = false,
                    familyViewModel = familyViewModel
                )
            }

            composable(Screen.EnhancedAIChat.route) {
                EnhancedAIChatScreen(
                    onExpenseCreated = { expense, budgetType ->
                        expenseViewModel.addExpense(expense.copy(userId = userId), budgetType)
                        // Don't navigate back automatically - let user continue chatting
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    userId = userId,
                    userCurrency = userCurrency,
                    categories = categories,
                    monthlyBudget = monthlyBudget,
                    currentSpending = categorySpending.values.sum(),
                    recentExpenses = expenses.take(10), // Pass recent expenses for context
                    familyViewModel = familyViewModel
                )
            }

            // Create Family screen
            composable(Screen.CreateFamily.route) {
                CreateFamilyScreen(
                    familyViewModel = familyViewModel,
                    onFamilyCreated = {
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Family Budget screen
            composable(Screen.FamilyBudget.route) {
                FamilyBudgetScreen(
                    familyViewModel = familyViewModel,
                    userViewModel = userViewModel
                )
            }

            // Family Expenses screen
            composable(Screen.FamilyExpenses.route) {
                FamilyExpensesScreen(
                    familyViewModel = familyViewModel,
                    expenseViewModel = expenseViewModel,
                    onNavigateToAddExpense = {
                        navController.navigate(Screen.AddExpense.route)
                    }
                )
            }

            composable("family_management") {
                FamilyManagementScreen(
                    familyViewModel = familyViewModel,
                    onNavigateToCreateFamily = {
                        navController.navigate(Screen.CreateFamily.route)
                    },
                    onNavigateToFamilyBudget = {
                        navController.navigate(Screen.FamilyBudget.route)
                    },
                    onNavigateToFamilyExpenses = {
                        navController.navigate(Screen.FamilyExpenses.route)
                    }
                )
            }
        }
    }
}