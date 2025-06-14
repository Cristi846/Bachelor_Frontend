package com.example.bachelor_frontend.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bachelor_frontend.classes.*
import com.example.bachelor_frontend.repository.FamilyRepository
import com.example.bachelor_frontend.repository.ExpenseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime

data class FamilyMember(
    val userId: String,
    val name: String,
    val email: String
)


class FamilyViewModel : ViewModel() {
    private val familyRepository = FamilyRepository()
    private val expenseRepository = ExpenseRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()


    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    private val _family = MutableStateFlow<FamilyDto?>(null)
    val family: StateFlow<FamilyDto?> = _family.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _pendingInvitations = MutableStateFlow<List<FamilyInvitationDto>>(emptyList())
    val pendingInvitations: StateFlow<List<FamilyInvitationDto>> = _pendingInvitations.asStateFlow()

    private val _familyExpenses = MutableStateFlow<List<ExpenseDto>>(emptyList())
    val familyExpenses: StateFlow<List<ExpenseDto>> = _familyExpenses.asStateFlow()

    private val _familyAnalytics = MutableStateFlow<FamilyAnalyticsResponse?>(null)
    val familyAnalytics: StateFlow<FamilyAnalyticsResponse?> = _familyAnalytics.asStateFlow()

    private val _familyMembers = MutableStateFlow<Map<String, FamilyMember>>(emptyMap())
    val familyMembers: StateFlow<Map<String, FamilyMember>> = _familyMembers.asStateFlow()


    companion object {
        private const val TAG = "FamilyViewModel"
    }

    init {
        loadUserFamily()
        loadPendingInvitations()
    }

    /**
     * Load current user's family
     */
    fun loadUserFamily() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val family = familyRepository.getFamilyByUserId(currentUser.uid)
                _family.value = family

                if (family != null) {
                    loadFamilyExpenses(family.id)
                    loadFamilyAnalytics(family.id)
                    loadFamilyMemberDetails(family)
                }

                _error.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user family", e)
                _error.value = "Failed to load family: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun loadFamilyMemberDetails(family: FamilyDto) {
        try {
            val membersMap = mutableMapOf<String, FamilyMember>()

            for (memberId in family.members) {
                try {
                    val userDoc = db.collection("users").document(memberId).get().await()
                    if (userDoc.exists()) {
                        val name = userDoc.getString("name") ?: "Unknown User"
                        val email = userDoc.getString("email") ?: ""
                        membersMap[memberId] = FamilyMember(memberId, name, email)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading member details for $memberId", e)
                    membersMap[memberId] = FamilyMember(memberId, "Member ${memberId.take(8)}", "")
                }
            }

            _familyMembers.value = membersMap
        } catch (e: Exception) {
            Log.e(TAG, "Error loading family member details", e)
        }
    }

    /**
     * Create a new family
     */
    fun createFamily(familyName: String, currency: String = "RON") {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val family = FamilyDto(
                    name = familyName,
                    createdBy = currentUser.uid,
                    members = listOf(currentUser.uid),
                    adminUsers = listOf(currentUser.uid),
                    createdAt = LocalDateTime.now(),
                    sharedBudget = FamilyBudget(
                        monthlyBudget = 0.0,
                        currency = currency,
                        categoryBudgets = emptyMap()
                    )
                )

                val familyId = familyRepository.createFamily(family)
                family.id = familyId
                _family.value = family
                _error.value = null

                Log.d(TAG, "Family created successfully: $familyId")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating family", e)
                _error.value = "Failed to create family: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateFamilyCurrency(newCurrency: String) {
        val currentFamily = _family.value ?: return
        val currentUser = auth.currentUser ?: return

        if (!currentFamily.adminUsers.contains(currentUser.uid)) {
            _error.value = "Only admins can change family currency"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val updatedBudget = currentFamily.sharedBudget.copy(currency = newCurrency)
                val updatedFamily = currentFamily.copy(sharedBudget = updatedBudget)

                familyRepository.updateFamily(updatedFamily)
                _family.value = updatedFamily

                loadFamilyAnalytics(currentFamily.id)
                _error.value = null

                Log.d(TAG, "Family currency updated to: $newCurrency")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating family currency", e)
                _error.value = "Failed to update currency: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Invite user to family
     */
    fun inviteUserToFamily(email: String) {
        val currentUser = auth.currentUser
        val currentFamily = _family.value

        if (currentUser == null) {
            _error.value = "User not authenticated"
            return
        }

        if (currentFamily == null) {
            _error.value = "No family found"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val invitation = FamilyInvitationDto(
                    familyId = currentFamily.id,
                    familyName = currentFamily.name,
                    invitedBy = currentUser.uid,
                    invitedByName = currentUser.displayName ?: "Unknown",
                    invitedEmail = email,
                    createdAt = LocalDateTime.now(),
                    expiresAt = LocalDateTime.now().plusDays(7)
                )

                familyRepository.createInvitation(invitation)
                _error.value = null

                Log.d(TAG, "Invitation sent successfully to: $email")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending invitation", e)
                _error.value = "Failed to send invitation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Accept family invitation
     */
    fun acceptInvitation(invitationId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val invitation = familyRepository.getInvitationById(invitationId)
                if (invitation == null) {
                    _error.value = "Invitation not found"
                    return@launch
                }

                // Update invitation status
                invitation.status = InvitationStatus.ACCEPTED
                invitation.invitedUserId = currentUser.uid
                invitation.respondedAt = LocalDateTime.now()
                familyRepository.updateInvitation(invitation)

                // Add user to family
                val family = familyRepository.getFamilyById(invitation.familyId)
                if (family != null) {
                    val updatedMembers = family.members.toMutableList()
                    if (!updatedMembers.contains(currentUser.uid)) {
                        updatedMembers.add(currentUser.uid)
                        family.members = updatedMembers
                        familyRepository.updateFamily(family)
                        _family.value = family
                    }
                }

                // Remove from pending invitations
                loadPendingInvitations()
                _error.value = null

                Log.d(TAG, "Invitation accepted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting invitation", e)
                _error.value = "Failed to accept invitation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Decline family invitation
     */
    fun declineInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val invitation = familyRepository.getInvitationById(invitationId)
                if (invitation != null) {
                    invitation.status = InvitationStatus.DECLINED
                    invitation.respondedAt = LocalDateTime.now()
                    familyRepository.updateInvitation(invitation)

                    // Remove from pending invitations
                    loadPendingInvitations()
                }

                _error.value = null
                Log.d(TAG, "Invitation declined successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error declining invitation", e)
                _error.value = "Failed to decline invitation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load pending invitations for current user
     */
    fun loadPendingInvitations() {
        val currentUser = auth.currentUser
        if (currentUser?.email == null) return

        viewModelScope.launch {
            try {
                val invitations = familyRepository.getPendingInvitationsByEmail(currentUser.email!!)
                _pendingInvitations.value = invitations
            } catch (e: Exception) {
                Log.e(TAG, "Error loading pending invitations", e)
            }
        }
    }

    /**
     * Load family expenses
     */
    private fun loadFamilyExpenses(familyId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading family expenses for family: $familyId")

                val family = familyRepository.getFamilyById(familyId)
                if (family == null) {
                    Log.e(TAG, "Family not found: $familyId")
                    _familyExpenses.value = emptyList()
                    return@launch
                }

                // Try the primary method first
                var familyExpenses = try {
                    expenseRepository.getFamilyExpenses(familyId)
                } catch (e: Exception) {
                    Log.w(TAG, "Primary method failed, trying alternative approach", e)
                    // If the primary method fails due to index issues, use alternative
                    expenseRepository.getFamilyExpensesAlternative(family.members)
                }

                // Filter to ensure we only get expenses with matching family ID
                familyExpenses = familyExpenses.filter { expense ->
                    expense.familyId == familyId && expense.budgetType == BudgetType.FAMILY
                }

                _familyExpenses.value = familyExpenses.sortedByDescending { it.timestamp }

                Log.d(TAG, "Loaded ${familyExpenses.size} family expenses")
                familyExpenses.forEach { expense ->
                    Log.d(TAG, "  - Expense: ${expense.description}, Amount: ${expense.amount}, Type: ${expense.budgetType}, FamilyID: ${expense.familyId}")
                }

                // Refresh analytics after loading expenses
                loadFamilyAnalytics(familyId)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading family expenses", e)
                _familyExpenses.value = emptyList()
            }
        }
    }

    /**
     * Load family analytics
     */
    private fun loadFamilyAnalytics(familyId: String) {
        viewModelScope.launch {
            try {
                val family = familyRepository.getFamilyById(familyId) ?: return@launch

                // Use the expenses we already loaded to avoid another query
                val familyExpenses = _familyExpenses.value

                Log.d(TAG, "Calculating analytics for ${familyExpenses.size} family expenses")

                // Filter current month expenses for budget analysis
                val currentMonth = LocalDateTime.now().month
                val currentYear = LocalDateTime.now().year

                val currentMonthExpenses = familyExpenses.filter { expense ->
                    expense.timestamp.month == currentMonth && expense.timestamp.year == currentYear
                }

                Log.d(TAG, "Current month expenses: ${currentMonthExpenses.size}")

                // Calculate category spending for current month
                val categorySpending = currentMonthExpenses.groupBy { it.category }
                    .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }

                // Calculate member spending for current month
                val memberSpending = currentMonthExpenses.groupBy { it.userId }
                    .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }

                val totalSpending = currentMonthExpenses.sumOf { it.amount }
                val remainingBudget = family.sharedBudget.monthlyBudget - totalSpending

                // Find over budget categories
                val overBudgetCategories = categorySpending.filter { (category, spent) ->
                    val budget = family.sharedBudget.categoryBudgets[category] ?: 0.0
                    spent > budget && budget > 0.0
                }.keys.toList()

                Log.d(TAG, "Category spending: $categorySpending")
                Log.d(TAG, "Total spending: $totalSpending")
                Log.d(TAG, "Over budget categories: $overBudgetCategories")

                _familyAnalytics.value = FamilyAnalyticsResponse(
                    categorySpending = categorySpending,
                    memberSpending = memberSpending,
                    totalFamilySpending = totalSpending,
                    remainingBudget = remainingBudget,
                    overBudgetCategories = overBudgetCategories
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading family analytics", e)
                _familyAnalytics.value = FamilyAnalyticsResponse()
            }
        }
    }

    /**
     * Update family budget
     */
    fun updateFamilyBudget(budget: FamilyBudget) {
        val currentFamily = _family.value ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val updatedFamily = currentFamily.copy(sharedBudget = budget)
                familyRepository.updateFamily(updatedFamily)
                _family.value = updatedFamily

                // Refresh analytics
                loadFamilyAnalytics(currentFamily.id)
                _error.value = null

                Log.d(TAG, "Family budget updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating family budget", e)
                _error.value = "Failed to update budget: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Remove family member
     */
    fun removeFamilyMember(memberUserId: String) {
        val currentUser = auth.currentUser
        val currentFamily = _family.value

        if (currentUser == null || currentFamily == null) {
            _error.value = "Invalid state"
            return
        }

        if (!currentFamily.adminUsers.contains(currentUser.uid)) {
            _error.value = "Only admins can remove members"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val updatedMembers = currentFamily.members.toMutableList()
                val updatedAdmins = currentFamily.adminUsers.toMutableList()

                updatedMembers.remove(memberUserId)
                updatedAdmins.remove(memberUserId)

                val updatedFamily = currentFamily.copy(
                    members = updatedMembers,
                    adminUsers = updatedAdmins
                )

                familyRepository.updateFamily(updatedFamily)
                _family.value = updatedFamily

                // Refresh expenses and analytics
                loadFamilyExpenses(currentFamily.id)
                loadFamilyAnalytics(currentFamily.id)
                _error.value = null

                Log.d(TAG, "Member removed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing member", e)
                _error.value = "Failed to remove member: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Leave family
     */
    fun leaveFamily() {
        val currentUser = auth.currentUser
        val currentFamily = _family.value

        if (currentUser == null || currentFamily == null) {
            _error.value = "Invalid state"
            return
        }

        if (currentFamily.createdBy == currentUser.uid) {
            _error.value = "Family creator cannot leave. Delete the family instead."
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val updatedMembers = currentFamily.members.toMutableList()
                val updatedAdmins = currentFamily.adminUsers.toMutableList()

                updatedMembers.remove(currentUser.uid)
                updatedAdmins.remove(currentUser.uid)

                val updatedFamily = currentFamily.copy(
                    members = updatedMembers,
                    adminUsers = updatedAdmins
                )

                familyRepository.updateFamily(updatedFamily)
                _family.value = null
                _familyExpenses.value = emptyList()
                _familyAnalytics.value = null
                _error.value = null

                Log.d(TAG, "Left family successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error leaving family", e)
                _error.value = "Failed to leave family: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete family (only creator can do this)
     */
    fun deleteFamily() {
        val currentUser = auth.currentUser
        val currentFamily = _family.value

        if (currentUser == null || currentFamily == null) {
            _error.value = "Invalid state"
            return
        }

        if (currentFamily.createdBy != currentUser.uid) {
            _error.value = "Only the family creator can delete the family"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                expenseRepository.deleteFamilyExpenses(currentFamily.id)

                familyRepository.deleteFamily(currentFamily.id)
                _family.value = null
                _familyExpenses.value = emptyList()
                _familyAnalytics.value = null
                _error.value = null

                Log.d(TAG, "Family deleted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting family", e)
                _error.value = "Failed to delete family: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Promote member to admin
     */
    fun promoteToAdmin(memberUserId: String) {
        val currentUser = auth.currentUser
        val currentFamily = _family.value

        if (currentUser == null || currentFamily == null) {
            _error.value = "Invalid state"
            return
        }

        if (!currentFamily.adminUsers.contains(currentUser.uid)) {
            _error.value = "Only admins can promote members"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val updatedAdmins = currentFamily.adminUsers.toMutableList()
                if (!updatedAdmins.contains(memberUserId)) {
                    updatedAdmins.add(memberUserId)

                    val updatedFamily = currentFamily.copy(adminUsers = updatedAdmins)
                    familyRepository.updateFamily(updatedFamily)
                    _family.value = updatedFamily
                }

                _error.value = null
                Log.d(TAG, "Member promoted to admin successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error promoting member", e)
                _error.value = "Failed to promote member: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if current user is admin
     */
    fun isCurrentUserAdmin(): Boolean {
        val currentUser = auth.currentUser
        val currentFamily = _family.value
        return currentUser != null && currentFamily != null &&
                currentFamily.adminUsers.contains(currentUser.uid)
    }

    /**
     * Check if current user is family creator
     */
    fun isCurrentUserCreator(): Boolean {
        val currentUser = auth.currentUser
        val currentFamily = _family.value
        return currentUser != null && currentFamily != null &&
                currentFamily.createdBy == currentUser.uid
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Refresh all family data
     */
    fun refreshFamilyData() {
        loadUserFamily()
        loadPendingInvitations()
    }

    /**
     * Method to be called after a family expense is added
     */
    fun refreshAfterExpenseAdded() {
        val currentFamily = _family.value
        if (currentFamily != null) {
            Log.d(TAG, "Refreshing family data after expense added")
            loadFamilyExpenses(currentFamily.id)
            loadFamilyAnalytics(currentFamily.id)
        }
    }

    /**
     * Force reload all family data
     */
    fun forceReloadFamilyData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    _isLoading.value = true

                    // Reload family from scratch
                    val family = familyRepository.getFamilyByUserId(currentUser.uid)
                    _family.value = family

                    if (family != null) {
                        Log.d(TAG, "Force reloading family data for: ${family.id}")
                        loadFamilyExpenses(family.id)
                        loadFamilyAnalytics(family.id)
                        loadFamilyMemberDetails(family)
                    }

                    _error.value = null
                } catch (e: Exception) {
                    Log.e(TAG, "Error force reloading family data", e)
                    _error.value = "Failed to reload family data: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

}