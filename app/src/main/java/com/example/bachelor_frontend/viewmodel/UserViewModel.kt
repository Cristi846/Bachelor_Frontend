package com.example.bachelor_frontend.viewmodel

import android.app.Application
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

    private val _categoryBudgets = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryBudgets: StateFlow<Map<String, Double>> = _categoryBudgets.asStateFlow()

    private val _memberSince = MutableStateFlow("")
    val memberSince: StateFlow<String> = _memberSince.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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

                    // Force refresh user data from Firebase Auth to get latest photo URL
                    withContext(Dispatchers.IO) {
                        currentUser.reload().await()
                    }

                    // Get the photo URL from refreshed user data
                    val authPhotoUrl = currentUser.photoUrl?.toString()
                    Log.d("UserViewModel", "Auth photo URL: $authPhotoUrl")

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
                        // Try to get photo URL from Firestore first, then fall back to Auth
                        val firestorePhotoUrl = userDoc.getString("photoUrl")
                        val finalPhotoUrl = firestorePhotoUrl ?: authPhotoUrl

                        Log.d("UserViewModel", "Firestore photo URL: $firestorePhotoUrl")
                        Log.d("UserViewModel", "Final photo URL: $finalPhotoUrl")

                        _userPhotoUrl.value = finalPhotoUrl

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
                        // Set photo URL from Auth if available
                        _userPhotoUrl.value = authPhotoUrl
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

                // Get current photo URL from Auth if available
                val currentPhotoUrl = auth.currentUser?.photoUrl?.toString()

                val userData = hashMapOf<String, Any>(
                    "monthlyBudget" to 0.0,
                    "currency" to "USD",
                    "expenseCategories" to defaultCategories,
                    "categoryBudgets" to defaultCategoryBudgets
                )

                // Add photo URL if available
                currentPhotoUrl?.let {
                    userData["photoUrl"] = it
                }

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
                _userPhotoUrl.value = currentPhotoUrl

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
                currentUser.reload().await() // Reload to ensure changes are reflected
            }

            withContext(Dispatchers.IO){
                try {
                    db.collection("users")
                        .document(currentUser.uid)
                        .update("name", newName)
                        .await()
                }catch (e: Exception){
                    throw e
                }
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

    suspend fun updateUserEmail(newEmail: String) {
        try {
            _loading.value = true

            val currentUser = auth.currentUser ?: return

            // Update email in Firebase Auth
            withContext(Dispatchers.IO) {
                currentUser.updateEmail(newEmail).await()
                currentUser.reload().await() // Reload to ensure changes are reflected
            }

            _userEmail.value = newEmail
            _error.value = null

            // Trigger update for other ViewModels
            triggerUpdate()
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating user email", e)
            _error.value = "Failed to update email: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    suspend fun updateUserPhoto(photoUri: String) {
        try {
            _loading.value = true
            Log.d("UserViewModel", "=== STARTING PHOTO UPDATE ===")
            Log.d("UserViewModel", "Input URI: $photoUri")

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e("UserViewModel", "Current user is null!")
                _error.value = "User not authenticated"
                return
            }

            // Step 1: Validate and parse URI
            val uri = try {
                Uri.parse(photoUri)
            } catch (e: Exception) {
                Log.e("UserViewModel", "Invalid URI format: $photoUri", e)
                _error.value = "Invalid photo selected"
                return
            }

            // Step 2: Verify URI can be accessed (important for content:// URIs)
            val context = getApplication().applicationContext
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e("UserViewModel", "Cannot open input stream for URI: $uri")
                    _error.value = "Cannot access selected photo"
                    return
                }
                inputStream.close()
                Log.d("UserViewModel", "URI validation successful")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Cannot access URI: $uri", e)
                _error.value = "Cannot access selected photo"
                return
            }

            // Step 3: Create storage reference with proper path
            val fileName = "${currentUser.uid}_${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference
                .child("profile_images")
                .child(fileName)

            Log.d("UserViewModel", "Storage path: ${storageRef.path}")

            // Step 4: Upload file with proper error handling
            Log.d("UserViewModel", "Starting upload...")
            val uploadResult = withContext(Dispatchers.IO) {
                try {
                    // Use putFile with the URI directly
                    val uploadTask = storageRef.putFile(uri)

                    // Wait for completion
                    val result = uploadTask.await()

                    Log.d("UserViewModel", "Upload completed successfully")
                    Log.d("UserViewModel", "Bytes transferred: ${result.metadata?.sizeBytes}")

                    result
                } catch (e: Exception) {
                    Log.e("UserViewModel", "Upload failed", e)
                    when (e) {
                        is com.google.firebase.storage.StorageException -> {
                            Log.e("UserViewModel", "Storage exception code: ${e.errorCode}")
                            Log.e("UserViewModel", "Storage exception message: ${e.message}")
                        }
                    }
                    throw e
                }
            }

            // Step 5: Get download URL - WAIT before trying to get URL
            Log.d("UserViewModel", "Getting download URL...")
            val downloadUrl = withContext(Dispatchers.IO) {
                try {
                    // Small delay to ensure upload is fully processed
                    kotlinx.coroutines.delay(1000)

                    val url = storageRef.downloadUrl.await()
                    Log.d("UserViewModel", "Download URL: $url")
                    url
                } catch (e: Exception) {
                    Log.e("UserViewModel", "Failed to get download URL", e)
                    throw e
                }
            }

            val downloadUrlString = downloadUrl.toString()

            // Step 6: Update Firestore first
            Log.d("UserViewModel", "Updating Firestore...")
            withContext(Dispatchers.IO) {
                try {
                    val updates = hashMapOf<String, Any>(
                        "photoUrl" to downloadUrlString,
                        "photoUpdatedAt" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("users")
                        .document(currentUser.uid)
                        .update(updates)
                        .await()

                    Log.d("UserViewModel", "Firestore updated successfully")
                } catch (e: Exception) {
                    Log.e("UserViewModel", "Firestore update failed", e)
                    throw e
                }
            }

            // Step 7: Update Firebase Auth profile
            Log.d("UserViewModel", "Updating Auth profile...")
            withContext(Dispatchers.IO) {
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUrl)
                        .build()

                    currentUser.updateProfile(profileUpdates).await()
                    currentUser.reload().await()

                    Log.d("UserViewModel", "Auth profile updated successfully")
                } catch (e: Exception) {
                    Log.e("UserViewModel", "Auth update failed", e)
                    // Don't throw here - Firestore update was successful
                    Log.w("UserViewModel", "Continuing despite Auth update failure")
                }
            }

            // Step 8: Update local state
            _userPhotoUrl.value = downloadUrlString
            _error.value = null

            Log.d("UserViewModel", "Photo update completed successfully!")
            Log.d("UserViewModel", "Final photo URL: $downloadUrlString")

            // Trigger update for other ViewModels
            triggerUpdate()

        } catch (e: Exception) {
            Log.e("UserViewModel", "Photo update failed completely", e)
            _error.value = when (e) {
                is com.google.firebase.storage.StorageException -> {
                    when (e.errorCode) {
                        com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND ->
                            "Upload failed - file not found"
                        com.google.firebase.storage.StorageException.ERROR_BUCKET_NOT_FOUND ->
                            "Storage configuration error"
                        com.google.firebase.storage.StorageException.ERROR_PROJECT_NOT_FOUND ->
                            "Project configuration error"
                        com.google.firebase.storage.StorageException.ERROR_QUOTA_EXCEEDED ->
                            "Storage quota exceeded"
                        com.google.firebase.storage.StorageException.ERROR_RETRY_LIMIT_EXCEEDED ->
                            "Upload timeout - please try again"
                        else -> "Upload failed: ${e.message}"
                    }
                }
                is java.io.IOException -> "Cannot access photo file"
                is java.net.UnknownHostException -> "No internet connection"
                else -> "Failed to update photo: ${e.message}"
            }
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

    private fun getApplication(): Application {
        return try {
            // This assumes you're using AndroidViewModel
            // If using regular ViewModel, you'll need to pass context differently
            Class.forName("android.app.Application")
                .getDeclaredMethod("getApplication")
                .invoke(null) as Application
        } catch (e: Exception) {
            throw IllegalStateException("Cannot get Application context", e)
        }
    }
}