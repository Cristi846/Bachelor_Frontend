package com.example.bachelor_frontend.classes

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.LocalDateTime.now

data class ExpenseDto(
    var id: String = "",
    var userId: String = "",
    var amount: Double = 0.0,
    var category: String = "",
    var description: String = "",
    var timestamp: LocalDateTime = LocalDateTime.now(),
    var receiptImageUrl: String? = null,
    var budgetType: BudgetType = BudgetType.PERSONAL,
    var familyId: String? = null
)

enum class BudgetType(val displayName: String){
    PERSONAL("Personal Budget"),
    FAMILY("Family Budget")
}