package com.example.bachelor_frontend

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bachelor_frontend.repository.AuthRepository
import com.example.bachelor_frontend.ui.navigation.MainNavigation
import com.example.bachelor_frontend.ui.pages.AuthScreen
import com.example.bachelor_frontend.ui.theme.FinanceTrackerTheme
import com.example.bachelor_frontend.utils.GoogleSignInHelper
import com.example.bachelor_frontend.viewmodel.AuthUiState
import com.example.bachelor_frontend.viewmodel.AuthViewModel
import com.example.bachelor_frontend.viewmodel.ExpenseViewModel
import com.example.bachelor_frontend.viewmodel.UserViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bachelor_frontend.ui.pages.EnhancedExpenseChatScreen
import com.example.bachelor_frontend.utils.ExpenseChatParser
import com.example.bachelor_frontend.viewmodel.FamilyViewModel
import com.example.bachelor_frontend.viewmodel.RecurringExpenseViewModel

class MainActivity : ComponentActivity() {
    private val expenseViewModel: ExpenseViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val familyViewModel: FamilyViewModel by viewModels() // Add Family ViewModel
    private val recurringExpenseViewModel: RecurringExpenseViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInHelper: GoogleSignInHelper

    // Create AuthViewModel using the repository
    private val authRepository by lazy { AuthRepository() }
    private val authViewModel by lazy { AuthViewModel(authRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        try {
            // Initialize Firebase - make sure this happens first
            FirebaseApp.initializeApp(this)
            // Only after Firebase is initialized, get the auth instance
            auth = Firebase.auth

            // Initialize Google Sign-In Helper
            googleSignInHelper = GoogleSignInHelper(this)

            // Connect the view models so they can share data
            expenseViewModel.connectToUserViewModel(userViewModel)

            // Important: Make sure we're not automatically signing in
            // If you want to force showing the login screen, you can sign out here
            // auth.signOut()

            // Log successful initialization
            Log.d("MainActivity", "Firebase initialized successfully")
        } catch (e: Exception) {
            // Log any errors
            Log.e("MainActivity", "Firebase initialization failed", e)
        }

        setContent {
            FinanceTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Main app UI
                    AuthenticationHandler(
                        authViewModel = authViewModel,
                        userViewModel = userViewModel,
                        expenseViewModel = expenseViewModel,
                        familyViewModel = familyViewModel, // Pass Family ViewModel
                        recurringExpenseViewModel = recurringExpenseViewModel,
                        onSignOut = {
                            authViewModel.signOut()
                            googleSignInHelper.signOut()
                        },
                        googleSignInHelper = googleSignInHelper
                    )
                }
            }
        }
    }
}

@Composable
fun AuthenticationHandler(
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    expenseViewModel: ExpenseViewModel = viewModel(),
    familyViewModel: FamilyViewModel= viewModel(),
    recurringExpenseViewModel: RecurringExpenseViewModel,
    googleSignInHelper: GoogleSignInHelper,
    onSignOut: () -> Unit
) {
    val authUiState by authViewModel.authUiState.collectAsState()


    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        authViewModel.handleGoogleSignInResult(task)
    }

    when (authUiState) {
        is AuthUiState.Initializing -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is AuthUiState.Unauthenticated, is AuthUiState.Error, is AuthUiState.PasswordResetEmailSent -> {
            AuthScreen(
                authViewModel = authViewModel,
                onSignInWithGoogle = {
                    signInLauncher.launch(googleSignInHelper.getSignInIntent())
                }
            )
        }
        is AuthUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is AuthUiState.Authenticated -> {
            val user = (authUiState as AuthUiState.Authenticated).user

            LaunchedEffect(user.uid) {
                userViewModel.loadUserData()
                expenseViewModel.loadUserData(user.uid)
                expenseViewModel.loadExpenses(user.uid)
                familyViewModel.loadUserFamily()
                recurringExpenseViewModel.loadRecurringExpenses(user.uid)
            }

            MainNavigation(
                expenseViewModel = expenseViewModel,
                userViewModel = userViewModel,
                authViewModel = authViewModel,
                familyViewModel = familyViewModel,
                recurringExpenseViewModel = recurringExpenseViewModel,
                userId = user.uid,
                onSignOut = onSignOut
            )
        }
    }
}