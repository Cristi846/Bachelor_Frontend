package com.example.bachelor_frontend.repository

import android.util.Log
import com.example.bachelor_frontend.classes.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class FamilyRepository {
    private val db = FirebaseFirestore.getInstance()
    private val familiesCollection = "families"
    private val invitationsCollection = "familyInvitations"

    companion object {
        private const val TAG = "FamilyRepository"
    }

    // Family operations
    suspend fun createFamily(family: FamilyDto): String {
        val familyData = familyDtoToMap(family)
        val documentRef = db.collection(familiesCollection).add(familyData).await()
        val familyId = documentRef.id

        // Update the document with its own ID
        family.id = familyId
        val updatedFamilyData = familyDtoToMap(family)
        db.collection(familiesCollection).document(familyId).set(updatedFamilyData).await()

        return familyId
    }

    suspend fun getFamilyById(familyId: String): FamilyDto? {
        return try {
            val snapshot = db.collection(familiesCollection).document(familyId).get().await()
            if (snapshot.exists()) {
                mapToFamilyDto(snapshot.data!!, snapshot.id)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting family by ID", e)
            null
        }
    }

    suspend fun getFamilyByUserId(userId: String): FamilyDto? {
        return try {
            val snapshot = db.collection(familiesCollection)
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val document = snapshot.documents[0]
                mapToFamilyDto(document.data!!, document.id)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting family by user ID", e)
            null
        }
    }

    suspend fun updateFamily(family: FamilyDto) {
        val familyData = familyDtoToMap(family)
        db.collection(familiesCollection).document(family.id).set(familyData, SetOptions.merge()).await()
    }

    suspend fun deleteFamily(familyId: String) {
        db.collection(familiesCollection).document(familyId).delete().await()
    }

    // Family invitation operations
    suspend fun createInvitation(invitation: FamilyInvitationDto): String {
        val invitationData = invitationDtoToMap(invitation)
        val documentRef = db.collection(invitationsCollection).add(invitationData).await()
        val invitationId = documentRef.id

        // Update with ID
        invitation.id = invitationId
        val updatedInvitationData = invitationDtoToMap(invitation)
        db.collection(invitationsCollection).document(invitationId).set(updatedInvitationData).await()

        return invitationId
    }

    suspend fun getInvitationById(invitationId: String): FamilyInvitationDto? {
        return try {
            val snapshot = db.collection(invitationsCollection).document(invitationId).get().await()
            if (snapshot.exists()) {
                mapToInvitationDto(snapshot.data!!, snapshot.id)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting invitation by ID", e)
            null
        }
    }

    suspend fun getPendingInvitationsByEmail(email: String): List<FamilyInvitationDto> {
        return try {
            val snapshot = db.collection(invitationsCollection)
                .whereEqualTo("invitedEmail", email)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    mapToInvitationDto(document.data!!, document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping invitation", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pending invitations", e)
            emptyList()
        }
    }

    suspend fun getFamilyInvitations(familyId: String): List<FamilyInvitationDto> {
        return try {
            val snapshot = db.collection(invitationsCollection)
                .whereEqualTo("familyId", familyId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    mapToInvitationDto(document.data!!, document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping invitation", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting family invitations", e)
            emptyList()
        }
    }

    suspend fun updateInvitation(invitation: FamilyInvitationDto) {
        val invitationData = invitationDtoToMap(invitation)
        db.collection(invitationsCollection).document(invitation.id).set(invitationData, SetOptions.merge()).await()
    }

    suspend fun deleteInvitation(invitationId: String) {
        db.collection(invitationsCollection).document(invitationId).delete().await()
    }

    // Helper functions for data mapping
    private fun familyDtoToMap(family: FamilyDto): Map<String, Any> {
        return hashMapOf(
            "id" to family.id,
            "name" to family.name,
            "createdBy" to family.createdBy,
            "createdAt" to Date.from(family.createdAt.atZone(ZoneId.systemDefault()).toInstant()),
            "members" to family.members,
            "adminUsers" to family.adminUsers,
            "sharedBudget" to hashMapOf(
                "monthlyBudget" to family.sharedBudget.monthlyBudget,
                "currency" to family.sharedBudget.currency,
                "categoryBudgets" to family.sharedBudget.categoryBudgets
            ),
            "settings" to hashMapOf(
                "allowMembersToAddExpenses" to family.settings.allowMembersToAddExpenses,
                "requireApprovalForExpenses" to family.settings.requireApprovalForExpenses,
                "expenseApprovalThreshold" to family.settings.expenseApprovalThreshold,
                "allowPersonalExpenses" to family.settings.allowPersonalExpenses
            )
        )
    }

    private fun mapToFamilyDto(data: Map<String, Any>, id: String): FamilyDto {
        val sharedBudgetMap = data["sharedBudget"] as? Map<String, Any> ?: emptyMap()
        val settingsMap = data["settings"] as? Map<String, Any> ?: emptyMap()

        return FamilyDto(
            id = id,
            name = data["name"] as? String ?: "",
            createdBy = data["createdBy"] as? String ?: "",
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDateTime() ?: LocalDateTime.now(),
            members = (data["members"] as? List<String>) ?: emptyList(),
            adminUsers = (data["adminUsers"] as? List<String>) ?: emptyList(),
            sharedBudget = FamilyBudget(
                monthlyBudget = (sharedBudgetMap["monthlyBudget"] as? Number)?.toDouble() ?: 0.0,
                currency = sharedBudgetMap["currency"] as? String ?: "USD",
                categoryBudgets = (sharedBudgetMap["categoryBudgets"] as? Map<String, Number>)?.mapValues { it.value.toDouble() } ?: emptyMap()
            ),
            settings = FamilySettings(
                allowMembersToAddExpenses = settingsMap["allowMembersToAddExpenses"] as? Boolean ?: true,
                requireApprovalForExpenses = settingsMap["requireApprovalForExpenses"] as? Boolean ?: false,
                expenseApprovalThreshold = (settingsMap["expenseApprovalThreshold"] as? Number)?.toDouble() ?: 100.0,
                allowPersonalExpenses = settingsMap["allowPersonalExpenses"] as? Boolean ?: true
            )
        )
    }

    private fun invitationDtoToMap(invitation: FamilyInvitationDto): Map<String, Any> {
        val map = hashMapOf(
            "id" to invitation.id,
            "familyId" to invitation.familyId,
            "familyName" to invitation.familyName,
            "invitedBy" to invitation.invitedBy,
            "invitedByName" to invitation.invitedByName,
            "invitedEmail" to invitation.invitedEmail,
            "invitedUserId" to invitation.invitedUserId,
            "status" to invitation.status.name,
            "createdAt" to Date.from(invitation.createdAt.atZone(ZoneId.systemDefault()).toInstant()),
            "expiresAt" to Date.from(invitation.expiresAt.atZone(ZoneId.systemDefault()).toInstant())
        )

        invitation.respondedAt?.let {
            map["respondedAt"] = Date.from(it.atZone(ZoneId.systemDefault()).toInstant())
        }

        return map
    }

    private fun mapToInvitationDto(data: Map<String, Any>, id: String): FamilyInvitationDto {
        return FamilyInvitationDto(
            id = id,
            familyId = data["familyId"] as? String ?: "",
            familyName = data["familyName"] as? String ?: "",
            invitedBy = data["invitedBy"] as? String ?: "",
            invitedByName = data["invitedByName"] as? String ?: "",
            invitedEmail = data["invitedEmail"] as? String ?: "",
            invitedUserId = data["invitedUserId"] as? String ?: "",
            status = InvitationStatus.valueOf(data["status"] as? String ?: "PENDING"),
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDateTime() ?: LocalDateTime.now(),
            expiresAt = (data["expiresAt"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDateTime() ?: LocalDateTime.now().plusDays(7),
            respondedAt = (data["respondedAt"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
        )
    }


}