package com.example.bachelor_frontend.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bachelor_frontend.R
import com.example.bachelor_frontend.viewmodel.AuthUiState
import com.example.bachelor_frontend.viewmodel.AuthViewModel
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onSignInWithGoogle: () -> Unit
) {
    // Observe auth UI state
    val authUiState by authViewModel.authUiState.collectAsState()

    // Local UI state
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    var showForgotPassword by rememberSaveable { mutableStateOf(false) }

    // Form state
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    // Error handling
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPasswordError by rememberSaveable { mutableStateOf<String?>(null) }

    // Validation functions
    fun validateEmail(): Boolean {
        return if (email.isEmpty()) {
            emailError = "Email cannot be empty"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Invalid email format"
            false
        } else {
            emailError = null
            true
        }
    }

    fun validatePassword(): Boolean {
        return if (password.isEmpty()) {
            passwordError = "Password cannot be empty"
            false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            false
        } else {
            passwordError = null
            true
        }
    }

    fun validateConfirmPassword(): Boolean {
        return if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            false
        } else {
            confirmPasswordError = null
            true
        }
    }

    // Handle form submission
    fun handleSubmit() {
        // Reset errors
        emailError = null
        passwordError = null
        confirmPasswordError = null

        // Validate fields
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        val isConfirmPasswordValid = if (isRegistering) validateConfirmPassword() else true

        // If all fields are valid, proceed with authentication
        if (isEmailValid && isPasswordValid && isConfirmPasswordValid) {
            if (isRegistering) {
                authViewModel.createAccountWithEmail(email, password)
            } else {
                authViewModel.signInWithEmail(email, password)
            }
        }
    }

    // Handle forgot password
    fun handleForgotPassword() {
        val isEmailValid = validateEmail()

        if (isEmailValid) {
            authViewModel.sendPasswordResetEmail(email)
        }
    }

    // Handle the UI based on authentication state
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App logo/branding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Replace with your app logo
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "Finance Tracker Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(80.dp)
                )
            }

            // Title
            Text(
                text = "Finance Tracker",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Subtitle
            Text(
                text = if (isRegistering) "Create your account" else "Sign in to your account",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Error message
            if (authUiState is AuthUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = (authUiState as AuthUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Password reset confirmation
            if (authUiState is AuthUiState.PasswordResetEmailSent) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Sent",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Password reset email has been sent. Please check your inbox.",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Forgot password form
            if (showForgotPassword) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (emailError != null) validateEmail()
                    },
                    label = { Text("Email") },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Button(
                    onClick = { handleForgotPassword() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Send Reset Link")
                }

                TextButton(
                    onClick = {
                        showForgotPassword = false
                        authViewModel.resetErrorState()
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Back to Sign In")
                }
            } else {
                // Regular sign in / register form
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (emailError != null) validateEmail()
                    },
                    label = { Text("Email") },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) validatePassword()
                    },
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Confirm password field (only for registration)
                if (isRegistering) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (confirmPasswordError != null) validateConfirmPassword()
                        },
                        label = { Text("Confirm Password") },
                        placeholder = { Text("Confirm your password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = confirmPasswordError != null,
                        supportingText = confirmPasswordError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                // Sign in / Register button
                Button(
                    onClick = { handleSubmit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = if (isRegistering) Icons.Default.PersonAdd else Icons.Default.Login,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(if (isRegistering) "Create Account" else "Sign In")
                }

                // Forgot password link (only for sign in)
                if (!isRegistering) {
                    TextButton(
                        onClick = {
                            showForgotPassword = true
                            authViewModel.resetErrorState()
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text("Forgot Password?")
                    }
                }

                // Divider with "or" text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f))
                    Text(
                        text = "OR",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Divider(modifier = Modifier.weight(1f))
                }

                // Google sign-in button
                OutlinedButton(
                    onClick = { onSignInWithGoogle() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White
                    )
                ) {
                    // Replace with Google logo
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Google Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue with Google",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Anonymous sign-in button
                OutlinedButton(
                    onClick = { authViewModel.signInAnonymously() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Anonymous",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Continue as Guest")
                }

                // Toggle between sign in and register
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRegistering) "Already have an account? " else "Don't have an account? ",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    TextButton(
                        onClick = {
                            isRegistering = !isRegistering
                            authViewModel.resetErrorState()
                        }
                    ) {
                        Text(if (isRegistering) "Sign In" else "Register")
                    }
                }
            }
        }

        // Loading indicator
        if (authUiState is AuthUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}