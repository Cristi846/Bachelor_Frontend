package com.example.bachelor_frontend.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // State flows for user data
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userPhotoUrl = MutableStateFlow<String?>(null)
    val userPhotoUrl: StateFlow<String?> = _userPhotoUrl.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(0.0)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _currency = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    // Add the categoryBudgets StateFlow
    private val _categoryBudgets = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryBudgets: StateFlow<Map<String, Double>> = _categoryBudgets.asStateFlow()

    private val _memberSince = MutableStateFlow("")
    val memberSince: StateFlow<String> = _memberSince.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Shared update listener for ExpenseViewModel
    private val _updateTrigger = MutableStateFlow(0)
    val updateTrigger: StateFlow<Int> = _updateTrigger.asStateFlow()

    init {
        // Load user data whenever the view model is created
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            try {
                _loading.value = true

                // Get current user
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Basic user info from Firebase Auth
                    _userName.value = currentUser.displayName ?: "User"
                    _userEmail.value = currentUser.email ?: ""
                    _userPhotoUrl.value = currentUser.photoUrl?.toString()

                    Log.d("UserViewModel", "Loaded photo URL: ${_userPhotoUrl.value}")

                    // Format account creation date
                    val creationTime = currentUser.metadata?.creationTimestamp ?: 0
                    val date = Date(creationTime)
                    val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    _memberSince.value = format.format(date)

                    // Get additional user data from Firestore
                    val userDoc = withContext(Dispatchers.IO) {
                        db.collection("users")
                            .document(currentUser.uid)
                            .get()
                            .await()
                    }

                    if (userDoc.exists()) {
                        // Monthly budget
                        val budget = userDoc.getDouble("monthlyBudget") ?: 0.0
                        _monthlyBudget.value = budget

                        // Currency
                        val userCurrency = userDoc.getString("currency") ?: "USD"
                        _currency.value = userCurrency

                        // Expense categories
                        @Suppress("UNCHECKED_CAST")
                        val userCategories = userDoc.get("expenseCategories") as? List<String>
                        if (!userCategories.isNullOrEmpty()) {
                            _categories.value = userCategories
                        } else {
                            _categories.value = getDefaultCategories()
                        }

                        // Category budgets
                        @Suppress("UNCHECKED_CAST")
                        val categoryBudgetsMap = userDoc.get("categoryBudgets") as? Map<String, Double>
                        if (categoryBudgetsMap != null) {
                            _categoryBudgets.value = categoryBudgetsMap
                        } else {
                            // Initialize empty budgets for all categories
                            val defaultBudgets = _categories.value.associateWith { 0.0 }
                            _categoryBudgets.value = defaultBudgets

                            // Save default budgets to Firestore
                            withContext(Dispatchers.IO) {
                                db.collection("users")
                                    .document(currentUser.uid)
                                    .update("categoryBudgets", defaultBudgets)
                                    .await()
                            }
                        }
                    } else {
                        // If no user document exists, create one with default values
                        createUserDocument(currentUser.uid)
                    }
                }

                _error.value = null
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error loading user data", e)
                _error.value = "Failed to load user data: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun createUserDocument(userId: String) {
        viewModelScope.launch {
            try {
                val defaultCategories = getDefaultCategories()
                val defaultCategoryBudgets = defaultCategories.associateWith { 0.0 }

                val userData = hashMapOf(
                    "monthlyBudget" to 0.0,
                    "currency" to "USD",
                    "expenseCategories" to defaultCategories,
                    "categoryBudgets" to defaultCategoryBudgets
                )

                withContext(Dispatchers.IO) {
                    db.collection("users")
                        .document(userId)
                        .set(userData)
                        .await()
                }

                _categories.value = defaultCategories
                _monthlyBudget.value = 0.0
                _currency.value = "USD"
                _categoryBudgets.value = defaultCategoryBudgets

            } catch (e: Exception) {
                Log.e("UserViewModel", "Error creating user document", e)
            }
        }
    }

    private fun getDefaultCategories(): List<String> {
        return listOf(
            "Food",
            "Transportation",
            "Housing",
            "Entertainment",
            "Utilities",
            "Healthcare",
            "Shopping",
            "Other"
        )
    }

    // Add method to update a single category budget
    suspend fun updateCategoryBudget(category: String, amount: Double) {
        try {
            _loading.value = true

            val currentUser = auth.currentUser ?: return

            // Create a new map with the updated budget
            val updatedBudgets = _categoryBudgets.value.toMutableMap()
            updatedBudgets[category] = amount

            // Update in Firestore
            withContext(Dispatchers.IO) {
                db.collection("users")
                    .document(currentUser.uid)
                    .update("categoryBudgets", updatedBudgets)
                    .await()
            }

            _categoryBudgets.value = updatedBudgets
            _error.value = null

            // Trigger update for other ViewModels
            triggerUpdate()
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating category budget", e)
            _error.value = "Failed to update category budget: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun updateUserName(newName: String) {
        try {
            _loading.value = true

            val currentUser = auth.currentUser ?: return

            // Update display name in Firebase Auth
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            withContext(Dispatchers.IO) {
                currentUser.updateProfile(profileUpdates).await()
            }

            _userName.value = newName
            _error.value = null

            // Trigger update for other ViewModels
            triggerUpdate()
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating user name", e)
            _error.value = "Failed to update name: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun updateUserPhoto(photoUri: String) {
        try {
            _loading.value = true

            val currentUser = auth.currentUser ?: return

            Log.d("UserViewModel", "Updating photo with URI: $photoUri")

            // Upload image to Firebase Storage
            val storageRef = storage.reference
                .child("profile_images")
                .child("${currentUser.uid}_${System.currentTimeMillis()}.jpg")

            withContext(Dispatchers.IO) {
                storageRef.putFile(Uri.parse(photoUri)).await()
            }

            // Get download URL
            val downloadUrl = withContext(Dispatchers.IO) {
                storageRef.downloadUrl.await()
            }

            Log.d("UserViewModel", "Uploaded photo, download URL: $downloadUrl")

            // Update the Firestore user document with the photo URL
            withContext(Dispatchers.IO) {
                db.collection("users")
                    .document(currentUser.uid)
                    .update("photoUrl", downloadUrl.toString())
                    .await()
            }

            // Update profile photo URL in Firebase Auth
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(downloadUrl)
                .build()

            withContext(Dispatchers.IO) {
                currentUser.updateProfile(profileUpdates).await()
                // Force refresh the user data to ensure changes are reflected
                currentUser.reload().await()
            }

            _userPhotoUrl.value = downloadUrl.toString()
            Log.d("UserViewModel", "Updated photo URL in state: ${_userPhotoUrl.value}")

            _error.value = null

            // Trigger update for other ViewModels
            triggerUpdate()
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating user photo", e)
            _error.value = "Failed to update photo: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun updateMonthlyBudget(budget: Double) {
        try {
            _loading.value = true

            val currentUser = auth.currentUser ?: return

            // Update monthly budget in Firestore
            withContext(Dispatchers.IO) {
                db.collection("users")
                    .document(currentUser.uid)
                    .update("monthlyBudget", budget)
                    .await()
            }

            _monthlyBudget.value = budget
            _error.value = null

            // Trigger update for other ViewModels
            triggerUpdate()
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating monthly budget", e)
            _error.value = "Failed to update budget: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun addCategory(category: String) {
        try {
            _loading.value = true

            val currentUser = auth.currentUser ?: return
            val newCategories = _categories.value.toMutableList()

            // Check if category already exists
            if (!newCategories.contains(category)) {
                newCategories.add(category)

                // Update the category budgets map to include the new category
                val updatedBudgets = _categoryBudgets.value.toMutableMap()
                updatedBudgets[category] = 0.0

                // Update multiple fields atomically
                val updates = hashMapOf<String, Any>(
                    "expenseCategories" to newCategories,
                    "categoryBudgets" to updatedBudgets
                )

                // Update categories in Firestore
                withContext(Dispatchers.IO) {
                    db.collection("users")
                        .document(currentUser.uid)
                        .update(updates)
                        .await()
                }

                _categories.value = newCategories
                _categoryBudgets.value = updatedBudgets

                // Trigger update for other ViewModels
                triggerUpdate()
            }

            _error.value = null
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error adding category", e)
            _error.value = "Failed to add category: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun removeCategory(category: String) {
        try {
            _loading.value = true

            val currentUser = auth.currentUser ?: return
            val newCategories = _categories.value.toMutableList()

            // Don't allow removing all categories
            if (newCategories.size > 1) {
                newCategories.remove(category)

                // Also remove from the category budgets map
                val updatedBudgets = _categoryBudgets.value.toMutableMap()
                updatedBudgets.remove(category)

                // Update multiple fields atomically
                val updates = hashMapOf<String, Any>(
                    "expenseCategories" to newCategories,
                    "categoryBudgets" to updatedBudgets
                )

                // Update in Firestore
                withContext(Dispatchers.IO) {
                    db.collection("users")
                        .document(currentUser.uid)
                        .update(updates)
                        .await()
                }

                _categories.value = newCategories
                _categoryBudgets.value = updatedBudgets

                // Trigger update for other ViewModels
                triggerUpdate()
            }

            _error.value = null
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error removing category", e)
            _error.value = "Failed to remove category: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun updateUserCurrency(currency: String) {
        try {
            _loading.value = true

            val currentUser = auth.currentUser ?: return

            // Update currency in Firestore
            withContext(Dispatchers.IO) {
                db.collection("users")
                    .document(currentUser.uid)
                    .update("currency", currency)
                    .await()
            }

            _currency.value = currency
            _error.value = null

            // Trigger update for other ViewModels
            triggerUpdate()
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating currency", e)
            _error.value = "Failed to update currency: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    private fun triggerUpdate() {
        // Increment the update trigger to notify observers
        _updateTrigger.value = _updateTrigger.value + 1
        Log.d("UserViewModel", "Triggered update: ${_updateTrigger.value}")
    }

    fun signOut() {
        auth.signOut()
    }

    fun clearError() {
        _error.value = null
    }
}