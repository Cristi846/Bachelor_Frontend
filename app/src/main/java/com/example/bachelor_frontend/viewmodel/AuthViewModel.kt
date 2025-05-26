package com.example.bachelor_frontend.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bachelor_frontend.repository.AuthRepository
import com.example.bachelor_frontend.repository.AuthState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    // UI State for authentication
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Initializing)
    val authUiState: StateFlow<AuthUiState> = _authUiState

    init {
        // Observe authentication state changes
        viewModelScope.launch {
            authRepository.authState.collect { authState ->
                when (authState) {
                    is AuthState.Initializing -> _authUiState.value = AuthUiState.Initializing
                    is AuthState.Unauthenticated -> _authUiState.value = AuthUiState.Unauthenticated
                    is AuthState.Authenticated -> _authUiState.value = AuthUiState.Authenticated(authState.user)
                }
            }
        }
    }

    /**
     * Create an account with email and password
     */
    fun createAccountWithEmail(email: String, password: String) {
        _authUiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = authRepository.createAccountWithEmail(email, password)

            result.fold(
                onSuccess = { user ->
                    _authUiState.value = AuthUiState.Authenticated(user)
                },
                onFailure = { exception ->
                    _authUiState.value = AuthUiState.Error(exception.message ?: "Account creation failed")
                }
            )
        }
    }

    /**
     * Sign in with email and password
     */
    fun signInWithEmail(email: String, password: String) {
        _authUiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, password)

            result.fold(
                onSuccess = { user ->
                    _authUiState.value = AuthUiState.Authenticated(user)
                },
                onFailure = { exception ->
                    _authUiState.value = AuthUiState.Error(exception.message ?: "Sign in failed")
                }
            )
        }
    }

    /**
     * Handle Google sign-in result
     */
    fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        _authUiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val result = authRepository.signInWithGoogle(account)

                    result.fold(
                        onSuccess = { user ->
                            _authUiState.value = AuthUiState.Authenticated(user)
                        },
                        onFailure = { exception ->
                            _authUiState.value = AuthUiState.Error(exception.message ?: "Google sign in failed")
                        }
                    )
                } else {
                    _authUiState.value = AuthUiState.Error("Google sign in failed")
                }
            } catch (e: ApiException) {
                _authUiState.value = AuthUiState.Error(when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign in was cancelled"
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error occurred"
                    else -> "Google sign in failed: ${e.message}"
                })
            } catch (e: Exception) {
                _authUiState.value = AuthUiState.Error(e.message ?: "Google sign in failed")
            }
        }
    }

    /**
     * Sign in anonymously
     */
    fun signInAnonymously() {
        _authUiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = authRepository.signInAnonymously()

            result.fold(
                onSuccess = { user ->
                    _authUiState.value = AuthUiState.Authenticated(user)
                },
                onFailure = { exception ->
                    _authUiState.value = AuthUiState.Error(exception.message ?: "Anonymous sign in failed")
                }
            )
        }
    }

    /**
     * Convert anonymous account to permanent account
     */
    fun convertAnonymousAccount(email: String, password: String) {
        _authUiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = authRepository.convertAnonymousAccount(email, password)

            result.fold(
                onSuccess = { user ->
                    _authUiState.value = AuthUiState.Authenticated(user)
                },
                onFailure = { exception ->
                    _authUiState.value = AuthUiState.Error(exception.message ?: "Account conversion failed")
                }
            )
        }
    }

    /**
     * Send password reset email
     */
    fun sendPasswordResetEmail(email: String) {
        _authUiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)

            result.fold(
                onSuccess = {
                    _authUiState.value = AuthUiState.PasswordResetEmailSent
                },
                onFailure = { exception ->
                    _authUiState.value = AuthUiState.Error(exception.message ?: "Failed to send password reset email")
                }
            )
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        authRepository.signOut()
        // State will be updated by the auth state listener in the repository
    }

    /**
     * Check if current user is anonymous
     */
    fun isCurrentUserAnonymous(): Boolean {
        val currentUser = authRepository.getCurrentUser()
        return currentUser?.isAnonymous ?: false
    }

    /**
     * Reset error state
     */
    fun resetErrorState() {
        if (_authUiState.value is AuthUiState.Error) {
            _authUiState.value = AuthUiState.Unauthenticated
        }
    }
}

/**
 * UI State for authentication screens
 */
sealed class AuthUiState {
    object Initializing : AuthUiState()
    object Unauthenticated : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val user: FirebaseUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object PasswordResetEmailSent : AuthUiState()
}