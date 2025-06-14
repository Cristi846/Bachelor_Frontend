package com.example.bachelor_frontend.classes

import java.time.LocalDateTime

data class FamilyDto(
    var id: String = "",
    var name: String = "",
    var createdBy: String = "",
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var members: List<String> = emptyList(),
    var adminUsers: List<String> = emptyList(),
    var sharedBudget: FamilyBudget = FamilyBudget(),
    var settings: FamilySettings = FamilySettings()
)

data class FamilyBudget(
    var monthlyBudget: Double = 0.0,
    var currency: String = "USD",
    var categoryBudgets: Map<String, Double> = emptyMap()
)

data class FamilySettings(
    var allowMembersToAddExpenses: Boolean = true,
    var requireApprovalForExpenses: Boolean = false,
    var expenseApprovalThreshold: Double = 100.0,
    var allowPersonalExpenses: Boolean = true
)

data class FamilyInvitationDto(
    var id: String = "",
    var familyId: String = "",
    var familyName: String = "",
    var invitedBy: String = "",
    var invitedByName: String = "",
    var invitedEmail: String = "",
    var invitedUserId: String = "",
    var status: InvitationStatus = InvitationStatus.PENDING,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7),
    var respondedAt: LocalDateTime? = null
)

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    CANCELLED
}

// Request/Response DTOs
data class CreateFamilyRequest(
    val familyName: String,
    val userId: String
)

data class CreateFamilyResponse(
    val success: Boolean,
    val message: String,
    val familyId: String?,
    val family: FamilyDto?
)

data class InviteUserRequest(
    val familyId: String,
    val inviterUserId: String,
    val inviteeEmail: String
)

data class AcceptInvitationRequest(
    val userId: String
)

data class UpdateFamilyBudgetRequest(
    val userId: String,
    val sharedBudget: FamilyBudget
)

data class RemoveFamilyMemberRequest(
    val adminUserId: String,
    val memberUserId: String
)

data class FamilyAnalyticsResponse(
    val categorySpending: Map<String, Double> = emptyMap(),
    val memberSpending: Map<String, Double> = emptyMap(),
    val totalFamilySpending: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val overBudgetCategories: List<String> = emptyList()
)

// UI State classes
data class FamilyUiState(
    val family: FamilyDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingInvitations: List<FamilyInvitationDto> = emptyList(),
    val familyInvitations: List<FamilyInvitationDto> = emptyList(),
    val familyExpenses: List<ExpenseDto> = emptyList(),
    val familyAnalytics: FamilyAnalyticsResponse? = null
)