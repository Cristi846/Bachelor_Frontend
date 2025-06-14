
package com.example.bachelor_frontend.classes

import java.time.LocalDateTime
import java.time.LocalDate

data class RecurringExpenseDto(
    var id: String = "",
    var userId: String = "",
    var amount: Double = 0.0,
    var category: String = "",
    var description: String = "",
    var frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    var startDate: LocalDate = LocalDate.now(),
    var endDate: LocalDate? = null, // null means no end date
    var nextPaymentDate: LocalDate = LocalDate.now(),
    var isActive: Boolean = true,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var lastProcessedDate: LocalDate? = null,
    var automaticallyGenerate: Boolean = true // Auto-generate expenses or just remind
)

enum class RecurrenceFrequency(val displayName: String, val dayInterval: Long) {
    WEEKLY("Weekly", 7),
    BIWEEKLY("Bi-weekly", 14),
    MONTHLY("Monthly", 30),
    QUARTERLY("Quarterly", 90),
    YEARLY("Yearly", 365);

    fun getNextDate(currentDate: LocalDate): LocalDate {
        return when (this) {
            WEEKLY -> currentDate.plusWeeks(1)
            BIWEEKLY -> currentDate.plusWeeks(2)
            MONTHLY -> currentDate.plusMonths(1)
            QUARTERLY -> currentDate.plusMonths(3)
            YEARLY -> currentDate.plusYears(1)
        }
    }
}