package com.example.bachelor_frontend.repository

import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState

    init {
        // Listen for authentication state changes
        firebaseAuth.addAuthStateListener { auth ->
            val currentUser = auth.currentUser
            if (currentUser != null) {
                _authState.value = AuthState.Authenticated(currentUser)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }

        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(user)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    /**
     * Create an account with email and password
     */
    suspend fun createAccountWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                // Create user profile in Firestore
                createUserProfile(user)
                Result.success(user)
            } else {
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: FirebaseAuthException) {
            Log.e("AuthRepository", "createAccountWithEmail failed", e)
            Result.failure(parseAuthException(e))
        } catch (e: Exception) {
            Log.e("AuthRepository", "createAccountWithEmail failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: FirebaseAuthException) {
            Log.e("AuthRepository", "signInWithEmail failed", e)
            Result.failure(parseAuthException(e))
        } catch (e: Exception) {
            Log.e("AuthRepository", "signInWithEmail failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google
     */
    suspend fun signInWithGoogle(googleAccount: GoogleSignInAccount): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(googleAccount.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                // Check if this is a new user
                if (authResult.additionalUserInfo?.isNewUser == true) {
                    createUserProfile(user)
                }
                Result.success(user)
            } else {
                Result.failure(Exception("Google sign in failed"))
            }
        } catch (e: FirebaseAuthException) {
            Log.e("AuthRepository", "signInWithGoogle failed", e)
            Result.failure(parseAuthException(e))
        } catch (e: Exception) {
            Log.e("AuthRepository", "signInWithGoogle failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in anonymously
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user

            if (user != null) {
                // Create a basic profile for anonymous users
                createUserProfile(user, isAnonymous = true)
                Result.success(user)
            } else {
                Result.failure(Exception("Anonymous sign in failed"))
            }
        } catch (e: FirebaseAuthException) {
            Log.e("AuthRepository", "signInAnonymously failed", e)
            Result.failure(parseAuthException(e))
        } catch (e: Exception) {
            Log.e("AuthRepository", "signInAnonymously failed", e)
            Result.failure(e)
        }
    }

    /**
     * Convert anonymous account to permanent account
     */
    suspend fun convertAnonymousAccount(email: String, password: String): Result<FirebaseUser> {
        val currentUser = firebaseAuth.currentUser

        if (currentUser == null || !currentUser.isAnonymous) {
            return Result.failure(Exception("No anonymous user is signed in"))
        }

        return try {
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
            val authResult = currentUser.linkWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                // Update the user profile to mark as non-anonymous
                updateUserProfileOnConversion(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Account conversion failed"))
            }
        } catch (e: FirebaseAuthException) {
            Log.e("AuthRepository", "convertAnonymousAccount failed", e)
            Result.failure(parseAuthException(e))
        } catch (e: Exception) {
            Log.e("AuthRepository", "convertAnonymousAccount failed", e)
            Result.failure(e)
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Log.e("AuthRepository", "sendPasswordResetEmail failed", e)
            Result.failure(parseAuthException(e))
        } catch (e: Exception) {
            Log.e("AuthRepository", "sendPasswordResetEmail failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Get current user
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Create user profile in Firestore
     */
    private suspend fun createUserProfile(user: FirebaseUser, isAnonymous: Boolean = false) {
        // Default categories for new users
        val defaultCategories = listOf(
            "Food",
            "Transportation",
            "Housing",
            "Entertainment",
            "Utilities",
            "Healthcare",
            "Shopping",
            "Other"
        )

        // Default category budgets (all zero)
        val defaultCategoryBudgets = defaultCategories.associateWith { 0.0 }

        // Create user profile object
        val userProfile = hashMapOf(
            "userId" to user.uid,
            "email" to (user.email ?: ""),
            "name" to (user.displayName ?: "User"),
            "isAnonymous" to isAnonymous,
            "monthlyBudget" to 0.0,
            "currency" to "USD",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "expenseCategories" to defaultCategories,
            "categoryBudgets" to defaultCategoryBudgets
        )

        // Save to Firestore
        try {
            firestore.collection("users")
                .document(user.uid)
                .set(userProfile)
                .await()

            Log.d("AuthRepository", "User profile created for ${user.uid}")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error creating user profile", e)
            throw e
        }
    }

    /**
     * Update user profile when converting from anonymous
     */
    private suspend fun updateUserProfileOnConversion(user: FirebaseUser) {
        try {
            firestore.collection("users")
                .document(user.uid)
                .update(
                    mapOf(
                        "isAnonymous" to false,
                        "email" to (user.email ?: ""),
                        "name" to (user.displayName ?: "User")
                    )
                )
                .await()

            Log.d("AuthRepository", "User profile updated after conversion for ${user.uid}")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error updating user profile after conversion", e)
            throw e
        }
    }

    /**
     * Parse Firebase Auth exceptions into user-friendly messages
     */
    private fun parseAuthException(e: FirebaseAuthException): Exception {
        val errorMessage = when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
            "ERROR_USER_DISABLED" -> "The user account has been disabled."
            "ERROR_USER_NOT_FOUND" -> "No account found with this email."
            "ERROR_WRONG_PASSWORD" -> "The password is invalid."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already in use."
            "ERROR_WEAK_PASSWORD" -> "The password is too weak."
            "ERROR_OPERATION_NOT_ALLOWED" -> "This sign-in method is not enabled."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "An account already exists with the same email but different sign-in credentials."
            else -> "Authentication failed: ${e.message}"
        }

        return Exception(errorMessage)
    }
}

/**
 * Authentication state for the app
 */
sealed class AuthState {
    object Initializing : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}